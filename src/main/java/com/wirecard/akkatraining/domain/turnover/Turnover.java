package com.wirecard.akkatraining.domain.turnover;

import com.wirecard.akkatraining.domain.account.AccountId;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
public class Turnover {

  AccountId accountId;
  int sigNum;
  BigDecimal amount;
  AccountId reference;
  LocalDateTime occurredOn;
}
