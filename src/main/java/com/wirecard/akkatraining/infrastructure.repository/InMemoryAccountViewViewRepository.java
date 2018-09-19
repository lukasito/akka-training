package com.wirecard.akkatraining.infrastructure.repository;

import com.wirecard.akkatraining.domain.account.AccountId;
import com.wirecard.akkatraining.domain.view.Account;
import com.wirecard.akkatraining.domain.view.AccountViewRepository;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAccountViewViewRepository implements AccountViewRepository {

  private final List<Account> accounts = new ArrayList<>();

  @Override
  public Account find(AccountId accountId) {
    return accounts.stream()
      .filter(view -> view.accountId().equals(accountId))
      .findFirst()
      .orElse(null);
  }

  public void add(Account account) {
    accounts.add(account);
  }
}
