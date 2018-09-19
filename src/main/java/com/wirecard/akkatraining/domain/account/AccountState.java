package com.wirecard.akkatraining.domain.account;

import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
public class AccountState {

  BigDecimal balance;
  BigDecimal allocatedBalance;
  List<PendingTransfer> transfers;
}
