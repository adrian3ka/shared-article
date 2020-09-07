package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class User {
  private final Integer id;
  private final String name;
  private final String email;
  private final String department;

  @JsonCreator
  public User(
    @JsonProperty("id") Integer id,
    @JsonProperty("name") String name,
    @JsonProperty("email") String email,
    @JsonProperty("department") String department
  ) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.department = department;

    System.out.println(this.toString());
  }

  @Override
  public String toString() {
    return "User{" +
      "id=" + id +
      ", name=" + name +
      ", email='" + email + '\'' +
      ", department='" + department + '\'' +
      '}';
  }
}
