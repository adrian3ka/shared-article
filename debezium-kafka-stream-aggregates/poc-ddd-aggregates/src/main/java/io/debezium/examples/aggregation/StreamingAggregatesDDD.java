package io.debezium.examples.aggregation;

import io.debezium.examples.aggregation.model.*;
import io.debezium.examples.aggregation.serdes.SerdeFactory;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StreamingAggregatesDDD {
  private static final String AUTO_OFFSET_RESET_CONFIG = "earliest";
  private static final boolean ENABLE_AUTO_COMMIT_CONFIG = true;

  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("usage: java -jar <package> "
        + StreamingAggregatesDDD.class.getName() + " <parent_topic> <children_topic> <bootstrap_servers>");
      System.exit(-1);
    }

    final String parentTopic = args[0];
    final String childrenTopic = args[1];
    final String bootstrapServers = args[2];

    final String TABLE_AGGREGATE = childrenTopic + "_table_aggregate";

    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-aggregates");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024);
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "60");
    props.put(TopicConfig.FLUSH_MESSAGES_INTERVAL_CONFIG, "60");
    props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);

    // CHANGE AND TRY CONFIG
    System.out.println("AUTO_OFFSET_RESET_CONFIG >> " + AUTO_OFFSET_RESET_CONFIG);
    System.out.println("ENABLE_AUTO_COMMIT_CONFIG >> " + ENABLE_AUTO_COMMIT_CONFIG);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_CONFIG);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT_CONFIG);

    final Serde<DefaultId> defaultIdSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(DefaultId.class, true);
    final Serde<Customer> customerSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(Customer.class, false);
    final Serde<Address> addressSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(Address.class, false);
    final Serde<LatestAddress> latestAddressSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(LatestAddress.class, false);
    final Serde<Addresses> addressesSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(Addresses.class, false);
    final Serde<CustomerAddressAggregate> aggregateSerde =
      SerdeFactory.createDbzEventJsonPojoSerdeFor(CustomerAddressAggregate.class, false);

    StreamsBuilder builder = new StreamsBuilder();

    System.out.println("parentTopic >> " + parentTopic);
    System.out.println("childrenTopic >> " + childrenTopic);
    System.out.println("bootstrapServers >> " + bootstrapServers);

    //1) read parent topic i.e. customers as ktable
    KTable<DefaultId, Customer> customerTable =
      builder.table(parentTopic, Consumed.with(defaultIdSerde, customerSerde));

    //2) read children topic i.e. addresses as kstream
    KStream<DefaultId, Address> addressStream = builder.stream(childrenTopic,
      Consumed.with(defaultIdSerde, addressSerde));

    Map<String, String> stateStoreConfig = new HashMap<>();
    stateStoreConfig.put(TopicConfig.SEGMENT_BYTES_CONFIG, "3000");
    stateStoreConfig.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    stateStoreConfig.put(TopicConfig.RETENTION_MS_CONFIG, "60000"); // 1 minutes

    System.out.println(stateStoreConfig);

    //2a) pseudo-aggreate addresses to keep latest relationship info
    KTable<DefaultId, LatestAddress> tempTable = addressStream
      .groupByKey(Serialized.with(defaultIdSerde, addressSerde))
      .aggregate(
        LatestAddress::new,
        (DefaultId addressId, Address address, LatestAddress latest) -> {
          System.out.println("address >> " + address);
          System.out.println("addressId >> " + addressId);
          System.out.println("defaultId >> " + new DefaultId(address.getCustomer_id()));
          System.out.println("------------------------------------------------------------------------");

          latest.update(address, addressId, new DefaultId(address.getCustomer_id()));
          return latest;
        },
        Materialized.<DefaultId, LatestAddress, KeyValueStore<Bytes, byte[]>>
          as(childrenTopic + "_table_temporary")
          .withKeySerde(defaultIdSerde)
          .withValueSerde(latestAddressSerde)
          .withLoggingEnabled(stateStoreConfig)
        // Prevent read from changelog
        // .withLoggingDisabled()
      );

    //2b) aggregate addresses per customer id
    KTable<DefaultId, Addresses> addressTable = tempTable.toStream()
      .map((addressId, latestAddress) -> new KeyValue<>(latestAddress.getCustomerId(), latestAddress))
      .groupByKey(Serialized.with(defaultIdSerde, latestAddressSerde))
      .aggregate(
        Addresses::new,
        (customerId, latestAddress, addresses) -> {
          System.out.println("customerId >> " + customerId);
          System.out.println("latestAddress >> " + latestAddress);
          System.out.println("addresses >> " + addresses);
          System.out.println("------------------------------------------------------------------------");

          addresses.update(latestAddress);
          return addresses;
        },
        Materialized.<DefaultId, Addresses, KeyValueStore<Bytes, byte[]>>
          as(TABLE_AGGREGATE)
          .withKeySerde(defaultIdSerde)
          .withValueSerde(addressesSerde)
          .withLoggingEnabled(stateStoreConfig)
        // Prevent read from changelog
        // .withLoggingDisabled()
      );

    //3) KTable-KTable JOIN to combine customer and addresses
    KTable<DefaultId, CustomerAddressAggregate> dddAggregate =
      customerTable.join(addressTable, (customer, addresses) -> {
        System.out.println("customer.get_eventType() >> " + customer.get_eventType());

        return customer.get_eventType() == EventType.DELETE ?
          null : new CustomerAddressAggregate(customer, addresses.getEntries());
      });

    dddAggregate.toStream().to("final_ddd_aggregates",
      Produced.with(defaultIdSerde, (Serde) aggregateSerde));

    dddAggregate.toStream().print(Printed.toSysOut());

    final KafkaStreams streams = new KafkaStreams(builder.build(), props);
    streams.cleanUp();
    streams.start();

    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
  }
}
