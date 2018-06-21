package com.wirecard.akkatraining.domain;

import lombok.Value;

@Value
public class PaymentId {

  String value;

  public String asString() {
    return value;
  }
}
