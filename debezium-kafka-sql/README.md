# Debezium Kafka Sql Example

Kafka Sql
-
After we get our hand dirty with Kafka Stream, let's explore to the kafka sql. 

First start MySql and setup the connector, and then follow the command below:
```shell script
# Start Kafka, Kafka Connect, a MySQL and a MongoDB database and the aggregator
docker-compose down # shutdown previous app
docker-compose up mysql zookeeper kafka connect_source

# Start MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @mysql-source-ksql.json

docker-compose up ksql-server

docker-compose up -d ksql-cli
docker-compose exec ksql-cli ksql http://ksql-server:8088
```

```text
LIST TOPICS;
SHOW TABLES;

SET 'auto.offset.reset' = 'earliest';
CREATE STREAM orders_from_debezium (order_number integer, order_date string, purchaser integer, quantity integer, product_id integer) WITH (KAFKA_TOPIC='dbserver.inventory.orders',VALUE_FORMAT='json');
CREATE STREAM customers_from_debezium (id integer, first_name string, last_name string, email string) WITH (KAFKA_TOPIC='dbserver.inventory.customers',VALUE_FORMAT='json');

CREATE STREAM orders WITH (KAFKA_TOPIC='ORDERS_REPART',VALUE_FORMAT='json',PARTITIONS=1) as SELECT * FROM orders_from_debezium PARTITION BY PURCHASER;
CREATE STREAM customers_stream WITH (KAFKA_TOPIC='CUSTOMERS_REPART',VALUE_FORMAT='json',PARTITIONS=1) as SELECT * FROM customers_from_debezium PARTITION BY ID;

SELECT * FROM orders_from_debezium LIMIT 1;

CREATE TABLE customers (id integer, first_name string, last_name string, email string) WITH (KAFKA_TOPIC='CUSTOMERS_REPART',VALUE_FORMAT='json',KEY='id');
```

```shell script
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'
```

```text
INSERT INTO orders VALUES(default, NOW(), 1003,5,101);
UPDATE customers SET first_name='Annie' WHERE id=1004;
UPDATE orders SET quantity=20 WHERE order_number=10004;
```

Reference:
- https://debezium.io/blog/2018/03/08/creating-ddd-aggregates-with-debezium-and-kafka-streams/ 
  accessed on 10th August 2020.
- https://zookeeper.apache.org/ accessed on 13th August 2020.
- https://debezium.io/documentation/reference/1.2/features.html accessed on 13th August 2020.
- https://www.mysql.com/about/ accessed on 13th August 2020.
- https://kafka.apache.org/documentation/streams/ accessed on 13th August 2020.
- https://debezium.io/blog/2018/05/24/querying-debezium-change-data-eEvents-with-ksql/ 
  accessed on 15th August 2020.
