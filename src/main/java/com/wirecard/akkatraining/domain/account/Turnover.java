package com.wirecard.akkatraining.domain.account;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class Turnover {

  int sigNum;
  BigDecimal amount;
  AccountId reference;
  LocalDateTime occurredOn;
}
