package com.wirecard.akkatraining.infrastructure.projections;

import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.journal.leveldb.javadsl.LeveldbReadJournal;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

public class TurnoverProjection implements WithTimeOffsetFromDataSource {

  private static final String INSERT_TURNOVER = "insert into turnovers (transfer_id, account_id, sigNum, amount, reference_account_id, occurred_on) values (?,?,?,?,?,?)";

  private final Materializer materializer;
  private final LeveldbReadJournal journal;
  private final DataSource dataSource;

  public TurnoverProjection(
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
    journal.eventsByTag(TransferProtocol.TransferCompleted.class.getName(), findOffset("turnover"))
      .map(EventEnvelope::event)
      .map(event -> (TransferProtocol.TransferCompleted) event)
      .runWith(Sink.foreach(this::processEvent), materializer);
  }

  @SneakyThrows
  private void processEvent(TransferProtocol.TransferCompleted event) {
    try (Connection c = dataSource.getConnection()) {
      Timestamp occurredOn = Timestamp.from(Instant.now());
      PreparedStatement creditorsTurnover = c.prepareStatement(INSERT_TURNOVER);
      creditorsTurnover.setString(1, event.transferId().value());
      creditorsTurnover.setString(2, event.creditor().value());
      creditorsTurnover.setInt(3, 1);
      creditorsTurnover.setBigDecimal(4, event.amount());
      creditorsTurnover.setString(5, event.debtor().value());
      creditorsTurnover.setTimestamp(6, occurredOn);
      creditorsTurnover.executeUpdate();

      PreparedStatement debtorsTurnover = c.prepareStatement(INSERT_TURNOVER);
      creditorsTurnover.setString(1, event.transferId().value());
      creditorsTurnover.setString(2, event.debtor().value());
      creditorsTurnover.setInt(3, -1);
      creditorsTurnover.setBigDecimal(4, event.amount());
      creditorsTurnover.setString(5, event.creditor().value());
      creditorsTurnover.setTimestamp(6, occurredOn);
      debtorsTurnover.executeUpdate();
    }
  }

  @Override
  public DataSource dataSource() {
    return dataSource;
  }
}
