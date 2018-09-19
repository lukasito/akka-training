package com.wirecard.akkatraining.domain.account;

import com.wirecard.akkatraining.domain.transfer.TransferId;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;

@Value
public class PendingTransfer implements Serializable {
  TransferId transferId;
  BigDecimal amount;
  AccountId creditor;
}
