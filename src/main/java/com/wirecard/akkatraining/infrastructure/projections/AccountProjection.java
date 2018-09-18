package com.wirecard.akkatraining.infrastructure.projections;

import akka.actor.ActorSystem;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.journal.leveldb.javadsl.LeveldbReadJournal;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

public class AccountProjection implements WithTimeOffsetFromDataSource {

  private static final String INSERT_ACCOUNT = "insert into accounts (id, account_name, balance, allocated_balance, created_on, udpated_on) values (?,?,?,?,?,?)";

  private final Materializer materializer;
  private final LeveldbReadJournal journal;
  private final DataSource dataSource;

  public AccountProjection(
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
    journal.eventsByTag(AccountProtocol.Event.class.getName(), findOffset("account"))
      .runWith(Sink.foreach(this::processEvent), materializer);
  }

  @SneakyThrows
  private void processEvent(EventEnvelope eventEnvelope) {
    Object event = eventEnvelope.event();
    try (Connection c = dataSource.getConnection()) {
      if (event instanceof AccountProtocol.Created) {
        AccountProtocol.Created created = (AccountProtocol.Created) event;
        Timestamp now = Timestamp.from(Instant.now());
        PreparedStatement ps = c.prepareStatement(INSERT_ACCOUNT);
        ps.setString(1, eventEnvelope.persistenceId());
        ps.setString(2, created.accountName());
        ps.setBigDecimal(3, created.balance());
        ps.setBigDecimal(4, created.allocatedBalance());
        ps.setTimestamp(5, now);
        ps.setTimestamp(6, now);
        ps.executeUpdate();
      }
      // TODO credit, debit, money allocated
    }
  }

  @Override
  public DataSource dataSource() {
    return dataSource;
  }
}
