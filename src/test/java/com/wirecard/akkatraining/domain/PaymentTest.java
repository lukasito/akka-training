package com.wirecard.akkatraining.domain;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.wirecard.akkatraining.domain.PaymentProtocol.DescribePayment;
import com.wirecard.akkatraining.domain.PaymentProtocol.PaymentDescriptor;
import com.wirecard.akkatraining.domain.PaymentProtocol.Purchase;
import com.wirecard.akkatraining.domain.PaymentProtocol.Refund;
import com.wirecard.akkatraining.domain.PaymentProtocol.TransactionCreated;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentTest {

  private static ActorSystem system;

  @BeforeClass
  public static void setup() {
    system = ActorSystem.create("test-system");
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void test() {
    ActorRef payment = system.actorOf(Payment.props(), "Payment-" + UUID.randomUUID());
    TestKit sender = new TestKit(system);

    payment.tell(new Purchase(new Price(BigDecimal.ONE, "EUR")), sender.getRef());
    TransactionCreated transactionCreated = sender.expectMsgClass(TransactionCreated.class);
    System.out.println(transactionCreated);

    payment.tell(new Purchase(new Price(BigDecimal.ONE, "EUR")), sender.getRef());
    transactionCreated = sender.expectMsgClass(TransactionCreated.class);
    System.out.println(transactionCreated);

    payment.tell(new Refund(new Price(BigDecimal.TEN, "EUR")), sender.getRef());
    transactionCreated = sender.expectMsgClass(TransactionCreated.class);
    System.out.println(transactionCreated);

    payment.tell(DescribePayment.instance(), sender.getRef());
    PaymentDescriptor descriptor = sender.expectMsgClass(PaymentDescriptor.class);
    System.out.println(descriptor);
  }
}
