package com.wirecard.akkatraining.infrastructure.adapter;

import akka.persistence.journal.Tagged;
import akka.persistence.journal.WriteEventAdapter;
import com.google.common.collect.Sets;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.transfer.TransferProtocol;

public class ProtocolEventTaggingAdapter implements WriteEventAdapter {

  @Override
  public String manifest(Object event) {
    return "";
  }

  @Override
  public Object toJournal(Object event) {
    String eventClassName = event.getClass().getName();
    if (event instanceof AccountProtocol.Event) {
      return new Tagged(event, Sets.newHashSet(
        AccountProtocol.Event.class.getName(),
        eventClassName
      ));
    } else if (event instanceof TransferProtocol.Event) {
      return new Tagged(event, Sets.newHashSet(
        TransferProtocol.Event.class.getName(),
        eventClassName
      ));
    } else {
      return new Tagged(event, Sets.newHashSet(eventClassName));
    }
  }
}
