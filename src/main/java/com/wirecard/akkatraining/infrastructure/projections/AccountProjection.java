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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class AccountProjection implements WithTimeOffsetFromDataSource {

  private static final String INSERT_ACCOUNT =
    "insert into accounts (id, account_name, balance, created_on, udpated_on) " +
      "values (?,?,?,?,?)";

  private static final String INSERT_PENDING_TRANSFER =
    "insert into pending_transfers (account_id, transfer_id, allocated_money) " +
      "values (?,?,?)";

  private static final String REMOVE_PENDING_TRANSFER =
    "delete from pending_transfers where account_id = ? and transfer_id = ?";

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
      .map(EventEnvelope::event)
      .map(e -> (AccountProtocol.Event) e)
      .runWith(Sink.foreach(this::processEvent), materializer);
  }

  @SneakyThrows
  private void processEvent(AccountProtocol.Event event) {
    try (Connection c = dataSource.getConnection()) {
      if (event instanceof AccountProtocol.Created) {
        AccountProtocol.Created created = (AccountProtocol.Created) event;
        Timestamp now = Timestamp.from(Instant.now());
        PreparedStatement ps = c.prepareStatement(INSERT_ACCOUNT);
        ps.setString(1, created.accountId().value());
        ps.setString(2, created.accountName());
        ps.setBigDecimal(3, created.balance());
        ps.setTimestamp(4, now);
        ps.setTimestamp(5, now);
        ps.executeUpdate();
      } else if (event instanceof AccountProtocol.MoneyAllocated) {
        insertPendingTransfer(c, (AccountProtocol.MoneyAllocated) event);
      } else if (event instanceof AccountProtocol.DebitSuccessful) {
        releasePendingTransfer(c, (AccountProtocol.DebitSuccessful) event);
      }
    }
  }

  private void insertPendingTransfer(Connection c, AccountProtocol.MoneyAllocated event) throws SQLException {
    PreparedStatement ps = c.prepareStatement(INSERT_PENDING_TRANSFER);
    ps.setString(1, event.debtor().value());
    ps.setString(2, event.transferId().value());
    ps.setBigDecimal(3, event.amount());
    ps.executeUpdate();
  }

  private void releasePendingTransfer(Connection c, AccountProtocol.DebitSuccessful ds) throws SQLException {
    PreparedStatement ps = c.prepareStatement(REMOVE_PENDING_TRANSFER);
    ps.setString(1, ds.debtor().value());
    ps.setString(2, ds.pendingTransfer().id().value());
    ps.executeUpdate();
  }

  @Override
  public DataSource dataSource() {
    return dataSource;
  }
}
