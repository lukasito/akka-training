package com.wirecard.akkatraining.domain.account;

import lombok.Value;

public interface AccountRepositoryProtocol {

  @Value
  class Forward {
    AccountId accountId;
    AccountProtocol.Command command;
  }
}
