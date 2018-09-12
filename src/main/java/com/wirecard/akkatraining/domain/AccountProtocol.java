package com.wirecard.akkatraining.domain;

import lombok.Value;

import java.math.BigDecimal;

public interface AccountProtocol {

  @Value
  class Credit {
    BigDecimal amount;
  }

  @Value
  class Debit {
    BigDecimal amount;
  }

  class ShowBalance {

    private static final ShowBalance instance = new ShowBalance();

    public static ShowBalance instance() {
      return instance;
    }
  }

  @Value
  class AccountBalance {
    BigDecimal balance;
  }
}
