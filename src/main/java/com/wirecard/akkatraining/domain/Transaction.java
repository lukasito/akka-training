package com.wirecard.akkatraining.domain;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
class Transaction {
  PaymentId paymentId;
  String type;
  Price price;
  LocalDateTime completionTime;

  public BigDecimal value() {
    return price.value();
  }
}
