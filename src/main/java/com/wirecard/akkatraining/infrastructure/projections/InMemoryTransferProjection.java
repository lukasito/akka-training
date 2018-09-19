package com.wirecard.akkatraining.infrastructure.projections;

import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.Sequence;
import akka.persistence.query.journal.leveldb.javadsl.LeveldbReadJournal;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.transfer.Status;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;
import com.wirecard.akkatraining.domain.view.AccountViewRepository;
import com.wirecard.akkatraining.domain.view.Transfer;
import com.wirecard.akkatraining.domain.view.Turnover;
import com.wirecard.akkatraining.infrastructure.repository.InMemoryTransferDao;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class InMemoryTransferProjection {

  private final Materializer materializer;
  private final InMemoryTransferDao transferDao;
  private final LeveldbReadJournal journal;
  private final AccountViewRepository accountViewRepository;

  public InMemoryTransferProjection(
    ActorSystem actorSystem,
    InMemoryTransferDao transferDao,
    AccountViewRepository accountViewRepository) {
    journal = PersistenceQuery.get(actorSystem)
      .getReadJournalFor(LeveldbReadJournal.class, LeveldbReadJournal.Identifier());
    materializer = ActorMaterializer.create(actorSystem);
    this.transferDao = transferDao;
    this.accountViewRepository = accountViewRepository;
  }

  public void runStream() {
    journal.eventsByTag(TransferProtocol.Event.class.getName(), new Sequence(0L))
      .map(EventEnvelope::event)
      .map(event -> (TransferProtocol.Event) event)
      .runWith(Sink.foreach(this::processEvent), materializer);
  }

  @SneakyThrows
  private void processEvent(TransferProtocol.Event event) {
    log.info("Got event: {}", event);
    if (event instanceof TransferProtocol.TransferCompleted) {
      TransferProtocol.TransferCompleted tc = (TransferProtocol.TransferCompleted) event;
      insertTransfer(tc.transferId(), tc.debtor(), tc.creditor(), tc.amount(), Status.SUCCESS.name());
      insertTurnover(tc.creditor(), tc.debtor(), BigDecimal.ONE, tc.amount());
      insertTurnover(tc.debtor(), tc.creditor(), BigDecimal.valueOf(-1), tc.amount());
    } else if (event instanceof TransferProtocol.TransferFailed) {
      TransferProtocol.TransferFailed tf = (TransferProtocol.TransferFailed) event;
      insertTransfer(tf.transferId(), tf.debtor(), tf.creditor(), tf.amount(), Status.FAILED.name());
    }
  }

  private void insertTransfer(
    TransferId transferId,
    AccountId debtor,
    AccountId creditor,
    BigDecimal amount,
    String status
  ) {
    Transfer transfer = Transfer.builder()
      .transferId(transferId)
      .debtor(debtor)
      .creditor(creditor)
      .amount(amount)
      .status(status)
      .build();
    transferDao.add(transfer);
  }

  private void insertTurnover(
    AccountId accountId,
    AccountId reference,
    BigDecimal sigNum,
    BigDecimal amount
  ) {
    Turnover turnover = new Turnover(accountId, amount.multiply(sigNum), reference);
    accountViewRepository.find(accountId).addTurnover(turnover);
  }
}
