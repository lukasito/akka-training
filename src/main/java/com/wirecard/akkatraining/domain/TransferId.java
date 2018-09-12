package com.wirecard.akkatraining.domain;

import lombok.Value;

@Value(staticConstructor = "of")
public class TransferId {

  String value;
}
