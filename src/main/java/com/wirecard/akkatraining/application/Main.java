package com.wirecard.akkatraining.application;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.wirecard.akkatraining.domain.Account;
import com.wirecard.akkatraining.domain.AccountNumber;

import java.math.BigDecimal;

public class Main {

  public static void main(String[] args) {
    ActorSystem actorSystem = ActorSystem.create();

    AccountNumber accountNumber = AccountNumber.of("123456");
    ActorRef account = actorSystem.actorOf(Account.props(BigDecimal.ONE, accountNumber), accountNumber.toString());

    actorSystem.terminate();
  }
}
