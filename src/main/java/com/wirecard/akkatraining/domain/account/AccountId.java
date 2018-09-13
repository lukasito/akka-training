package com.wirecard.akkatraining.domain.account;

import lombok.Value;

@Value(staticConstructor = "of")
public class AccountId {
  String value;
}
