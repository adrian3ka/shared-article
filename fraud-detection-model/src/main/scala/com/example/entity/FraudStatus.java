package com.example.entity;

import javax.validation.ValidationException;

public enum FraudStatus {
  SAFE(1),
  SUSPICIOUS(2),
  FRAUDSTER(3);

  public final int numericType;

  FraudStatus(int d) {
    this.numericType = d;
  }

  public static FraudStatus convertFromNumeric(int d) {
    if (d == 1) {
      return SAFE;
    } else if (d == 2) {
      return SUSPICIOUS;
    } else if (d == 3) {
      return FRAUDSTER;
    }

    throw new ValidationException("Invalid numeric type");
  }

  public static FraudStatus convertFromLiteral(String s) {
    if (s.equals(SAFE.toString())) {
      return SAFE;
    } else if (s.equals(SUSPICIOUS.toString())) {
      return SUSPICIOUS;
    } else if (s.equals(FRAUDSTER.toString())) {
      return FRAUDSTER;
    }

    throw new ValidationException("Invalid literal type");
  }
}
