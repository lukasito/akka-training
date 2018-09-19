package com.wirecard.akkatraining.infrastructure.repository;

import com.wirecard.akkatraining.domain.transfer.TransferId;
import com.wirecard.akkatraining.domain.view.Transfer;

import java.util.ArrayList;
import java.util.List;

public class InMemoryTransferDao {

  private final List<Transfer> transfers = new ArrayList<>();

  public void add(Transfer transfer) {
    transfers.add(transfer);
  }

  public Transfer find(TransferId transferId) {
    return transfers.stream()
      .filter(t -> t.transferId().equals(transferId))
      .findFirst()
      .orElse(null);
  }
}
