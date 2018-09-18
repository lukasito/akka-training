package com.wirecard.akkatraining.domain.transfer;

import akka.actor.ActorRef;
import com.wirecard.akkatraining.domain.Confirmation;
import com.wirecard.akkatraining.domain.account.AccountId;
import lombok.Value;

import java.math.BigDecimal;

public interface TransferProtocol {

  interface Command {
  }

  interface Event {
  }

  @Value
  class ExecuteTransfer implements Command {
    BigDecimal amount;
    AccountId creditor;
    AccountId debtor;
  }

  @Value
  class MessageSent implements Event {
    Object message;
  }

  @Value
  class MessageConfirmed implements Event, Confirmation {
    long deliveryId;
    ConfirmationType confirmationType;
  }

  @Value
  class TransferInitiated implements Event {
    TransferId transferId;
    AccountId debtor;
    AccountId creditor;
    BigDecimal amount;
    ActorRef requester;
  }

  @Value
  class TransferCompleted implements Event {
    TransferId transferId;
    AccountId debtor;
    AccountId creditor;
    BigDecimal amount;
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
