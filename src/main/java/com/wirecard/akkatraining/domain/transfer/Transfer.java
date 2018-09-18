package com.wirecard.akkatraining.domain.transfer;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.persistence.AbstractPersistentActorWithAtLeastOnceDelivery;
import com.wirecard.akkatraining.domain.Delivery;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.ExecuteTransfer;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.MessageConfirmed;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.MessageSent;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferCompleted;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferFailed;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol.TransferInitiated;

import java.math.BigDecimal;

public class Transfer extends AbstractPersistentActorWithAtLeastOnceDelivery {

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
    TransferInitiated event = new TransferInitiated(
      new TransferId(self().path().name()),
      executeTransfer.debtor(), executeTransfer.creditor(), executeTransfer.amount(), sender());

    persist(event, e -> {
      onTransferInitiated(e);
      notifyRequester(event);
    });

    persist(
      new MessageSent(
        new AccountProtocol.AllocateMoney(transferId(), creditor, amount)),
      this::onMessageSent);
  }

  private void moneyAllocated(AccountProtocol.MoneyAllocated moneyAllocated) {
    persist(
      new MessageConfirmed(moneyAllocated.deliveryId(), ConfirmationType.MONEY_ALLOCATED),
      this::onMessageConfirmed);

    persist(
      new MessageSent(
        new AccountProtocol.Credit(transferId(), amount)),
      this::onMessageSent);

    persist(
      new MessageSent(
        new AccountProtocol.Debit(transferId())),
      this::onMessageSent);

    log.info("Money allocated status: {}", status);
  }

  private void moneyAllocationFailed(AccountProtocol.MoneyAllocationFailed moneyAllocationFailed) {
    persist(
      new MessageConfirmed(moneyAllocationFailed.deliveryId(), ConfirmationType.MONEY_ALLOCATION_FAILED),
      this::onMessageConfirmed);

    persist(
      new TransferFailed(transferId(), debtor, creditor, amount, moneyAllocationFailed.reason()),
      event -> {
        onTransferFailed(event);
        notifyRequester(event);
      });

    log.info("Money allocation failed, status: {}", status);
  }

  private void creditSuccessful(AccountProtocol.CreditSuccessful creditSuccessful) {
    persist(
      new MessageConfirmed(creditSuccessful.deliveryId(), ConfirmationType.CREDIT),
      this::onMessageConfirmed
    );

    if (creditCompleted && debitCompleted) {
      persist(
        new TransferCompleted(transferId(), debtor, creditor, amount),
        e -> {
          onTransferCompleted(e);
          notifyRequester(e);
        });
    }
    log.info("Credit successful, status: {}", status);
  }

  private void debitSuccessful(AccountProtocol.DebitSuccessful debitSuccessful) {
    persist(
      new MessageConfirmed(debitSuccessful.deliveryId(), ConfirmationType.DEBIT),
      this::onMessageConfirmed
    );

    if (creditCompleted && debitCompleted) {
      persist(
        new TransferCompleted(transferId(), debtor, creditor, amount),
        e -> {
          onTransferCompleted(e);
          notifyRequester(e);
        });
    }
    log.info("Debit successful, status: {}", status);
  }

  private TransferId transferId() {
    return new TransferId(self().path().name());
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
      .match(TransferInitiated.class, this::onTransferInitiated)
      .match(MessageSent.class, this::onMessageSent)
      .match(MessageConfirmed.class, this::onMessageConfirmed)
      .match(TransferCompleted.class, this::onTransferCompleted)
      .match(TransferFailed.class, this::onTransferFailed)
      .build();
  }

  private void onTransferInitiated(TransferInitiated transferInitiated) {
    this.amount = transferInitiated.amount();
    this.creditor = transferInitiated.creditor();
    this.debtor = transferInitiated.debtor();
    this.status = Status.IN_PROGRESS;
    requester = transferInitiated.requester();
    getContext().become(transferInitiated());
  }

  private void onTransferFailed(TransferFailed transferFailed) {
    status = Status.FAILED;
    getContext().become(emptyBehavior());
  }

  private void onTransferCompleted(TransferCompleted transferCompleted) {
    status = Status.SUCCESS;
    getContext().become(emptyBehavior());
  }

  private void onMessageSent(MessageSent messageSent) {
    Object msg = messageSent.message();
    if (msg instanceof AccountProtocol.AllocateMoney) {
      AccountProtocol.AllocateMoney message = (AccountProtocol.AllocateMoney) msg;
      deliver(debtor, message);
    }
    if (msg instanceof AccountProtocol.Credit) {
      getContext().become(finishFlow());
      AccountProtocol.Credit message = (AccountProtocol.Credit) msg;
      deliver(creditor, message);
    }
    if (msg instanceof AccountProtocol.Debit) {
      getContext().become(finishFlow());
      AccountProtocol.Debit message = (AccountProtocol.Debit) msg;
      deliver(debtor, message);
    }
  }

  private void onMoneyAllocated() {
    getContext().become(finishFlow());
  }

  private void onMoneyAllocationFailed() {
    status = Status.FAILED;
  }

  private void onDebitSuccessful() {
    debitCompleted = true;
    if (creditCompleted) {
      status = Status.SUCCESS;
    }
  }

  private void onCreditSuccessful() {
    creditCompleted = true;
    if (debitCompleted) {
      status = Status.SUCCESS;
    }
  }

  // AT LEAST ONCE DELIVERY (confirm delivery)
  private void onMessageConfirmed(MessageConfirmed confirmed) {
    confirmDelivery(confirmed.deliveryId());
    ConfirmationType confirmationType = confirmed.confirmationType();
    switch (confirmationType) {
      case MONEY_ALLOCATED:
        onMoneyAllocated();
        break;
      case MONEY_ALLOCATION_FAILED:
        onMoneyAllocationFailed();
        break;
      case DEBIT:
        onDebitSuccessful();
        break;
      case CREDIT:
        onCreditSuccessful();
        break;
    }
  }

  // AT LEAST ONCE DELIVERY (deliver)
  private void deliver(AccountId to, Object message) {
    deliver(accountRepository.path(), deliveryId ->
      new AccountRepositoryProtocol.Forward(to, new Delivery(deliveryId, message)));
  }
}
