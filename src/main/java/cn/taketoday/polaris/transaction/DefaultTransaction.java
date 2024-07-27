/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.polaris.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import cn.taketoday.polaris.jdbc.datasource.ConnectionSource;
import cn.taketoday.polaris.logging.Logger;
import cn.taketoday.polaris.logging.LoggerFactory;
import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/21 18:07
 */
public class DefaultTransaction implements Transaction {

  private static final Logger log = LoggerFactory.getLogger(DefaultTransaction.class);

  private final TransactionConfig config;

  private final ConnectionSource connectionSource;

  @Nullable
  private Connection connection;

  @Nullable
  private Integer previousIsolationLevel;

  private boolean resetReadOnly;

  private boolean mustRestoreAutoCommit;

  public DefaultTransaction(TransactionConfig config, ConnectionSource connectionSource) {
    this.config = config;
    this.connectionSource = connectionSource;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = this.connection;
    if (connection == null) {
      if (log.isDebugEnabled()) {
        log.debug("Opening JDBC Connection");
      }
      connection = connectionSource.getConnection();
      this.resetReadOnly = !connection.isReadOnly();
      this.previousIsolationLevel = prepareConnectionForTransaction(connection, config);

      // Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
      // so we don't want to do it unnecessarily (for example if we've explicitly
      // configured the connection pool to set it already).
      if (connection.getAutoCommit()) {
        this.mustRestoreAutoCommit = true;
        if (log.isDebugEnabled()) {
          log.debug("Switching JDBC Connection [{}] to manual commit", connection);
        }
        connection.setAutoCommit(false);
      }

      this.connection = connection;
    }
    return connection;
  }

  @Override
  public void commit() throws SQLException {
    if (connection != null) {
      if (log.isDebugEnabled()) {
        log.debug("Committing JDBC Connection [{}]", connection);
      }
      connection.commit();
    }
  }

  @Override
  public void rollback() throws SQLException {
    if (connection != null) {
      if (log.isDebugEnabled()) {
        log.debug("Rolling back JDBC Connection [{}]", connection);
      }
      connection.rollback();
    }
  }

  @Override
  public void close() throws SQLException {
    if (connection != null) {
      resetConnectionAfterTransaction(connection);
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [{}]", connection);
      }
      connectionSource.releaseConnection(connection);
    }
  }

  static Integer prepareConnectionForTransaction(Connection con, @Nullable TransactionConfig config) throws SQLException {
    if (config != null) {
      // Set read-only flag.
      if (config.isReadOnly()) {
        if (log.isDebugEnabled()) {
          log.debug("Setting JDBC Connection [{}] read-only", con);
        }
        try {
          con.setReadOnly(true);
        }
        catch (SQLException | RuntimeException ex) {
          Throwable exToCheck = ex;
          while (exToCheck != null) {
            if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
              // Assume it's a connection timeout that would otherwise get lost: e.g. from JDBC 4.0
              throw ex;
            }
            exToCheck = exToCheck.getCause();
          }
          // "read-only not supported" SQLException -> ignore, it's just a hint anyway
          log.debug("Could not set JDBC Connection read-only", ex);
        }
      }

      // Apply specific isolation level, if any.
      Integer previousIsolationLevel = null;
      int isolationLevel = config.getIsolationLevel();
      if (isolationLevel != TransactionConfig.ISOLATION_DEFAULT) {
        if (log.isDebugEnabled()) {
          log.debug("Changing isolation level of JDBC Connection [{}] to {}", con, isolationLevel);
        }
        int currentIsolation = con.getTransactionIsolation();
        if (currentIsolation != isolationLevel) {
          previousIsolationLevel = currentIsolation;
          con.setTransactionIsolation(isolationLevel);
        }
      }
      return previousIsolationLevel;
    }
    return null;
  }

  void resetConnectionAfterTransaction(Connection con) {
    try {
      if (mustRestoreAutoCommit) {
        con.setAutoCommit(true);
      }

      // Reset transaction isolation to previous value, if changed for the transaction.
      if (previousIsolationLevel != null) {
        if (log.isDebugEnabled()) {
          log.debug("Resetting isolation level of JDBC Connection [{}] to {}", con, previousIsolationLevel);
        }
        con.setTransactionIsolation(previousIsolationLevel);
      }

      // Reset read-only flag if we originally switched it to true on transaction begin.
      if (resetReadOnly) {
        if (log.isDebugEnabled()) {
          log.debug("Resetting read-only flag of JDBC Connection [{}]", con);
        }
        con.setReadOnly(false);
      }
    }
    catch (Throwable ex) {
      log.debug("Could not reset JDBC Connection after transaction", ex);
    }
  }
}
