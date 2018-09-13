package com.wirecard.akkatraining.infrastructure;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.account.Account;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class InMemoryAccountRepository extends AbstractLoggingActor {

  private final Map<AccountId, ActorRef> map = new HashMap<>();

  public static Props props() {
    return Props.create(InMemoryAccountRepository.class, InMemoryAccountRepository::new);
  }

  private InMemoryAccountRepository() {
    AccountId accountId1 = AccountId.of("Account-1");
    ActorRef account1Ref = context().actorOf(Account.props(BigDecimal.TEN, accountId1), accountId1.value());

    AccountId accountId2 = AccountId.of("Account-2");
    ActorRef account2Ref = context().actorOf(Account.props(BigDecimal.ONE, accountId2), accountId2.value());
    map.put(accountId1, account1Ref);
    map.put(accountId2, account2Ref);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(AccountRepositoryProtocol.Forward.class, this::forward)
      .build();
  }

  private void forward(AccountRepositoryProtocol.Forward forward) {
    AccountId accountId = forward.accountId();
    if (map.containsKey(accountId)) {
      map.get(accountId).forward(forward.command(), context());
    }
  }
}
