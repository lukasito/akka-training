package com.wirecard.akkatraining.domain.transfer;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActor;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol.Forward;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.ExecuteTransfer;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferCompleted;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferFailed;

import java.math.BigDecimal;

public class Transfer extends AbstractPersistentActor {

  private ActorRef requester;
  private BigDecimal amount;
  private AccountId creditor;
  private AccountId debtor;
  private Status status;
  private ActorRef accountRepository;
  private boolean creditCompleted;
  private boolean debitCompleted;

  private final LoggingAdapter log = Logging.getLogger(context().system(), this);

  private Transfer(ActorRef accountRepository) {
    this.accountRepository = accountRepository;
  }

  public static Props props(ActorRef accountRepository) {
    return Props.create(Transfer.class, () -> new Transfer(accountRepository));
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(ExecuteTransfer.class, this::executeTransfer)
      .build();
  }

  private Receive transferInitiated() {
    return ReceiveBuilder.create()
      .match(AccountProtocol.MoneyAllocated.class, this::moneyAllocated)
      .match(AccountProtocol.MoneyAllocationFailed.class, this::moneyAllocationFailed)
      .build();
  }

  private Receive finishFlow() {
    return ReceiveBuilder.create()
      .match(AccountProtocol.CreditSuccessful.class, this::creditSuccessful)
      .match(AccountProtocol.DebitSuccessful.class, this::debitSuccessful)
      .build();
  }

  private void executeTransfer(ExecuteTransfer executeTransfer) {
    TransferProtocol.TransferInitiated event = new TransferProtocol.TransferInitiated(
      new TransferId(self().path().name()),
      executeTransfer.debtor(), executeTransfer.creditor(), executeTransfer.amount(), sender());

    persist(event, e -> {
      accept(e);
      accountRepository.tell(new Forward(
        debtor, new AccountProtocol.AllocateMoney(transferId(), creditor, amount)
      ), self());
      notifyRequester(e);
    });
  }

  private void moneyAllocated(AccountProtocol.MoneyAllocated moneyAllocated) {
    getContext().become(finishFlow());
    accountRepository.tell(
      new Forward(creditor, new AccountProtocol.Credit(transferId(), amount)),
      self()
    );

    accountRepository.tell(
      new Forward(debtor, new AccountProtocol.Debit(transferId())),
      self()
    );

    log.info("Money allocated status: {}", status);
  }

  private void moneyAllocationFailed(AccountProtocol.MoneyAllocationFailed moneyAllocationFailed) {
    status = Status.FAILED;
    notifyRequester(new TransferFailed(transferId(), debtor, creditor, amount, moneyAllocationFailed.reason()));
    log.info("Money allocation failed, status: {}", status);
  }

  private void creditSuccessful(AccountProtocol.CreditSuccessful creditSuccessful) {
    if (debitCompleted) {
      status = Status.SUCCESS;
      notifyRequester(new TransferCompleted(transferId(), debtor, creditor, amount));
    } else {
      this.creditCompleted = true;
    }
    log.info("Credit successful, status: {}", status);
  }

  private void debitSuccessful(AccountProtocol.DebitSuccessful debitSuccessful) {
    if (creditCompleted) {
      status = Status.SUCCESS;
      notifyRequester(new TransferCompleted(transferId(), debtor, creditor, amount));
    } else {
      this.debitCompleted = true;
    }
    log.info("Debit successful, status: {}", status);
  }

  private TransferId transferId() {
    return new TransferId(self().path().name());
  }

  private void notify(Object event) {
    context().system().eventStream().publish(event);
    sender().tell(event, self());
  }

  private void notifyRequester(Object event) {
    context().system().eventStream().publish(event);
    requester.tell(event, self());
  }

  // PERSISTENCE

  @Override
  public String persistenceId() {
    return self().path().name();
  }

  @Override
  public Receive createReceiveRecover() {
    return ReceiveBuilder.create()
      .match(TransferProtocol.TransferInitiated.class, this::accept)
      .build();
  }

  private void accept(TransferProtocol.TransferInitiated transferInitiated) {
    this.amount = transferInitiated.amount();
    this.creditor = transferInitiated.creditor();
    this.debtor = transferInitiated.debtor();
    this.status = Status.IN_PROGRESS;
    requester = transferInitiated.requester();
    getContext().become(transferInitiated());
  }
}
