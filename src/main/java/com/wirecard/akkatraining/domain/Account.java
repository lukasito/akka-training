package com.wirecard.akkatraining.domain;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

import java.math.BigDecimal;

public class Account extends AbstractLoggingActor {

  private BigDecimal balance;

  private Account(BigDecimal balance) {
    this.balance = balance;
  }

  public static Props props(BigDecimal balance) {
    return Props.create(Account.class, () -> new Account(balance));
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(AccountProtocol.Credit.class, this::doCredit)
      .match(AccountProtocol.Debit.class, this::doDebit)
      .matchAny(o -> log().error("Unknown message {}", o))
      .build();
  }

  private void doDebit(AccountProtocol.Debit debit) {
    // business rule encapsulation
    BigDecimal debitAmount = debit.amount();
    if (balance.compareTo(debitAmount) >= 0) {
      balance = balance.subtract(debitAmount);
    }
  }

  private void doCredit(AccountProtocol.Credit credit) {
    // business rule encapsulation
    balance = balance.add(credit.amount());
  }
}
