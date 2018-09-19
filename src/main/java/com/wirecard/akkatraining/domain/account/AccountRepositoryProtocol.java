package com.wirecard.akkatraining.domain.account;

import lombok.Value;

import java.math.BigDecimal;

public interface AccountRepositoryProtocol {

  @Value
  class Forward {
    AccountId accountId;
    Object command;
  }

  @Value
  class Save {
    String accountName;
    AccountId accountId;
    BigDecimal balance;
    BigDecimal allocatedBalance;
  }
}
