package com.wirecard.akkatraining.infrastructure.projections;

import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.Sequence;
import akka.persistence.query.journal.leveldb.javadsl.LeveldbReadJournal;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.account.PendingTransfer;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import com.wirecard.akkatraining.domain.view.Account;
import com.wirecard.akkatraining.infrastructure.repository.InMemoryAccountViewViewRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class InMemoryAccountProjection {

  private final Materializer materializer;
  private final LeveldbReadJournal journal;
  private final InMemoryAccountViewViewRepository accountRepository;

  public InMemoryAccountProjection(
    ActorSystem actorSystem,
    InMemoryAccountViewViewRepository accountRepository
  ) {
    journal = PersistenceQuery.get(actorSystem)
      .getReadJournalFor(LeveldbReadJournal.class, LeveldbReadJournal.Identifier());
    materializer = ActorMaterializer.create(actorSystem);
    this.accountRepository = accountRepository;
  }

  public void runStream() {
    journal.eventsByTag(AccountProtocol.Event.class.getName(), new Sequence(0L))
      .map(EventEnvelope::event)
      .map(e -> (AccountProtocol.Event) e)
      .runWith(Sink.foreach(this::processEvent), materializer);
  }

  @SneakyThrows
  private void processEvent(AccountProtocol.Event event) {
    log.info("Got event: {}", event);
    if (event instanceof AccountProtocol.Created) {
      AccountProtocol.Created created = (AccountProtocol.Created) event;
      Account account = Account.builder()
        .accountId(created.accountId())
        .accountName(created.accountName())
        .balance(created.balance())
        .pendingTransfers(new ArrayList<>())
        .turnovers(new ArrayList<>())
        .build();
      accountRepository.add(account);
    } else if (event instanceof AccountProtocol.MoneyAllocated) {
      addPendingTransfer((AccountProtocol.MoneyAllocated) event);
    } else if (event instanceof AccountProtocol.DebitSuccessful) {
      removePendingTransfer((AccountProtocol.DebitSuccessful) event);
    }
  }

  private void addPendingTransfer(AccountProtocol.MoneyAllocated event) {
    PendingTransfer pendingTransfer = new PendingTransfer(event.transferId(), event.amount(), event.creditor());
    Account account = accountRepository.find(event.debtor());
    account.addPendingTransfer(pendingTransfer);
    log.info("Added pending transfer: {}", pendingTransfer);
  }

  private void removePendingTransfer(AccountProtocol.DebitSuccessful ds) {
    Account account = accountRepository.find(ds.debtor());
    TransferId transferId = ds.pendingTransfer().transferId();
    account.removePendingTransfer(transferId);
    log.info("Pending transfer removed: {}", transferId);
  }
}
