package com.wirecard.akkatraining.domain;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class Price {
  BigDecimal value;
  String currency;

  public boolean isPossitive() {
    return value.compareTo(BigDecimal.ZERO) > 0;
  }
}
