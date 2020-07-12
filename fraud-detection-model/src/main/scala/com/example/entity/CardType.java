package com.example.entity;

import javax.validation.ValidationException;

public enum CardType {
  SILVER(1),
  GOLD(2),
  PLATINUM(3);

  public final int numericType;

  CardType(int d) {
    this.numericType = d;
  }

  public static CardType convertFromNumeric(int d) {
    if (d == 1) {
      return SILVER;
    } else if (d == 2) {
      return GOLD;
    } else if (d == 3) {
      return PLATINUM;
    }

    throw new ValidationException("Invalid numeric type");
  }

  public static CardType convertFromLiteral(String s) {
    if (s.equals(SILVER.toString())) {
      return SILVER;
    } else if (s.equals(GOLD.toString())) {
      return GOLD;
    } else if (s.equals(PLATINUM.toString())) {
      return PLATINUM;
    }

    throw new ValidationException("Invalid literal type");
  }
}
