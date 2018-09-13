package com.wirecard.akkatraining.domain;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.AccountProtocol.AccountOverview;
import com.wirecard.akkatraining.domain.AccountProtocol.CancelTransfer;
import com.wirecard.akkatraining.domain.AccountProtocol.CompleteTransfer;
import com.wirecard.akkatraining.domain.AccountProtocol.GetAccountOverview;
import com.wirecard.akkatraining.domain.AccountProtocol.InitiateTransfer;
import com.wirecard.akkatraining.domain.AccountProtocol.TransferCancellationFailed;
import com.wirecard.akkatraining.domain.AccountProtocol.TransferCancelled;
import com.wirecard.akkatraining.domain.AccountProtocol.TransferCompleted;
import com.wirecard.akkatraining.domain.AccountProtocol.TransferInitiated;
import com.wirecard.akkatraining.domain.AccountProtocol.TransferInitiationFailed;
import lombok.val;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account extends AbstractLoggingActor {

  private AccountNumber accountNumber;
  private BigDecimal balance;
  private BigDecimal blockedMoney;
  private final List<Transfer> pendingTransfers = new ArrayList<>();

  private Account(BigDecimal balance, AccountNumber accountNumber) {
    this.balance = balance;
    blockedMoney = BigDecimal.ZERO;
    this.accountNumber = accountNumber;
  }

  public static Props props(BigDecimal balance, AccountNumber accountNumber) {
    return Props.create(Account.class, () -> new Account(balance, accountNumber));
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(InitiateTransfer.class, this::accept)
      .match(CancelTransfer.class, this::accept)
      .match(CompleteTransfer.class, this::accept)
      .matchEquals(GetAccountOverview.instance(), this::overview)
      .matchAny(o -> log().error("Unknown message {}", o))
      .build();
  }

  private void overview(GetAccountOverview o) {
    AccountOverview overview = new AccountOverview(balance, blockedMoney, pendingTransfers.size());
    sender().tell(overview, self());
  }

  private void accept(InitiateTransfer initiateInternalTransfer) {
    // business rule encapsulation and domain validations
    val amount = initiateInternalTransfer.amount();
    val creditor = initiateInternalTransfer.creditor();

    if (creditor.equals(accountNumber)) {
      val rejection = new TransferInitiationFailed(amount, "Cannot make transfer to itself!");
      sender().tell(rejection, self());
      context().stop(self());
      return;
    }

    if (availableBalance().compareTo(amount) >= 0) {
      blockedMoney(amount);
      Transfer transfer = createPendingTransfer(amount, creditor);
      val event = new TransferInitiated(transfer.id(), this.accountNumber, creditor, amount);
      sender().tell(event, self());
    } else {
      val rejection = new TransferInitiationFailed(amount, "Balance too low");
      sender().tell(rejection, self());
      log().error("Transfer initialization failed {}", rejection);
    }
  }

  private void accept(CompleteTransfer completeTransfer) {
    pendingTransfers.stream().filter(transfer -> transfer.id().equals(completeTransfer.transferId()))
      .findFirst()
      .ifPresent(transfer -> {
        pendingTransfers.remove(transfer);
        releaseBlockedMoney(transfer.amount());
        chargeBalance(transfer.amount());
        sender().tell(new TransferCompleted(transfer.id()), self());
      });
  }

  private void accept(CancelTransfer cancelTransfer) {
    Object result = pendingTransfers.stream()
      .filter(transfer -> transfer.id().equals(cancelTransfer.transferId()))
      .findFirst()
      .map(transfer -> {
        pendingTransfers.remove(transfer);
        releaseBlockedMoney(transfer.amount());
        return (Object) new TransferCancelled(transfer.id());
      })
      .orElseGet(() -> new TransferCancellationFailed(cancelTransfer.transferId()));

    log().info("Cancellation result: {}", result);
    sender().tell(result, self());
  }

  private void blockedMoney(BigDecimal amount) {
    blockedMoney = blockedMoney.add(amount);
  }

  private BigDecimal availableBalance() {
    return balance.subtract(blockedMoney);
  }

  private void chargeBalance(BigDecimal amount) {
    balance = balance.subtract(amount);
  }

  private void releaseBlockedMoney(BigDecimal amount) {
    blockedMoney = blockedMoney.subtract(amount);
  }

  private Transfer createPendingTransfer(BigDecimal amount, AccountNumber creditor) {
    Transfer transfer = Transfer.newTransfer(amount, creditor);
    pendingTransfers.add(transfer);
    return transfer;
  }
}
