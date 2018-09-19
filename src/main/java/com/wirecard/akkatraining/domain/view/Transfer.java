package com.wirecard.akkatraining.domain.view;

import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.transfer.TransferId;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Transfer {
  TransferId transferId;
  AccountId debtor;
  AccountId creditor;
  BigDecimal amount;
  String status;
}
