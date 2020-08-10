package io.debezium.examples.aggregation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.debezium.examples.aggregation.StreamingAggregatesDDD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomerAddressAggregate {
  private static final Logger LOGGER = LoggerFactory.getLogger(CustomerAddressAggregate.class);


  private final Customer customer;

  private final List<Address> addresses;

  @JsonCreator
  public CustomerAddressAggregate(
    @JsonProperty("customer") Customer customer,
    @JsonProperty("addresses") List<Address> addresses
  ) {
    LOGGER.info("CustomerAddressAggregate is called");
    LOGGER.info("customer >> " + customer);
    LOGGER.info("addresses >> " + addresses);

    this.customer = customer;
    this.addresses = addresses;
  }

  public Customer getCustomer() {
    return customer;
  }

  public List<Address> getAddresses() {
    return addresses;
  }

  @Override
  public String toString() {
    return "CustomerAddressAggregate{" +
      "customer=" + customer +
      ", addresses=" + addresses +
      '}';
  }

}
