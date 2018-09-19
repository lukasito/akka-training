package com.wirecard.akkatraining.infrastructure.projections;

import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.journal.leveldb.javadsl.LeveldbReadJournal;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.transfer.Status;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class TransferProjection implements WithTimeOffsetFromDataSource {

  private static final String INSERT_TURNOVER =
    "insert into turnovers (transfer_id, account_id, sigNum, occurred_on) " +
      "values (?,?,?,?)";

  private static final String INSERT_TRANSFER =
    "insert into transfers (id, debtor, creditor, amount, status, created_on, updated_on) " +
      "values (?,?,?,?,?,?,?)";

  private final Materializer materializer;
  private final LeveldbReadJournal journal;
  private final DataSource dataSource;

  public TransferProjection(
    ActorSystem actorSystem,
    DataSource dataSource
  ) {
    this.dataSource = dataSource;
    journal = PersistenceQuery.get(actorSystem)
      .getReadJournalFor(LeveldbReadJournal.class, LeveldbReadJournal.Identifier());
    materializer = ActorMaterializer.create(actorSystem);
    runStream();
  }

  private void runStream() {
    journal.eventsByTag(TransferProtocol.Event.class.getName(), findOffset("transfer"))
      .map(EventEnvelope::event)
      .map(event -> (TransferProtocol.Event) event)
      .runWith(Sink.foreach(this::processEvent), materializer);
  }

  @SneakyThrows
  private void processEvent(TransferProtocol.Event event) {
    try (Connection c = dataSource.getConnection()) {
      Timestamp occurredOn = Timestamp.from(Instant.now());
      if (event instanceof TransferProtocol.TransferCompleted) {
        TransferProtocol.TransferCompleted tc = (TransferProtocol.TransferCompleted) event;
        insertTransfer(c, tc.transferId(), tc.debtor(), tc.creditor(), tc.amount(), Status.SUCCESS.name());
        insertTurnover(c, tc.transferId(), tc.creditor(), 1, occurredOn);
        insertTurnover(c, tc.transferId(), tc.debtor(), -1, occurredOn);
      } else if (event instanceof TransferProtocol.TransferFailed) {
        TransferProtocol.TransferFailed tf = (TransferProtocol.TransferFailed) event;
        insertTransfer(c, tf.transferId(), tf.debtor(), tf.creditor(), tf.amount(), Status.FAILED.name());
      }
    }
  }

  private void insertTransfer(
    Connection c,
    TransferId transferId,
    AccountId debtor,
    AccountId creditor,
    BigDecimal amount,
    String status
  ) throws SQLException {
    PreparedStatement ps = c.prepareStatement(INSERT_TRANSFER);
    Timestamp now = Timestamp.from(Instant.now());
    ps.setString(1, transferId.value());
    ps.setString(2, debtor.value());
    ps.setString(3, creditor.value());
    ps.setBigDecimal(4, amount);
    ps.setString(5, status);
    ps.setTimestamp(6, now);
    ps.setTimestamp(7, now);
    ps.executeUpdate();
  }

  private void insertTurnover(
    Connection c,
    TransferId id,
    AccountId accountId,
    int sigNum,
    Timestamp occurredOn
  ) throws SQLException {
    PreparedStatement ps = c.prepareStatement(INSERT_TURNOVER);
    ps.setString(1, id.value());
    ps.setString(2, accountId.value());
    ps.setInt(3, sigNum);
    ps.setTimestamp(4, occurredOn);
    ps.executeUpdate();
  }

  @Override
  public DataSource dataSource() {
    return dataSource;
  }
}
