package com.wirecard.akkatraining.domain;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.PaymentProtocol.DescribePayment;
import com.wirecard.akkatraining.domain.PaymentProtocol.Purchase;
import com.wirecard.akkatraining.domain.PaymentProtocol.Refund;
import com.wirecard.akkatraining.domain.PaymentProtocol.TransactionCreated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Payment extends AbstractActor {

  private PaymentId id;
  private BigDecimal summary = BigDecimal.ZERO;
  private final List<Transaction> transactions = new ArrayList<>();

  public static Props props() {
    return Props.create(Payment.class, Payment::new);
  }

  private Payment() {
    this.id = new PaymentId(self().path().name());
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(Purchase.class, this::accept)
      .match(Refund.class, this::accept)
      .match(DescribePayment.class, this::accept)
      .matchAny(msg -> System.out.println("unknown message" + msg))
      .build();
  }

  private void accept(Purchase purchase) {
    Transaction transaction = new Transaction(id, "purchase", purchase.price(), LocalDateTime.now());
    transactions.add(transaction);
    summary = summary.add(transaction.value());

    sender().tell(transactionCreated(transaction), self());
  }

  private void accept(Refund refund) {
    Transaction transaction = new Transaction(id, "refund", refund.price(), LocalDateTime.now());
    transactions.add(transaction);
    summary = summary.subtract(transaction.value());

    sender().tell(transactionCreated(transaction), self());
  }

  private void accept(DescribePayment describe) {
    sender().tell(new PaymentProtocol.PaymentDescriptor(transactions.size(), summary), self());
  }

  private TransactionCreated transactionCreated(Transaction transaction) {
    return TransactionCreated.builder()
      .paymentId(transaction.paymentId())
      .value(transaction.value())
      .type(transaction.type())
      .currency(transaction.price().currency())
      .completionTime(transaction.completionTime())
      .build();
  }
}
