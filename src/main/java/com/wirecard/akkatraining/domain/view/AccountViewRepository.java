package com.wirecard.akkatraining.domain.view;

import com.wirecard.akkatraining.domain.account.AccountId;

public interface AccountViewRepository {

  Account find(AccountId accountId);
}
