package com.wirecard.akkatraining.infrastructure.projections;

import akka.persistence.query.Offset;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public interface WithTimeOffsetFromDataSource {

  String SELECT_LAST_OFFSET = "select offset from offsets where offset_type = ?";

  DataSource dataSource();

  @SneakyThrows
  default Offset findOffset(String offsetType) {
    try (Connection c = dataSource().getConnection()) {
      PreparedStatement ps = c.prepareStatement(SELECT_LAST_OFFSET);
      ps.setString(1, offsetType);
      ResultSet resultSet = ps.executeQuery();
      resultSet.next();
      if (resultSet.next()) {
        return Offset.timeBasedUUID(UUID.fromString(resultSet.getString(1)));
      }
    }
    return Offset.noOffset();
  }
}
