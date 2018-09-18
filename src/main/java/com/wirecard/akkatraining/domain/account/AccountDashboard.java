package com.wirecard.akkatraining.domain.account;

import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Value
public class AccountDashboard {

  AccountId accountId;
  String accountName;
  BigDecimal balance;
  List<Turnover> turnovers;
  List<PendingTransfer> pendingTransfers;

  public BigDecimal pendingTransferSum() {
    return pendingTransfers.stream()
      .map(PendingTransfer::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add)
      .setScale(2, RoundingMode.HALF_UP);
  }
}
