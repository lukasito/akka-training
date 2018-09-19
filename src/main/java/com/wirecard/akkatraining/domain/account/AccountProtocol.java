package com.wirecard.akkatraining.domain.account;

import com.wirecard.akkatraining.domain.Confirmation;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;

public interface AccountProtocol {

  interface Command extends Serializable {
    default AccountRepositoryProtocol.Forward forwardTo(AccountId id) {
      return new AccountRepositoryProtocol.Forward(id, this);
    }
  }

  interface Event extends Serializable {
  }

  interface CommandRejection {
  }

  @Value
  class Create implements Command {
    String accountName;
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
  class Created implements Event {
    AccountId accountId;
    String accountName;
    BigDecimal balance;
    BigDecimal allocatedBalance;
  }

  @Value
  class MoneyAllocated implements Event, Confirmation {
    long deliveryId;
    TransferId transferId;
    AccountId debtor;
    AccountId creditor;
    BigDecimal amount;
  }

  @Value
  class DebitSuccessful implements Event, Confirmation {
    long deliveryId;
    AccountId debtor;
    PendingTransfer pendingTransfer;
  }

  @Value
  class CreditSuccessful implements Event, Confirmation {
    long deliveryId;
    TransferId transferId;
    BigDecimal amount;
    AccountId creditor;
  }

  @Value
  class DebitFailed implements CommandRejection {
    TransferId transferId;
    String reason;
  }

  @Value
  class MoneyAllocationFailed implements CommandRejection, Confirmation {
    long deliveryId;
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
