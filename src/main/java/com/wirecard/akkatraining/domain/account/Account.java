package com.wirecard.akkatraining.domain.account;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AllocateMoney;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Credit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.CreditSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Debit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitFailed;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.GetAccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Initialize;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Initialized;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocated;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocationFailed;
import com.wirecard.akkatraining.domain.transfer.TransferId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account extends AbstractPersistentActor {

  private AccountId accountId;
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
      .match(Initialize.class, this::initialize)
      .matchAny(o -> log.error("Unknown message {}", o))
      .build();
  }

  private Receive ready() {
    return ReceiveBuilder.create()
      .match(AllocateMoney.class, this::allocateMoney)
      .match(Credit.class, this::credit)
      .match(Debit.class, this::debit)
      .matchEquals(GetAccountOverview.instance(), this::overview)
      .matchAny(o -> log.error("Unknown message {}", o))
      .build();
  }

  private void initialize(Initialize initialize) {
    Initialized event = new Initialized(
      initialize.accountId(),
      initialize.balance(),
      initialize.allocatedBalance()
    );
    persist(event, this::accept);
  }

  private void allocateMoney(AllocateMoney allocateMoney) {
    BigDecimal amount = allocateMoney.amount();
    TransferId transferId = allocateMoney.transferId();
    if (availableBalance().compareTo(amount) >= 0) {
      MoneyAllocated event = new MoneyAllocated(transferId, accountId, allocateMoney.creditor(), amount);
      persist(event, this::accept);
      notify(event);
      log.info("Current state {}", createOverview());
    } else {
      // rejected
      notify(new MoneyAllocationFailed(transferId, accountId, "Not enough balance!"));
    }
  }

  private void credit(Credit credit) {
    CreditSuccessful event = new CreditSuccessful(credit.transferId(), credit.amount(), accountId);
    persist(event, this::accept);
    notify(event);
  }

  private void debit(Debit debit) {
    Object result = transfers.stream().filter(transfer -> transfer.id().equals(debit.transferId()))
      .findFirst()
      .map(transfer -> {
        DebitSuccessful debitSuccessful = new DebitSuccessful(transfer);
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

  @Override
  public String persistenceId() {
    return accountId.value();
  }

  @Override
  public Receive createReceiveRecover() {
    return ReceiveBuilder.create()
      .match(Initialized.class, this::accept)
      .match(MoneyAllocated.class, this::accept)
      .match(CreditSuccessful.class, this::accept)
      .match(DebitSuccessful.class, this::accept)
      .build();
  }

  private void accept(Initialized initialized) {
    accountId = initialized.accountId();
    balance = initialized.balance();
    allocatedBalance = initialized.allocatedBalance();
    getContext().become(ready());
  }

  private void accept(MoneyAllocated moneyAllocated) {
    BigDecimal amount = moneyAllocated.amount();
    allocateMoney(amount);
    transfers.add(new PendingTransfer(moneyAllocated.transferId(), amount, moneyAllocated.creditor()));
  }

  private void accept(CreditSuccessful creditSuccessful) {
    balance = balance.add(creditSuccessful.amount());
  }

  private void accept(DebitSuccessful debitSuccessful) {
    PendingTransfer pendingTransfer = debitSuccessful.pendingTransfer();
    transfers.remove(pendingTransfer);
    chargeMoney(pendingTransfer.amount());
  }
}
