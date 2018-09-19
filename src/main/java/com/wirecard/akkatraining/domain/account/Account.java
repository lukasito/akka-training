package com.wirecard.akkatraining.domain.account;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import com.wirecard.akkatraining.domain.Delivery;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AllocateMoney;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Create;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Created;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Credit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.CreditSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Debit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitFailed;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.GetAccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocated;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocationFailed;
import com.wirecard.akkatraining.domain.transfer.TransferId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account extends AbstractPersistentActor {

  private static final int snapShotInterval = 100;

  private BigDecimal balance;
  private BigDecimal allocatedBalance;
  private final List<PendingTransfer> transfers = new ArrayList<>();
  private final LoggingAdapter log = Logging.getLogger(context().system(), this);

  public static Props props() {
    return Props.create(Account.class, Account::new);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Create.class, this::create)
      .matchAny(o -> log.error("Unknown message {}", o))
      .build();
  }

  private Receive ready() {
    return ReceiveBuilder.create()
      .match(Delivery.class, this::delivery)
      .match(AllocateMoney.class, this::allocateMoney)
      .match(Credit.class, this::credit)
      .match(Debit.class, this::debit)
      .matchEquals(GetAccountOverview.instance(), this::overview)
      .matchAny(o -> log.error("Unknown message {}", o))
      .build();
  }

  private void delivery(Delivery delivery) {
    Object message = delivery.message();
    if (message instanceof AllocateMoney) {
      allocateMoney(delivery.deliveryId(), (AllocateMoney) message);
    } else if (message instanceof Credit) {
      credit(delivery.deliveryId(), (Credit) message);
    } else if (message instanceof Debit) {
      debit(delivery.deliveryId(), (Debit) message);
    }
  }

  private void create(Create create) {
    Created event = new Created(
      accountId(),
      create.accountName(),
      create.balance(),
      create.allocatedBalance()
    );
    persist(event, this::accept);
  }

  private void allocateMoney(long deliveryId, AllocateMoney allocateMoney) {
    BigDecimal amount = allocateMoney.amount();
    TransferId transferId = allocateMoney.transferId();
    if (availableBalance().compareTo(amount) >= 0) {
      MoneyAllocated event = new MoneyAllocated(deliveryId, transferId, accountId(), allocateMoney.creditor(), amount);
      persist(event, this::accept);
      notify(event);
      log.info("Current state {}", createOverview());
    } else {
      // rejected
      notify(new MoneyAllocationFailed(deliveryId, transferId, accountId(), "Not enough balance!"));
    }
  }

  private void allocateMoney(AllocateMoney allocateMoney) {
    allocateMoney(0, allocateMoney);
  }

  private void credit(Credit credit) {
    credit(0, credit);
  }

  private void credit(long deliveryId, Credit credit) {
    CreditSuccessful event = new CreditSuccessful(deliveryId, credit.transferId(), credit.amount(), accountId());
    persist(event, this::accept);
    notify(event);
  }

  private void debit(Debit debit) {
    debit(0, debit);
  }

  private void debit(long deliveryId, Debit debit) {
    Object result = transfers.stream().filter(transfer -> transfer.id().equals(debit.transferId()))
      .findFirst()
      .map(transfer -> {
        DebitSuccessful debitSuccessful = new DebitSuccessful(deliveryId, accountId(), transfer);
        persist(debitSuccessful, this::accept);
        return (Object) debitSuccessful;
      })
      .orElseGet(() -> new DebitFailed(debit.transferId(), "No allocated money for such transfer"));

    notify(result);
  }

  private void overview(GetAccountOverview o) {
    sender().tell(createOverview(), self());
  }

  private AccountOverview createOverview() {
    return new AccountOverview(balance, allocatedBalance, transfers.size());
  }


  private void allocateMoney(BigDecimal amount) {
    allocatedBalance = allocatedBalance.add(amount);
  }

  private BigDecimal availableBalance() {
    return balance.subtract(allocatedBalance);
  }

  private void chargeMoney(BigDecimal amount) {
    allocatedBalance = allocatedBalance.subtract(amount);
    balance = balance.subtract(amount);
  }

  private void notify(Object event) {
    context().system().eventStream().publish(event);
    sender().tell(event, self());
  }

  /*  PERSISTENCE & ES */

  private AccountId accountId() {
    return AccountId.of(persistenceId());
  }

  @Override
  public String persistenceId() {
    return self().path().name();
  }

  private void saveSnapshotIfNecessary() {
    if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() > 0) {
      saveSnapshot(new AccountState(balance, allocatedBalance, transfers));
    }
  }

  @Override
  public Receive createReceiveRecover() {
    return ReceiveBuilder.create()
      .match(Created.class, this::accept)
      .match(MoneyAllocated.class, this::accept)
      .match(CreditSuccessful.class, this::accept)
      .match(DebitSuccessful.class, this::accept)
      .match(SnapshotOffer.class, this::accept)
      .build();
  }

  private void accept(SnapshotOffer snapshotOffer) {
    AccountState snapshot = (AccountState) snapshotOffer.snapshot();
    balance = snapshot.balance();
    allocatedBalance = snapshot.allocatedBalance();
    transfers.clear();
    transfers.addAll(snapshot.transfers());
  }

  private void accept(Created created) {
    balance = created.balance();
    allocatedBalance = created.allocatedBalance();
    getContext().become(ready());
  }

  private void accept(MoneyAllocated moneyAllocated) {
    BigDecimal amount = moneyAllocated.amount();
    allocateMoney(amount);
    transfers.add(new PendingTransfer(moneyAllocated.transferId(), amount, moneyAllocated.creditor()));
    saveSnapshotIfNecessary();
  }

  private void accept(CreditSuccessful creditSuccessful) {
    balance = balance.add(creditSuccessful.amount());
    saveSnapshotIfNecessary();
  }

  private void accept(DebitSuccessful debitSuccessful) {
    PendingTransfer pendingTransfer = debitSuccessful.pendingTransfer();
    transfers.remove(pendingTransfer);
    chargeMoney(pendingTransfer.amount());
    saveSnapshotIfNecessary();
  }
}
