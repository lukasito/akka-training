package com.wirecard.akkatraining.application;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol.Save;
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

    AccountId debtor = AccountId.of("Account-1");
    AccountId creditor = AccountId.of("Account-2");
    Save acc1 = new Save(debtor, BigDecimal.TEN, BigDecimal.ZERO);
    Save acc2 = new Save(creditor, BigDecimal.ONE, BigDecimal.ZERO);

    accountRepository.tell(acc1, ActorRef.noSender());
    accountRepository.tell(acc2, ActorRef.noSender());

    TransferId transferId = new TransferId("Transfer-123");
    ActorRef transfer = actorSystem.actorOf(Transfer.props(accountRepository), transferId.value());


    transfer.tell(new TransferProtocol.ExecuteTransfer(BigDecimal.ONE, creditor, debtor), ActorRef.noSender());
  }
}
