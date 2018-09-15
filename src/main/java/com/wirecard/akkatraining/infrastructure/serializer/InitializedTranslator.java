package com.wirecard.akkatraining.infrastructure.serializer;

import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.proto.Account;
import io.vavr.control.Try;

import java.math.BigDecimal;

class InitializedTranslator {

  static byte[] toBinary(AccountProtocol.Initialized initialized) {
    return Account.Initialized.newBuilder()
      .setBalance(initialized.balance().doubleValue())
      .setAllocatedBalance(initialized.allocatedBalance().doubleValue())
      .build().toByteArray();
  }

  static Try<AccountProtocol.Initialized> fromBinary(byte[] array) {
    return Try.of(() -> {
      Account.Initialized parsed = Account.Initialized.parseFrom(array);
      return new AccountProtocol.Initialized(
        BigDecimal.valueOf(parsed.getBalance()),
        BigDecimal.valueOf(parsed.getAllocatedBalance())
      );
    });
  }
}
