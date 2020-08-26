package io.debezium.examples.aggregation.serdes;

import com.google.gson.Gson;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonDeserializer<T> implements Deserializer<T> {

  private Gson gson = new Gson();
  private Class<T> deserializedClass;

  public JsonDeserializer(final Class<T> deserializedClass) {
    this.deserializedClass = deserializedClass;
  }

  public JsonDeserializer() {
  }

  @Override
  @SuppressWarnings("unchecked")
  public void configure(final Map<String, ?> map, final boolean b) {
    if (deserializedClass == null) {
      deserializedClass = (Class<T>) map.get("serializedClass");
    }
  }

  @Override
  public T deserialize(final String topic, final byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    System.out.println(deserializedClass);
    System.out.println(topic);
    System.out.println(new String(bytes, StandardCharsets.UTF_8));

    return gson.fromJson(new String(bytes), deserializedClass);
  }

  @Override
  public void close() {

  }
}
