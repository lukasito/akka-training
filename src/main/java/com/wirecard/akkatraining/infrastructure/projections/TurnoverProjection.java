package com.wirecard.akkatraining.infrastructure.projections;

import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.Offset;
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
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class TurnoverProjection {

  private static final String SELECT_LAST_OFFSET = "select offset from offsets limit 1";
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
      .getReadJournalFor(LeveldbReadJournal.class, "akka.persistence.query.journal.leveldb");
    materializer = ActorMaterializer.create(actorSystem);
    runStream();
  }

  private void runStream() {
    journal.eventsByTag(TransferProtocol.TransferCompleted.class.getName(), findOffset())
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

  @SneakyThrows
  private Offset findOffset() {
    try (Connection c = dataSource.getConnection()) {
      ResultSet resultSet = c.createStatement().executeQuery(SELECT_LAST_OFFSET);
      resultSet.next();
      if (resultSet.next()) {
        return Offset.timeBasedUUID(UUID.fromString(resultSet.getString(1)));
      }
    }
    return Offset.noOffset();
  }
}
