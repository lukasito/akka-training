package com.wirecard.akkatraining.domain.view;

import com.wirecard.akkatraining.domain.account.AccountId;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class Turnover {

  AccountId owner;
  BigDecimal amount;
  AccountId reference;
}
