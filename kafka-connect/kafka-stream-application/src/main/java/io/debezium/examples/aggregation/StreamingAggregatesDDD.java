package io.debezium.examples.aggregation;

import io.debezium.examples.aggregation.model.User;
import io.debezium.examples.aggregation.serdes.SerdeFactory;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;

import java.util.Properties;

public class StreamingAggregatesDDD {
  private static final String AUTO_OFFSET_RESET_CONFIG = "earliest";
  private static final boolean ENABLE_AUTO_COMMIT_CONFIG = true;

  private static final String BOOTSTRAP_SERVER = "localhost:29092";
  private static final String INPUT_TOPIC = "quickstart-jdbc-test";

  public static void main(String[] args) {
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming-aggregates");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
    props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024);
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "60");
    props.put(TopicConfig.FLUSH_MESSAGES_INTERVAL_CONFIG, "60");
    props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");

    // CHANGE AND TRY CONFIG
    System.out.println("AUTO_OFFSET_RESET_CONFIG >> " + AUTO_OFFSET_RESET_CONFIG);
    System.out.println("ENABLE_AUTO_COMMIT_CONFIG >> " + ENABLE_AUTO_COMMIT_CONFIG);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_CONFIG);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT_CONFIG);

    final Serde<User> userSerde = SerdeFactory.createDbzEventJsonPojoSerdeFor(User.class, false);

    StreamsBuilder builder = new StreamsBuilder();

    final KStream<Long, User> userKStream =
      builder.stream(INPUT_TOPIC, Consumed.with(Serdes.Long(), userSerde));

    userKStream.print(Printed.toSysOut());

    final KafkaStreams streams = new KafkaStreams(builder.build(), props);
    streams.cleanUp();
    streams.start();

    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
  }
}
