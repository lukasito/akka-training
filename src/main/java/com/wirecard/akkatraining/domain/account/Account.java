package com.wirecard.akkatraining.domain.account;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.AllocateMoney;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Credit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.CreditSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Debit;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitFailed;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.GetAccountOverview;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Initialize;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocated;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocationFailed;
import com.wirecard.akkatraining.domain.transfer.TransferId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account extends AbstractLoggingActor {

  private AccountId accountId;
  private BigDecimal balance;
  private BigDecimal allocatedBalance;
  private final List<PendingTransfer> transfers = new ArrayList<>();

  public static Props props() {
    return Props.create(Account.class, Account::new);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Initialize.class, this::accept)
      .matchAny(o -> log().error("Unknown message {}", o))
      .build();
  }

  private Receive ready() {
    return ReceiveBuilder.create()
      .match(AllocateMoney.class, this::accept)
      .match(Credit.class, this::accept)
      .match(Debit.class, this::accept)
      .matchEquals(GetAccountOverview.instance(), this::overview)
      .matchAny(o -> log().error("Unknown message {}", o))
      .build();
  }

  private void accept(Initialize initialize) {
    accountId = initialize.accountId();
    balance = initialize.balance();
    allocatedBalance = initialize.allocatedBalance();
    getContext().become(ready());
    notify(new AccountProtocol.Initialized(accountId, balance, allocatedBalance));
  }

  private void accept(AllocateMoney allocateMoney) {
    BigDecimal amount = allocateMoney.amount();
    TransferId transferId = allocateMoney.transferId();
    if (availableBalance().compareTo(amount) >= 0) {
      allocateMoney(amount);
      transfers.add(new PendingTransfer(transferId, amount, allocateMoney.creditor()));
      notify(new MoneyAllocated(transferId, accountId, allocateMoney.creditor(), amount));
      log().info("Current state {}", createOverview());
    } else {
      notify(new MoneyAllocationFailed(transferId, accountId, "Not enough balance!"));
    }
  }

  private void overview(GetAccountOverview o) {
    sender().tell(createOverview(), self());
  }

  private AccountOverview createOverview() {
    return new AccountOverview(balance, allocatedBalance, transfers.size());
  }

  private void accept(Credit credit) {
    balance = balance.add(credit.amount());
    notify(new CreditSuccessful(credit.transferId(), credit.amount(), accountId));
  }

  private void accept(Debit debit) {
    Object result = transfers.stream().filter(transfer -> transfer.id().equals(debit.transferId()))
      .findFirst()
      .map(transfer -> {
        transfers.remove(transfer);
        chargeMoney(transfer.amount());
        return (Object) new DebitSuccessful(transfer);
      })
      .orElseGet(() -> new DebitFailed(debit.transferId(), "No allocated money for such transfer"));

    notify(result);
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
}
