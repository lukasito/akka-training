package com.wirecard.akkatraining.domain;

import lombok.Value;

import java.math.BigDecimal;

public interface AccountProtocol {

  interface Command {

  }

  interface Event {
  }

  interface CommandRejection {
  }

  @Value
  class CompleteTransfer implements Command {
    TransferId transferId;
  }

  @Value
  class CancelTransfer implements Command {
    TransferId transferId;
  }

  @Value
  class InitiateTransfer implements Command {
    AccountNumber creditor;
    BigDecimal amount;
  }

  @Value
  class TransferInitiationFailed implements CommandRejection {
    BigDecimal amount;
    String reason;
  }

  @Value
  class TransferInitiated implements Event {
    TransferId transferId;
    AccountNumber debtor;
    AccountNumber creditor;
    BigDecimal amount;
  }

  @Value
  class TransferCompleted implements Event {
    TransferId transferId;
  }

  @Value
  class TransferCancelled implements Event {
    TransferId transferId;
  }

  @Value
  class TransferCancellationFailed implements CommandRejection {
    TransferId transferId;
  }

  @Value
  class GetAccountOverview {
    private static final GetAccountOverview instance = new GetAccountOverview();

    public static GetAccountOverview instance() {
      return instance;
    }
  }

  @Value
  class AccountOverview {
    BigDecimal balance;
    BigDecimal blockedMoney;
    Integer pendingTransfers;
  }
}
