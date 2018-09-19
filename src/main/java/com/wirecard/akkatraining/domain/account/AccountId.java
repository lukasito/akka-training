package com.wirecard.akkatraining.domain.account;

import lombok.Value;

import java.io.Serializable;

@Value(staticConstructor = "of")
public class AccountId implements Serializable {
  String value;
}
