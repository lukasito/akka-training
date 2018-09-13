package com.wirecard.akkatraining.domain.transfer;

import com.wirecard.akkatraining.domain.account.AccountId;
import lombok.Value;

import java.math.BigDecimal;

public interface TransferProtocol {

  interface Event {
  }

  @Value
  class ExecuteTransfer {
    BigDecimal amount;
    AccountId creditor;
    AccountId debtor;
  }

  @Value
  class TransferInitiated implements Event {
    TransferId transferId;
    AccountId debtor;
    AccountId creditor;
    BigDecimal amount;
  }

  @Value
  class TransferCompleted implements Event {
    TransferId transferId;
  }

  @Value
  class TransferFailed implements Event {
    TransferId transferId;
    AccountId debtor;
    AccountId creditor;
    BigDecimal amount;
    String reason;
  }
}
