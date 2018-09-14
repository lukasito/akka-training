package com.wirecard.akkatraining.domain;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import com.wirecard.akkatraining.application.DomainEventListener;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.GetAccountOverview;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol.Forward;
import com.wirecard.akkatraining.domain.transfer.Transfer;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.ExecuteTransfer;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferCompleted;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferFailed;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferInitiated;
import com.wirecard.akkatraining.infrastructure.InMemoryAccountRepository;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static akka.actor.ActorRef.noSender;
import static com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol.Save;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest extends AbstractActorSystemTest {

  private ActorRef accountRepository;

  protected void before() {
    accountRepository = system().actorOf(InMemoryAccountRepository.props(), "account-repository");
    system().actorOf(DomainEventListener.props(), "domain-event-listener");
  }

  @Test
  public void thatTransferCanBeExecuted() {
    TestKit probe = new TestKit(system());
    AccountId debtor = AccountId.of("Account-123456");
    AccountId creditor = AccountId.of("Account-23133");

    accountRepository.tell(new Save(debtor, BigDecimal.TEN, BigDecimal.ZERO), noSender());
    accountRepository.tell(new Save(creditor, BigDecimal.ONE, BigDecimal.ZERO), noSender());

    ActorRef transfer = newTransfer();

    transfer.tell(new ExecuteTransfer(new BigDecimal("5"), creditor, debtor), probe.getRef());

    probe.expectMsgClass(TransferInitiated.class);
    probe.expectMsgClass(TransferCompleted.class);

    assertAccountOverview(probe, debtor, new BigDecimal("5"), BigDecimal.ZERO, 0);
    assertAccountOverview(probe, creditor, new BigDecimal("6"), BigDecimal.ZERO, 0);
  }

  @Test
  public void thatTransferFails() {
    TestKit probe = new TestKit(system());
    AccountId debtor = AccountId.of("Account-123456");
    AccountId creditor = AccountId.of("Account-23133");

    accountRepository.tell(new Save(debtor, BigDecimal.TEN, new BigDecimal("6")), noSender());
    accountRepository.tell(new Save(creditor, BigDecimal.ONE, BigDecimal.ZERO), noSender());

    ActorRef transfer = newTransfer();

    transfer.tell(new ExecuteTransfer(new BigDecimal("5"), creditor, debtor), probe.getRef());

    probe.expectMsgClass(TransferInitiated.class);
    TransferFailed transferFailed = probe.expectMsgClass(TransferFailed.class);

    assertThat(transferFailed)
      .hasFieldOrPropertyWithValue("reason", "Not enough balance!");
  }

  private ActorRef newTransfer() {
    return system().actorOf(Transfer.props(accountRepository), "Transfer-" + UUID.randomUUID());
  }

  private void accountOverview(AccountId accountId, TestKit probe) {
    accountRepository.tell(new Forward(accountId, GetAccountOverview.instance()), probe.getRef());
  }

  private void assertAccountOverview(
    TestKit probe,
    AccountId accountId,
    BigDecimal balance,
    BigDecimal blocked,
    int pendingTransfers
  ) {
    accountOverview(accountId, probe);
    AccountOverview accountOverview = probe.expectMsgClass(AccountOverview.class);
    assertThat(accountOverview)
      .extracting("balance", "allocatedBalance", "pendingTransfers")
      .containsExactly(balance, blocked, pendingTransfers);
  }
}
