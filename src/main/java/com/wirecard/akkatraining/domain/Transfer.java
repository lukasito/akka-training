package com.wirecard.akkatraining.domain;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class Transfer {

  TransferId id;
  BigDecimal amount;
  AccountNumber creditor;

  public static Transfer newTransfer(BigDecimal amount, AccountNumber creditor) {
    return new Transfer(TransferId.newId(), amount, creditor);
  }
}
