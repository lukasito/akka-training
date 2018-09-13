package com.wirecard.akkatraining.domain.account;

import com.wirecard.akkatraining.domain.transfer.TransferId;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class PendingTransfer {
  TransferId id;
  BigDecimal amount;
  AccountId creditor;
}
