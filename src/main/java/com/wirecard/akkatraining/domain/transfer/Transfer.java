package com.wirecard.akkatraining.domain.transfer;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol.Forward;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.ExecuteTransfer;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferFailed;

import java.math.BigDecimal;

public class Transfer extends AbstractLoggingActor {

  private BigDecimal amount;
  private AccountId creditor;
  private AccountId debtor;
  private Status status;
  private ActorRef accountRepository;
  private boolean creditCompleted;
  private boolean debitCompleted;

  private Transfer(ActorRef accountRepository) {
    this.accountRepository = accountRepository;
    subscribe(
      AccountProtocol.CreditSuccessful.class,
      AccountProtocol.DebitSuccessful.class,
      AccountProtocol.MoneyAllocated.class,
      AccountProtocol.MoneyAllocationFailed.class
    );
  }

  public static Props props(ActorRef accountRepository) {
    return Props.create(Transfer.class, () -> new Transfer(accountRepository));
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(ExecuteTransfer.class, this::accept)
      .build();
  }

  private Receive transferInitiated() {
    return ReceiveBuilder.create()
      .match(AccountProtocol.MoneyAllocated.class, this::accept)
      .match(AccountProtocol.MoneyAllocationFailed.class, this::accept)
      .build();
  }

  private Receive finishFlow() {
    return ReceiveBuilder.create()
      .match(AccountProtocol.CreditSuccessful.class, this::accept)
      .match(AccountProtocol.DebitSuccessful.class, this::accept)
      .build();
  }

  private void accept(ExecuteTransfer executeTransfer) {
    this.amount = executeTransfer.amount();
    this.creditor = executeTransfer.creditor();
    this.debtor = executeTransfer.debtor();
    this.status = Status.IN_PROGRESS;

    getContext().become(transferInitiated());
    accountRepository.tell(new Forward(
      debtor, new AccountProtocol.AllocateMoney(transferId(), creditor, amount)
    ), self());

    publish(
      new TransferProtocol.TransferInitiated(
        new TransferId(self().path().name()),
        debtor, creditor, amount));
  }

  private void accept(AccountProtocol.MoneyAllocated moneyAllocated) {
    getContext().become(finishFlow());
    accountRepository.tell(
      new Forward(creditor, new AccountProtocol.Credit(transferId(), amount)),
      self()
    );

    accountRepository.tell(
      new Forward(debtor, new AccountProtocol.Debit(transferId())),
      self()
    );
  }

  private void accept(AccountProtocol.MoneyAllocationFailed moneyAllocationFailed) {
    status = Status.FAILED;
    publish(new TransferFailed(transferId(), debtor, creditor, amount, moneyAllocationFailed.reason()));
  }

  private void accept(AccountProtocol.CreditSuccessful creditSuccessful) {
    if (debitCompleted) {
      status = Status.SUCCESS;
      publish(new TransferProtocol.TransferCompleted(transferId()));
    } else {
      this.creditCompleted = true;
    }
  }

  private void accept(AccountProtocol.DebitSuccessful debitSuccessful) {
    if (creditCompleted) {
      status = Status.SUCCESS;
      publish(new TransferProtocol.TransferCompleted(transferId()));
    } else {
      this.debitCompleted = true;
    }
  }

  private TransferId transferId() {
    return new TransferId(self().path().name());
  }

  private void publish(Object event) {
    context().system().eventStream().publish(event);
  }

  private void subscribe(Class<?>... eventClasses) {
    for (Class<?> klass : eventClasses) {
      context().system().eventStream().subscribe(self(), klass);
    }
  }
}
