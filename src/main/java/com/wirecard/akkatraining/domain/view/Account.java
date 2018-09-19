package com.wirecard.akkatraining.domain.view;


import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.PendingTransfer;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class Account {
  private AccountId accountId;
  private String accountName;
  private BigDecimal balance;
  private List<Turnover> turnovers;
  private List<PendingTransfer> pendingTransfers;

  public BigDecimal allocatedBalance() {
    return pendingTransfers.stream()
      .map(PendingTransfer::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add)
      .setScale(2, RoundingMode.HALF_UP);
  }

  public void addPendingTransfer(PendingTransfer pendingTransfer) {
    this.pendingTransfers.add(pendingTransfer);
  }

  public void removePendingTransfer(TransferId id) {
    pendingTransfers.stream()
      .filter(pt -> pt.transferId().equals(id))
      .findFirst()
      .ifPresent(pendingTransfers::remove);
  }

  public void addTurnover(Turnover turnover) {
    turnovers.add(turnover);
  }

  @Override
  public String toString() {
    return "Account{" +
      "accountId=" + accountId +
      ", accountName='" + accountName + '\'' +
      ", balance=" + balance +
      ", allocatedBalance=" + allocatedBalance() +
      ", turnovers=" + turnovers +
      ", pendingTransfers=" + pendingTransfers +
      '}';
  }
}