# Debezium and KStreams to Handle Data Aggregation

In now days microservice-based architectures is one of the most popular in industry, and they are often found in 
enterprise scale applications lately. The main goal of microservices is to keep the application small (micro) and have
its own knowledge to serve the specified domain to be maintainable and have a readable code. So microservice-based
architecture known as <b>Domain Driven Design (DDD)</b> to keep application to handle its domain. The applications will 
separated into small pieces along with its data it would be placed into different database to keep it small and neat. 
To achieve the main goal there will be 1 of the biggest things to be sacrifice that is the data management. The data
will be scattered away, and it's kinda hard to reassemble the data for analytical purpose and any activity related to
OLAP data. One possible way to keep data synchronized across multiple services, and stored properly with the expected
structure for OLAP is to make use of an approach called change data capture, or <b>CDC</b> for short.

Essentially <b>CDC</b> allows listening to any modifications which are occurring at one end of a data flow 
(i.e. the data source) and communicate them as change events to other interested parties or storing them into a data 
sink. Instead of doing this in a point-to-point fashion, it’s advisable to decouple this flow of events between data 
sources and data sinks. Such a scenario can be implemented based on Debezium and Apache Kafka. This demo will show how 
to join two CDC event streams created by Debezium into a single topic and sink the aggregated change events into 
MongoDB, using the Kafka Connect MongoDB sink connector (https://github.com/hpgrahsl/kafka-connect-mongodb).

---
These example consist of a microservice that handling customer data including addresses and orders. To keep the example
simple and straight forward we like to use one microservice with single database, but you can do it with a microservice 
with many databases at once or multiple microservices. For now, we only interested to capture any data change if the 
customer have address information changes. So we would only like to store the data to MongoDB if there's a data change 
on users and addresses to related users. 

Before we start there will be some component to be explained:
- <b>Kafka</b> acts as a message broker. A broker is an intermediary brings together two parties or services
  that don’t necessarily know each other for a mutually beneficial exchange to enriching the data.
- <b>Kafka Connect</b>, an open source component of Apache Kafka, is a framework for connecting Kafka with external 
  systems such as databases, key-value stores, search indexes, and file systems.
- <b>ZooKeeper</b> is a centralized service for maintaining configuration information, naming, providing distributed 
  synchronization, and providing group services. All of these kinds of services used to handle distributed applications. 
  Each time they are implemented there is a lot of work goes into fixing the bugs and race 
  conditions that are inevitable. Because of the difficulty of implementing these kinds of services, applications 
  initially usually skimp on them, which make them brittle in the presence of change and difficult to manage. 
  Even when done correctly, when applications deployed it could handle different implementations of these services 
  lead to management complexity.
- <b>Debezium</b> is a set of source connectors for Apache Kafka Connect, ingesting changes from different databases 
  using change data capture (CDC). Unlike other approaches such as polling or dual writes, log-based CDC as implemented 
  by Debezium:
  - makes sure <b>all data changes captured</b>.
  - produces change events with a <b>very low delay</b> (e.g. ms range for MySQL or Postgres) while avoiding increased 
    CPU usage of frequent polling.
  - requires <b>no changes to your data model</b> (such as "Last Updated" column),
  - can <b>capture deletes</b>.
  - can <b>capture old record state and further metadata</b> such as transaction id and causing query (depending on the 
    database’s capabilities and configuration).
- <b>MySQL</b> is one of many popular open source database using relational database concept (RDBMS).

## Preparations

We will use docker to run through the PoC for debezium data aggregation and resource management on production 
environment. First of all we will try to start the required stack to be started using `docker-compose`. Docker compose 
is a tool provides a way to orchestrate multiple containers that work together. So we could manage the docker resource
such as docker network to make available for each resource to communicate each other without any prior knowledge about
them.

```shell script
# Start Kafka, Kafka Connect, a MySQL and Zookeeper
docker-compose up mysql zookeeper kafka connect_source
```

Once all services have been started, register an instance of the Debezium MySQL connector by submitting the following 
JSON document:
```json
{
    "name": "mysql-source",
    "config": {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "tasks.max": "1",
        "database.hostname": "mysql",
        "database.port": "3306",
        "database.user": "debezium",
        "database.password": "dbz",
        "database.server.id": "184054",
        "database.server.name": "dbserver1",
        "table.whitelist": "inventory.customers,inventory.addresses",
        "database.history.kafka.bootstrap.servers": "kafka:9092",
        "database.history.kafka.topic": "schema-changes.inventory",
        "transforms": "unwrap",
        "transforms.unwrap.type":"io.debezium.transforms.UnwrapFromEnvelope",
        "transforms.unwrap.drop.tombstones":"false"
    }
}
```

The config above is trying describe how we set up the connector for the specified database, using the given credentials. 
For our purposes we’re only interested in changes to the customers and addresses tables, hence the `table.whitelist` 
property given to just select these two tables. Another noteworthy thing is the `unwrap` transform that is applied. 
By default, Debezium’s CDC events would contain the old and new state of changed rows and some additional metadata on 
the source of the change. By applying the `io.debezium.transforms.UnwrapFromEnvelope` 
SMT (single message transformation) on `transforms.unwrap.type` key, only the new state will be propagated into the 
corresponding Kafka topics.

```shell script
# Start MySQL connector
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8083/connectors/ -d @mysql-source.json
```

## Monitor the ingested message

We can take a look at them once the connector has been deployed and finished its initial snapshot of the two captured 
tables by using the `kafka-console-consumer` tools that already provided by originally from Kafka. We would like to
watch to topic at the same time because we wanted to listen to two tables at the same times. As the docker application
already run we could use command `docker-compose exec` to try execute the tools inside the kafka container by running 
these command:

```shell script
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.customers 

# Open another terminal or you could utilize your tmux to show up the CDC message on the addresses table
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic dbserver1.inventory.addresses
```

You should see the following output (formatted and omitting the schema information for the sake of readability) for the 
topic with customer changes:
```text
{
    "schema": { ... },
    "payload": {
        "id": 1001
    }
}
{
    "schema": { ... },
    "payload": {
        "id": 1001,
        "first_name": "Sally",
        "last_name": "Thomas",
        "email": "sally.thomas@acme.com"
    }
}
...
```
You should see the similar message including schema with the key and payload from the other terminal contains
CDC from address table for the other data. Before moving forward to the Kafka Stream application now take a look where's
the data came from. Open a new terminal to going inside the MySQL docker.

```shell script
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'
```

Inside the docker now you can take a look for the data already being prepared for this example. The connector will try
to ingest all the data from the beginning of the time. So it's up to us whether we wanted to pick and aggregate
the data from the beginning or the latest one. For this example we would try to aggregate all the data from the
beginning of the time. This could be done by add some config parameter on the code:

```
  private static final String AUTO_OFFSET_RESET_CONFIG = "earliest";

  ...

  Properties props = new Properties();
  
  ...

  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_CONFIG);
```

The other possible value is `latest` so the Kafka Stream application will not try to process the data from the beginning
of the time it would only try to process the latest one. This config only could apply once, because once the offset
already being marked you couldn't change it to the earliest (if you change your mind to aggregate the data from the
beginning of the time), the possibility are trying to reset it and reprocess it with `earliest` parameter. 
The application will respect `AUTO_OFFSET_RESET_CONFIG` parameter if the offset is not being setup on the system. Once
it already setup it would only read based on the offset. We will try to catch up about offset later on.

# Building Kafka Stream Aggregator

We would like to use Kafka Stream API to process and aggregate the data changes. Kafka Streams is a client library for 
building applications and microservices, where the input and output data stored in Kafka clusters. It combines the 
simplicity of writing and deploying standard Java and Scala applications on the client side with the benefits of Kafka's 
server-side cluster technology.

The KStreams application is going to process data from the two Kafka topics. These topics receive CDC events based on 
the customers and addresses relations found in MySQL, each of which has its corresponding Jackson-annotated POJO 
(Customer and Address), enriched by a field holding the CDC event type (i.e. UPSERT/DELETE). Since the Kafka topic 
records are in Debezium JSON format with unwrapped envelopes, a special SerDe has been written in order to be able to 
read/write these records using their POJO or Debezium event representation respectively. While the serializer simply 
converts the POJOs into JSON using Jackson, the deserializer is a "hybrid" one, being able to deserialize from either 
Debezium CDC events with POJOs. With that in place, the KStreams topology to create and maintain DDD aggregates 
on-the-fly can be built as follows:
  
####  Customers Topic ("parent")
All the customer records came from the customer topic into a KTable which will automatically maintain the latest state 
per customer according to the record key (i.e. the customer’s PK)

```text
KTable<DefaultId, Customer> customerTable = builder.table(parentTopic, Consumed.with(defaultIdSerde,customerSerde));
```

#### Addresses Topic ("children")
For the address records the processing is a bit more involved and needs several steps. First, all the address records 
streamed into a KStream.

```
KStream<DefaultId, Address> addressStream = builder.stream(childrenTopic, Consumed.with(defaultIdSerde, addressSerde));
```

Second, a `pseudo` grouping of these address records done based on their keys (the original primary key in the relation). 
During this step the relationships towards the corresponding customer records still maintained. This effectively allows 
keeping track which address record belongs to which customer record, even in the light of address record deletions. 
To achieve this an additional LatestAddress POJO introduced which allows to store the latest known PK <→ FK relation in 
addition to the Address record itself.

```text
KTable<DefaultId,LatestAddress> tempTable = addressStream
  .groupByKey(Serialized.with(defaultIdSerde, addressSerde))
  .aggregate(
    () -> new LatestAddress(),
    (DefaultId addressId, Address address, LatestAddress latest) -> {
      latest.update(
        address, addressId, new DefaultId(address.getCustomer_id()));
      return latest;
    },
    Materialized.<DefaultId,LatestAddress,KeyValueStore<Bytes, byte[]>>
      as(childrenTopic+"_table_temp")
        .withKeySerde(defaultIdSerde)
        .withValueSerde(latestAddressSerde)
  );
```

Third, the intermediate KTable is again converted to a KStream. The LatestAddress records transformed to have the 
customer id (FK relationship) as their new key in order to group them per customer. During the grouping step, customer 
specific addresses updated which can result in an address record being added or deleted. For this purpose, another 
POJO called Addresses introduced, which holds a map of address records that gets updated accordingly. 
The result is a KTable holding the most recent Addresses per customer id.

```
KTable<DefaultId, Addresses> addressTable = tempTable.toStream()
  .map((addressId, latestAddress) -> 
    new KeyValue<>(latestAddress.getCustomerId(),latestAddress))
  .groupByKey(Serialized.with(defaultIdSerde,latestAddressSerde))
  .aggregate(
    () -> new Addresses(),
    (customerId, latestAddress, addresses) -> {
      addresses.update(latestAddress);
      return addresses;
    },
    Materialized.<DefaultId,Addresses,KeyValueStore<Bytes, byte[]>>
      as(childrenTopic+"_table_aggregate")
        .withKeySerde(defaultIdSerde)
        .withValueSerde(addressesSerde)
  );
```

#### Combining Customers With Addresses
Finally, it’s easy to bring customers and addresses together by joining the customers KTable with the addresses KTable 
and thereby building the DDD aggregates which are represented by the CustomerAddressAggregate POJO. 
At the end, the KTable changes streamed to a KStream, which in turn gets saved into a kafka topic. 
This allows making use of the resulting DDD aggregates in manifold ways.

```
     KTable<DefaultId,CustomerAddressAggregate> dddAggregate =
               customerTable.join(addressTable, (customer, addresses) ->
                   customer.get_eventType() == EventType.DELETE ?
                           null :
                           new CustomerAddressAggregate(customer,addresses.getEntries())
               );
     
       dddAggregate.toStream().to("final_ddd_aggregates",
                                   Produced.with(defaultIdSerde,(Serde)aggregateSerde));
```
Records in the customers KTable might receive a CDC the `delete` event. If so, this can be detected by checking the 
event type field of the customer POJO and e.g. return 'null' instead of a DDD aggregate. 
Such a convention can be helpful whenever consuming parties also need to act to deletions accordingly.

The important part you should know is offset in Kafka. Kafka remembers your application by storing consumer offsets in a
special topic. Offsets are numbers assigned to messages by the Kafka broker(s) indicating the order in which they 
arrived at the broker(s). By remembering your application’s last committed offset, your application is only going to 
process newly arrived messages. The configuration setting `offsets.retention.minutes` controls how long Kafka will 
remember offsets in the special topic. The default value is 10,080 minutes (7 days).

If your application stopped (hasn’t connected to the Kafka cluster) for a while, you could end up in a situation where 
you start reprocessing data on application restart because the broker(s) have deleted the offsets in the meantime. 
The actual startup behavior depends on your `auto.offset.reset` configuration that can be set to 
`earliest`, `latest`, or `none`. To avoid this problem, it is recommended to increase `offsets.retention.minutes` to 
an appropriately large value.

After we have some overview to the code let's try to run compile and package the program. For this example I will pick
maven as the package manager to using pom file. So to package the application we could run this command below:
```shell script
mvn clean package -f poc-ddd-aggregates/pom.xml
```

You should see the success message below:
```text
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  4.543 s
[INFO] Finished at: 2020-08-14T23:29:36+07:00
[INFO] ------------------------------------------------------------------------
```

After that we would like try to run the application inside the docker. First we would like try to build it from the
docker file, so we could try to run it by using this command below:
```shell script
docker-compose up --build aggregator
```

The Kafka Stream application will aggregate the events and will try to publish the message into another Kafka Topic 
called: `final_ddd_aggregates`. Once the aggregation pipeline is running, we can take a look at the aggregated events 
using the console consumer:

```shell script
docker-compose exec kafka /kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --from-beginning \
    --property print.key=true \
    --topic final_ddd_aggregates
```

Transferring DDD Aggregates to Data Sinks
---
We originally set out to build these DDD aggregates in order to transfer data and synchronize changes between a data 
source (MySQL tables in this case), and a convenient data sink. By definition, DDD aggregates are typically complex data
structures and therefore it makes perfect sense to write them to data stores which offer flexible ways and means to 
query and/or index them. Talking about NoSQL databases, a document store seems the most natural choice with MongoDB 
being the leading database for such use cases.

Thanks to Kafka Connect and numerous turn-key ready connectors it is almost effortless to get this done. Using a MongoDB 
sink connector from the open-source community, it is easy to have the DDD aggregates written into MongoDB. All it needs 
is a proper configuration which can be posted to the REST API of Kafka Connect in order to run the connector.

So let’s start MongoDb and another Kafka Connect instance for hosting the sink connector:
```shell script
# Start MongoDB sink connector
docker-compose up -d mongodb connect_sink
```

In case the DDD aggregates should get written unmodified into MongoDB, a configuration may look as simple as follows:
```json
{
    "name": "mongodb-sink",
    "config": {
        "connector.class": "at.grahsl.kafka.connect.mongodb.MongoDbSinkConnector",
        "tasks.max": "1",
        "topics": "final_ddd_aggregates",
        "mongodb.connection.uri": "mongodb://mongodb:27017/inventory?w=1&journal=true",
        "mongodb.collection": "customers_with_addresses",
        "mongodb.document.id.strategy": "at.grahsl.kafka.connect.mongodb.processor.id.strategy.FullKeyStrategy",
        "mongodb.delete.on.null.values": "true"
    }
}
```

As with the source connector file, deploy the connector using curl:
```shell script
curl -i -X POST -H "Accept:application/json" -H  "Content-Type:application/json" http://localhost:8084/connectors/ -d @mongodb-sink.json
```

This connector will consume messages from the "final_ddd_aggregates" Kafka topic and write them as MongoDB documents 
into the `customers_with_addresses` collection.

You can take a look by firing up a Mongo shell and querying the collection’s contents:
```shell script
docker-compose exec mongodb bash -c 'mongo inventory'

db.customers_with_addresses.find().pretty()
```

The result would be look like this:
```json
{
    "_id": {
        "id": "1001"
    },
    "addresses": [
        {
            "zip": "76036",
            "_eventType": "UPSERT",
            "city": "Euless",
            "street": "3183 Moore Avenue",
            "id": "10",
            "state": "Texas",
            "customer_id": "1001",
            "type": "SHIPPING"
        },
        {
            "zip": "17116",
            "_eventType": "UPSERT",
            "city": "Harrisburg",
            "street": "2389 Hidden Valley Road",
            "id": "11",
            "state": "Pennsylvania",
            "customer_id": "1001",
            "type": "BILLING"
        }
    ],
    "customer": {
        "_eventType": "UPSERT",
        "last_name": "Thomas",
        "id": "1001",
        "first_name": "Sally",
        "email": "sally.thomas@acme.com"
    }
}
```

# Modify records in the source database via MySQL client
Due to the combination of the data in a single document some parts aren’t needed or redundant.
To get rid of any unwanted data (e.g. _eventType, customer_id of each address sub-document) it would also be possible to
adapt the configuration in order to blacklist said fields.

Finally, you update some customer or address data in the MySQL source database:
```shell script
docker-compose exec mysql bash -c 'mysql -u $MYSQL_USER -p$MYSQL_PASSWORD inventory'

UPDATE customers SET first_name= "Sarah" WHERE id = 1001;

INSERT INTO customers VALUES (default, 'Adrian', 'Sanjaya', 'eekkaaadrian@gmail.com'); # id should be 1005

UPDATE customers SET first_name = 'Adrian Eka' WHERE id = 1005;

# It will be not consumed by the aggregator unless it already have address attributes

INSERT INTO addresses VALUES (default, 1005, 'Street', 'City', 'State', '12312', 'LIVING');
```

Shortly thereafter, you should see that the corresponding aggregate document in MongoDB has been updated accordingly.

To enrich your knowledge about the config parameter and handling operational issue let's try some other thing. Try to
take down all of your application before we begin to explore some other config. To shut down all the docker container
you could use `docker-compose down` from the main directory. After waiting for some times and its successfully 
shut down, now on the main class `StreamingAggregatesDDD` we would like to try to change some line of code. We will try
to change the line:
```
private static final String AUTO_OFFSET_RESET_CONFIG = "earliest";
```
to become:
```
private static final String AUTO_OFFSET_RESET_CONFIG = "latest";
```
That value above will attach to the parameter config named `auto.offset.reset`. 

## Auto Offset Reset

After you change it please follow through the command above until you reach this following steps. Please make sure
while you build the application the docker miss the cache while packing the application, or you can do it manually
to make sure it will miss the cache. Inside the `poc-ddd-aggregate` folder you could find a file named `Dockerfile`
actually its one of the file contains set of instructions how to build the aggregator application. After that open the
file using your favorite text editor and find the line `RUN echo "20"`, and please change it to echo another one, maybe
it's a great idea to change it to a greater sequence number like `21` so it would become `RUN echo "21"`.
```shell script
docker-compose up --build aggregator
```
After that try to watch the published message to `final_ddd_aggregates` topic using the very next command to view the 
message came in by using console consumer, or you can see on the terminal runs your `aggregator` application. You will 
not see any incoming message you see previously that you could see that the Kafka Stream application will try to
ingest the data from the beginning. Because the `latest` option would be read the data that have ingested time greater
than current time when application started and would ignore any earlier data than that. Also, it related to
`offsets.retention.minutes` config, that how long Kafka Stream application will remember the current latest committed
offset (please see the explanation above if you just jump into this part). If we already passed the specified the Kafka 
Stream application will respect to the value on `auto.offset.reset` config. Kafka stream will always renew the time 
while committed the offset each time a data is already processed.

### Changelog retention and file rotation


### Drawbacks and Limitations

While this first version for creating DDD aggregates from table-based CDC events basically works, it is very important 
to understand its current limitations:
- not generically applicable thus needs custom code for POJOs and intermediate types
- cannot be scaled across multiple instances as is due to missing but necessary data repartitioning prior to processing
- limited to building aggregates based on a single JOIN between 1:N relationships
- resulting DDD aggregates are eventually consistent, meaning it is possible for them to temporarily exhibit 
  intermediate state before converging

The first few can be addressed with a reasonable amount of work on the KStreams application. The last one, dealing with 
the eventually consistent nature of resulting DDD aggregates is much harder to correct and will require some efforts at 
Debezium’s own CDC mechanism.

### Outlook

In this post we described an approach for creating aggregated events from Debezium’s CDC events. 
In a follow-up blog post we may dive a bit more into the topic of how to be able to horizontally scale the DDD creation 
by running multiple KStreams aggregator instances. For that purpose, the data needs proper re-partitioning before 
running the topology. In addition, it could be interesting to look into a somewhat more generic version which only needs
custom classes to describe the two main POJOs involved.
We also thought about providing a ready-to-use component which would work in generic way (based on Connect records, 
i.e. not tied to a specific serialization format such as JSON) and could be set up as a configurable stand-alone process 
running given aggregations.

Reference:
- https://debezium.io/blog/2018/03/08/creating-ddd-aggregates-with-debezium-and-kafka-streams/ 
  accessed on 10th August 2020.
- https://zookeeper.apache.org/ accessed on 13th August 2020.
- https://debezium.io/documentation/reference/1.2/features.html accessed on 13th August 2020.
- https://www.mysql.com/about/ accessed on 13th August 2020.
- https://kafka.apache.org/documentation/streams/ accessed on 13th August 2020.
- https://docs.confluent.io/current/streams/faq.html#accessing-record-metadata-such-as-topic-partition-and-offset-information
  accessed on 15th August 2020.