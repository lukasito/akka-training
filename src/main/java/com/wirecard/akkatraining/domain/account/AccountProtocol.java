package com.wirecard.akkatraining.domain.account;

import com.wirecard.akkatraining.domain.transfer.TransferId;
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
  class Initialize {
    AccountId accountId;
    BigDecimal balance;
    BigDecimal allocatedBalance;
  }

  @Value
  class AllocateMoney implements Command {
    TransferId transferId;
    AccountId creditor;
    BigDecimal amount;
  }

  @Value
  class Debit implements Command {
    TransferId transferId;
  }

  @Value
  class Credit implements Command {
    TransferId transferId;
    BigDecimal amount;
  }

  @Value
  class MoneyAllocated implements Event {
    TransferId transferId;
    AccountId debtor;
    BigDecimal amount;
  }

  @Value
  class DebitSuccessful implements Event {
    TransferId transferId;
    AccountId debtor;
  }

  @Value
  class DebitFailed implements Event {
    TransferId transferId;
    String reason;
  }

  @Value
  class CreditSuccessful implements Event {
    TransferId transferId;
    AccountId creditor;
  }

  @Value
  class MoneyAllocationFailed implements CommandRejection {
    TransferId transferId;
    AccountId debtor;
    String reason;
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
    BigDecimal allocatedBalance;
    Integer pendingTransfers;
  }
}
