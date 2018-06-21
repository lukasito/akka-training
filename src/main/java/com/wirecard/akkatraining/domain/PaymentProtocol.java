package com.wirecard.akkatraining.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentProtocol {

  @Value
  class Purchase implements PaymentProtocol {
    Price price;
  }

  @Value
  class Refund implements PaymentProtocol {
    Price price;
  }

  @Value
  @Builder
  class TransactionCreated implements PaymentProtocol {
    PaymentId paymentId;
    String type;
    BigDecimal value;
    String currency;
    LocalDateTime completionTime;
  }

  @Value
  class DescribePayment implements PaymentProtocol {
    private static final DescribePayment instance = new DescribePayment();

    public static DescribePayment instance() {
      return instance;
    }
  }

  @Value
  class PaymentDescriptor implements PaymentProtocol {
    int transactionCount;
    BigDecimal summary;
  }
}
