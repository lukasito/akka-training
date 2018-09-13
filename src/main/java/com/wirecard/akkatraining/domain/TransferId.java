package com.wirecard.akkatraining.domain;

import lombok.Value;

import java.util.UUID;

@Value
public class TransferId {
  String value;

  public static TransferId newId() {
    return new TransferId(UUID.randomUUID().toString());
  }
}
