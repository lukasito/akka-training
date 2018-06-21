package com.wirecard.akkatraining.application;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.wirecard.akkatraining.domain.Payment;
import com.wirecard.akkatraining.domain.PaymentProtocol.Purchase;
import com.wirecard.akkatraining.domain.Price;

import java.math.BigDecimal;
import java.util.UUID;

public class Main {

  public static void main(String[] args) {
    ActorSystem actorSystem = ActorSystem.create();

    ActorRef payment = actorSystem.actorOf(Payment.props(), "payment-" + UUID.randomUUID());

    payment.tell(new Purchase(new Price(BigDecimal.ONE, "EUR")), ActorRef.noSender());
    payment.tell(new Purchase(new Price(BigDecimal.ONE, "EUR")), ActorRef.noSender());

    actorSystem.terminate();
  }
}
