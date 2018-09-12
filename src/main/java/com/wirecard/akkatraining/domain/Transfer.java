package com.wirecard.akkatraining.domain;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.AccountProtocol.AccountBalance;
import lombok.Value;

import java.math.BigDecimal;

public class Transfer extends AbstractLoggingActor {

  private ActorRef sender;
  private TransferId transferId;

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(TransferRequest.class, this::transfer)
      .matchAny(o -> log().error("Unknown message {}", o))
      .build();
  }

  private Receive transferringMoney() {
    return ReceiveBuilder.create()
      .match(AccountBalance.class, this::acceptBalance)
      .build();
  }

  private void acceptBalance(AccountBalance balance) {

  }

  private void transfer(TransferRequest transferRequest) {
    sender = sender();
    getContext().become(transferringMoney());
    BigDecimal amount = transferRequest.amount;
    transferRequest.debtor.tell(new AccountProtocol.Debit(amount), self());
  }

  @Value
  public static class TransferRequest {
    ActorRef debtor;
    ActorRef creditor;
    BigDecimal amount;
  }
}
