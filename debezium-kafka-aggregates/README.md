# Debezium KStreams Example

This demo shows how to join two CDC event streams created by Debezium into a single topic and
sink the aggregated change events into MongoDB, using the [Kafka Connect MongoDB sink connector(https://github.com/hpgrahsl/kafka-connect-mongodb).

We only interested to capture any data change if the customer have address information

## Preparations

```shell
# Start Kafka, Kafka Connect, a MySQL and a MongoDB database and the aggregator
export DEBEZIUM_VERSION=0.7
docker-compose up mysql zookeeper kafka connect_source

# Start MySQL connector
export DEBEZIUM_VERSION=0.7
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @mysql-source.json
```

# Consume aggregated messages

Browse the Kafka topic:

```shell
export DEBEZIUM_VERSION=0.7
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers 

export DEBEZIUM_VERSION=0.7
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.addresses
```

# Build the maven project
```
mvn clean package -f poc-ddd-aggregates/pom.xml
```

```
export DEBEZIUM_VERSION=0.7
docker-compose build --no-cache aggregator
docker-compose up --build aggregator
```

```
export DEBEZIUM_VERSION=0.7
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic final_ddd_aggregates
```

```
# Start MongoDB sink connector
export DEBEZIUM_VERSION=0.7
docker-compose up -d mongodb connect_sink

curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8084/connectors/ -d @mongodb-sink.json
```

Examine the target collection in the MongoDB sink database:

```shell
export DEBEZIUM_VERSION=0.7
docker-compose exec mongodb bash -c 'mongo inventory'

db.customers_with_addresses.find().pretty()
```

# Modify records in the source database via MySQL client

```shell
export DEBEZIUM_VERSION=0.7
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'

UPDATE customers SET first_name= "Sarah" WHERE id = 1001;

INSERT INTO customers VALUES (default, 'Adrian', 'Sanjaya', 'eekkaaadrian@gmail.com'); # id should be 1005

UPDATE customers SET first_name = 'Adrian Eka' WHERE id = 1005;

# It will be not consumed by the aggregator unless it already have address attributes

INSERT INTO addresses VALUES (default, 1005, 'Street', 'City', 'State', '12312', 'LIVING');
```

The corresponding aggregate should be updated inMongoDB.

Let's explore to the kafka sql. First start MySql and setup the connector, and then follow the command below:
```
# Start Kafka, Kafka Connect, a MySQL and a MongoDB database and the aggregator
export DEBEZIUM_VERSION=0.7
docker-compose down # shutdown previous app
docker-compose up mysql zookeeper kafka connect_source

# Start MySQL connector
export DEBEZIUM_VERSION=0.7
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @mysql-source-ksql.json


export DEBEZIUM_VERSION=0.7
docker-compose up ksql-server

export DEBEZIUM_VERSION=0.7
docker-compose up -d ksql-cli
docker-compose exec ksql-cli ksql http://ksql-server:8088

LIST TOPICS;
SHOW TABLES;

SET 'auto.offset.reset' = 'earliest';
CREATE STREAM orders_from_debezium (order_number integer, order_date string, purchaser integer, quantity integer, product_id integer) WITH (KAFKA_TOPIC='dbserver.inventory.orders',VALUE_FORMAT='json');
CREATE STREAM customers_from_debezium (id integer, first_name string, last_name string, email string) WITH (KAFKA_TOPIC='dbserver.inventory.customers',VALUE_FORMAT='json');

CREATE STREAM orders WITH (KAFKA_TOPIC='ORDERS_REPART',VALUE_FORMAT='json',PARTITIONS=1) as SELECT * FROM orders_from_debezium PARTITION BY PURCHASER;
CREATE STREAM customers_stream WITH (KAFKA_TOPIC='CUSTOMERS_REPART',VALUE_FORMAT='json',PARTITIONS=1) as SELECT * FROM customers_from_debezium PARTITION BY ID;

SELECT * FROM orders_from_debezium LIMIT 1;

CREATE TABLE customers (id integer, first_name string, last_name string, email string) WITH (KAFKA_TOPIC='CUSTOMERS_REPART',VALUE_FORMAT='json',KEY='id');

export DEBEZIUM_VERSION=0.7
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'

INSERT INTO orders VALUES(default,NOW(), 1003,5,101);
UPDATE customers SET first_name='Annie' WHERE id=1004;
UPDATE orders SET quantity=20 WHERE order_number=10004;
```

Reference:
- https://debezium.io/blog/2018/03/08/creating-ddd-aggregates-with-debezium-and-kafka-streams/ accessed on 10th August 2020
