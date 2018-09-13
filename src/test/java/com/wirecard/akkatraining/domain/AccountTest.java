package com.wirecard.akkatraining.domain;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.CancelTransfer;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Debit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.GetAccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.TransferMoney;
import com.wirecard.akkatraining.domain.account.AccountProtocol.TransferCancelled;
import com.wirecard.akkatraining.domain.account.AccountProtocol.TransferCompleted;
import com.wirecard.akkatraining.domain.account.AccountProtocol.TransferInitiated;
import org.junit.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest extends AbstractActorSystemTest {

  @Test
  public void thatTransferCanBeInitiated() {
    TestKit probe = new TestKit(system());

    AccountId debtor = AccountId.of("Account-123456");
    AccountId creditor = AccountId.of("Account-23133");
    ActorRef account = system().actorOf(Account.props(BigDecimal.TEN, debtor), debtor.value());

    account.tell(new TransferMoney(creditor, BigDecimal.ONE), probe.getRef());
    probe.expectMsgClass(TransferInitiated.class);

    account.tell(GetAccountOverview.instance(), probe.getRef());
    assertAccountOverview(probe, BigDecimal.TEN, BigDecimal.ONE, 1);
  }

  @Test
  public void thatTransferCanBeCanceled() {
    TestKit probe = new TestKit(system());

    AccountId debtor = AccountId.of("Account-123456");
    AccountId creditor = AccountId.of("Account-23133");
    ActorRef account = system().actorOf(Account.props(BigDecimal.TEN, debtor), debtor.value());

    account.tell(new TransferMoney(creditor, BigDecimal.ONE), probe.getRef());
    TransferInitiated transferInitiated = probe.expectMsgClass(TransferInitiated.class);

    account.tell(new CancelTransfer(transferInitiated.transferId()), probe.getRef());
    probe.expectMsgClass(TransferCancelled.class);

    account.tell(GetAccountOverview.instance(), probe.getRef());
    assertAccountOverview(probe, BigDecimal.TEN, BigDecimal.ZERO, 0);
  }

  @Test
  public void thatTransferCanBeCompleted() {
    TestKit probe = new TestKit(system());

    AccountId debtor = AccountId.of("Account-123456");
    AccountId creditor = AccountId.of("Account-23133");
    ActorRef account = system().actorOf(Account.props(BigDecimal.TEN, debtor), debtor.value());

    account.tell(new TransferMoney(creditor, BigDecimal.ONE), probe.getRef());
    TransferInitiated transferInitiated = probe.expectMsgClass(TransferInitiated.class);

    account.tell(new Debit(transferInitiated.transferId()), probe.getRef());
    probe.expectMsgClass(TransferCompleted.class);

    account.tell(GetAccountOverview.instance(), probe.getRef());
    assertAccountOverview(probe, BigDecimal.valueOf(9L), BigDecimal.ZERO, 0);
  }

  private void assertAccountOverview(TestKit probe, BigDecimal balance, BigDecimal blocked, int pendingTransfers) {
    AccountOverview accountOverview = probe.expectMsgClass(AccountOverview.class);
    assertThat(accountOverview)
      .extracting("balance", "blockedMoney", "pendingTransfers")
      .containsExactly(balance, blocked, pendingTransfers);
  }
}
