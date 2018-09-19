package com.wirecard.akkatraining.infrastructure;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.wirecard.akkatraining.domain.account.Account;
import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.account.AccountProtocol;
import com.wirecard.akkatraining.domain.account.AccountRepositoryProtocol;

public class InMemoryAccountRepository extends AbstractLoggingActor {

  public static Props props() {
    return Props.create(InMemoryAccountRepository.class, InMemoryAccountRepository::new);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
      .match(AccountRepositoryProtocol.Forward.class, this::forward)
      .match(AccountRepositoryProtocol.Save.class, this::save)
      .build();
  }

  private void save(AccountRepositoryProtocol.Save save) {
    AccountId accountId = save.accountId();

    context()
      .actorOf(Account.props(), accountId.value())
      .tell(new AccountProtocol.Create(save.accountName(), save.balance(), save.allocatedBalance()), self());
  }

  private void forward(AccountRepositoryProtocol.Forward msg) {
    getContext().findChild(msg.accountId().value())
      .ifPresent(ref -> ref.forward(msg.command(), context()));
  }
}
