package com.wirecard.akkatraining.domain;

import lombok.Value;

@Value
public class Delivery {

  long deliveryId;
  Object message;
}
