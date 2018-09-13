package com.wirecard.akkatraining.application;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.transfer.Transfer;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;
import com.wirecard.akkatraining.infrastructure.InMemoryAccountRepository;

import java.math.BigDecimal;

public class Main {

  public static void main(String[] args) {
    ActorSystem actorSystem = ActorSystem.create();

    actorSystem.actorOf(DomainEventListener.props(), "domain-event-listener");
    ActorRef accountRepository = actorSystem.actorOf(InMemoryAccountRepository.props(), "accountRepository");

    TransferId transferId = new TransferId("Transfer-123");
    ActorRef transfer = actorSystem.actorOf(Transfer.props(accountRepository), transferId.value());


    AccountId debtor = AccountId.of("Account-1");
    AccountId creditor = AccountId.of("Account-2");
    transfer.tell(new TransferProtocol.ExecuteTransfer(BigDecimal.ONE, creditor, debtor), ActorRef.noSender());
  }

  private static class DomainEventListener extends AbstractLoggingActor {

    static Props props() {
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
}
