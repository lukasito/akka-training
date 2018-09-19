package com.wirecard.akkatraining.application;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol.Save;
import com.wirecard.akkatraining.domain.transfer.Transfer;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;
import com.wirecard.akkatraining.domain.view.Account;
import com.wirecard.akkatraining.domain.view.AccountViewRepository;
import com.wirecard.akkatraining.infrastructure.projections.InMemoryAccountProjection;
import com.wirecard.akkatraining.infrastructure.projections.InMemoryTransferProjection;
import com.wirecard.akkatraining.infrastructure.repository.InMemoryAccountRepository;
import com.wirecard.akkatraining.infrastructure.repository.InMemoryAccountViewViewRepository;
import com.wirecard.akkatraining.infrastructure.repository.InMemoryTransferDao;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class Main {

  public static void main(String[] args) throws Exception {
    ActorSystem actorSystem = ActorSystem.create();

    AccountViewRepository viewRepository = startProjecting(actorSystem);

    actorSystem.actorOf(DomainEventListener.props(), "domain-event-listener");
    ActorRef accountRepository = actorSystem.actorOf(InMemoryAccountRepository.props(), "accountRepository");

    AccountId debtor = AccountId.of("Account-1");
    AccountId creditor = AccountId.of("Account-2");
    Save acc1 = new Save("debtors-account", debtor, BigDecimal.TEN, BigDecimal.ZERO);
    Save acc2 = new Save("creditors-account", creditor, BigDecimal.ONE, BigDecimal.ZERO);

    accountRepository.tell(acc1, ActorRef.noSender());
    accountRepository.tell(acc2, ActorRef.noSender());

    TransferId transferId = new TransferId("Transfer-123");
    ActorRef transfer = actorSystem.actorOf(Transfer.props(accountRepository), transferId.value());

    transfer.tell(new TransferProtocol.ExecuteTransfer(BigDecimal.ONE, creditor, debtor), ActorRef.noSender());

    Thread.sleep(10_000);
    Account account1 = viewRepository.find(debtor);
    Account account2 = viewRepository.find(creditor);

    log.info("Projection of debtor: {}", account1);
    log.info("Projection of creditor: {}", account2);
  }

  private static AccountViewRepository startProjecting(ActorSystem actorSystem) {
    InMemoryAccountViewViewRepository accountViewRepository = new InMemoryAccountViewViewRepository();
    InMemoryTransferProjection inMemoryTransferProjection = new InMemoryTransferProjection(actorSystem, new InMemoryTransferDao(), accountViewRepository);
    InMemoryAccountProjection inMemoryAccountProjection = new InMemoryAccountProjection(actorSystem, accountViewRepository);

    inMemoryAccountProjection.runStream();
    inMemoryTransferProjection.runStream();

    return accountViewRepository;
  }
}
