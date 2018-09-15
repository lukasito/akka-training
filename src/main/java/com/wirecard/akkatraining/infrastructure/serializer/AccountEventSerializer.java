package com.wirecard.akkatraining.infrastructure.serializer;

import akka.serialization.SerializerWithStringManifest;
import com.wirecard.akkatraining.domain.account.AccountProtocol.CreditSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.DebitSuccessful;
import com.wirecard.akkatraining.domain.account.AccountProtocol.Initialized;
import com.wirecard.akkatraining.domain.account.AccountProtocol.MoneyAllocated;

import java.io.NotSerializableException;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.instanceOf;

public class AccountEventSerializer extends SerializerWithStringManifest {

  private static final String MANIFEST_ACC_INITIALIZED_V1 = "account_initialized_v1";
  private static final String MANIFEST_ACC_MONEY_ALLOCATED_V1 = "account_money_allocated_v1";
  private static final String MANIFEST_ACC_DEBIT_SUCCESSFUL_V1 = "account_debit_successful_v1";
  private static final String MANIFEST_ACC_CREDIT_SUCCESSFUL_V1 = "account_credit_successful_v1";


  @Override
  public int identifier() {
    return 1000;
  }

  @Override
  public String manifest(Object o) {
    return Match(o).of(
      Case($(instanceOf(Initialized.class)), MANIFEST_ACC_INITIALIZED_V1),
      Case($(instanceOf(MoneyAllocated.class)), MANIFEST_ACC_MONEY_ALLOCATED_V1),
      Case($(instanceOf(DebitSuccessful.class)), MANIFEST_ACC_DEBIT_SUCCESSFUL_V1),
      Case($(instanceOf(CreditSuccessful.class)), MANIFEST_ACC_CREDIT_SUCCESSFUL_V1)
    );
  }

  @Override
  public byte[] toBinary(Object o) {
    return Match(o).of(
      Case($(instanceOf(Initialized.class)), InitializedTranslator::toBinary)
    );
  }

  @Override
  public Object fromBinary(byte[] bytes, String manifest) throws NotSerializableException {
    return Match(manifest).option(
      Case($(MANIFEST_ACC_INITIALIZED_V1), () -> InitializedTranslator.fromBinary(bytes))
    ).getOrElseThrow(NotSerializableException::new)
      .getOrElseThrow(e -> new NotSerializableException(e.getMessage()));
  }
}
