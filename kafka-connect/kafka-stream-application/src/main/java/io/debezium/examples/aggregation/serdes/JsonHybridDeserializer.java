package io.debezium.examples.aggregation.serdes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.examples.aggregation.model.EventType;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonHybridDeserializer<T> implements Deserializer<T> {
  public static final String DBZ_CDC_EVENT_PAYLOAD_FIELD = "payload";

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private Class<T> clazz;

  @SuppressWarnings("unchecked")
  @Override
  public void configure(Map<String, ?> props, boolean isKey) {
    clazz = (Class<T>) props.get("serializedClass");
  }

  @Override
  public T deserialize(String topic, byte[] bytes) {
    if (bytes == null)
      return null;

    T data;

    System.out.println(new String(bytes));
    System.out.println("Class >> " + clazz);

    // try to deserialize from DBZ json event
    
    try {
      Map temp = (Map) OBJECT_MAPPER.readValue(new String(bytes), Map.class)
        .get(DBZ_CDC_EVENT_PAYLOAD_FIELD);

      System.out.println("Payload >> " + temp);
      if (temp == null) {
        temp = new HashMap();
        //NOTE: we record a delete event in this case
        temp.put("_eventType", EventType.DELETE);
      }
      data = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsBytes(temp), clazz);
    } catch (IOException e2) {
      throw new SerializationException(e2);
    }

    return data;
  }

  @Override
  public void close() {
  }
}
