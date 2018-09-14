package com.wirecard.akkatraining.application;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;

public class DomainEventListener extends AbstractLoggingActor {

  public static Props props() {
    return Props.create(DomainEventListener.class, DomainEventListener::new);
  }

  private DomainEventListener() {
    context().system().eventStream().subscribe(self(), TransferProtocol.Event.class);
    context().system().eventStream().subscribe(self(), AccountProtocol.Event.class);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Object.class, o -> log().info(o.toString()))
      .build();
  }
}
