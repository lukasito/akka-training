package com.wirecard.akkatraining.domain.transfer;

import lombok.Value;

import java.io.Serializable;

@Value
public class TransferId implements Serializable {
  String value;
}
