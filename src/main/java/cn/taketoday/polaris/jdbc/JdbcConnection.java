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

package cn.taketoday.polaris.jdbc;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.polaris.jdbc.datasource.ConnectionSource;
import cn.taketoday.polaris.transaction.Transaction;
import cn.taketoday.polaris.transaction.TransactionConfig;
import cn.taketoday.transaction.IllegalTransactionStateException;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.TransactionSystemException;

/**
 * Represents a connection to the database with a transaction.
 */
public final class JdbcConnection implements Closeable, QueryProducer {

  private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

  private final RepositoryManager manager;

  private final ConnectionSource connectionSource;

  @Nullable
  private Connection root;

  final boolean autoClose;

  private boolean rollbackOnClose = true;

  private boolean rollbackOnException = true;

  private final HashSet<Statement> statements = new HashSet<>();

  @Nullable
  private Transaction transaction;

  JdbcConnection(RepositoryManager manager, ConnectionSource source, boolean autoClose) {
    this.manager = manager;
    this.autoClose = autoClose;
    this.connectionSource = source;
  }

  /**
   * @throws DataAccessException Could not acquire a connection from data-source
   * @see DataSource#getConnection()
   */
  @Override
  public Query createQuery(String queryText) {
    boolean returnGeneratedKeys = manager.isGeneratedKeys();
    return createQuery(queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  @Override
  public Query createQuery(String queryText, boolean returnGeneratedKeys) {
    return new Query(this, queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public Query createQuery(String queryText, String... columnNames) {
    return new Query(this, queryText, columnNames);
  }

  /**
   * @throws DataAccessException Could not acquire a connection from data-source
   * @see DataSource#getConnection()
   */
  @Override
  public NamedQuery createNamedQuery(String queryText) {
    boolean returnGeneratedKeys = manager.isGeneratedKeys();
    return createNamedQuery(queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  @Override
  public NamedQuery createNamedQuery(String queryText, boolean returnGeneratedKeys) {
    return new NamedQuery(this, queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public NamedQuery createNamedQuery(String queryText, String... columnNames) {
    return new NamedQuery(this, queryText, columnNames);
  }

  /**
   * use :p1, :p2, :p3 as the parameter name
   */
  public NamedQuery createNamedQueryWithParams(String queryText, Object... paramValues) {
    // due to #146, creating a query will not create a statement anymore
    // the PreparedStatement will only be created once the query needs to be executed
    // => there is no need to handle the query closing here anymore since there is nothing to close
    return createNamedQuery(queryText)
            .withParams(paramValues);
  }

  /**
   * Return a currently active transaction or create a new one, according to
   * the specified propagation behavior.
   * <p>Note that parameters like isolation level or timeout will only be applied
   * to new transactions, and thus be ignored when participating in active ones.
   * <p>Furthermore, not all transaction definition settings will be supported
   * by every transaction manager: A proper transaction manager implementation
   * should throw an exception when unsupported settings are encountered.
   * <p>An exception to the above rule is the read-only flag, which should be
   * ignored if no explicit read-only mode is supported. Essentially, the
   * read-only flag is just a hint for potential optimization.
   *
   * @return transaction status object representing the new or current transaction
   * @throws TransactionException in case of lookup, creation, or system errors
   * @throws IllegalTransactionStateException if the given transaction definition
   * cannot be executed (for example, if a currently active transaction is in
   * conflict with the specified propagation behavior)
   * @see TransactionConfig#getPropagationBehavior
   * @see TransactionConfig#getIsolationLevel
   * @see TransactionConfig#getTimeout
   * @see TransactionConfig#isReadOnly
   */
  public Transaction beginTransaction() {
    return beginTransaction(TransactionConfig.forDefaults());
  }

  /**
   * Return a currently active transaction or create a new one, according to
   * the specified propagation behavior.
   * <p>Note that parameters like isolation level or timeout will only be applied
   * to new transactions, and thus be ignored when participating in active ones.
   * <p>Furthermore, not all transaction definition settings will be supported
   * by every transaction manager: A proper transaction manager implementation
   * should throw an exception when unsupported settings are encountered.
   * <p>An exception to the above rule is the read-only flag, which should be
   * ignored if no explicit read-only mode is supported. Essentially, the
   * read-only flag is just a hint for potential optimization.
   *
   * @param config the TransactionConfig instance (can be {@code null} for defaults),
   * describing propagation behavior, isolation level, timeout etc.
   * @return transaction status object representing the new or current transaction
   * @throws TransactionException in case of lookup, creation, or system errors
   * @throws IllegalTransactionStateException if the given transaction definition
   * cannot be executed (for example, if a currently active transaction is in
   * conflict with the specified propagation behavior)
   * @see TransactionConfig#getPropagationBehavior
   * @see TransactionConfig#getIsolationLevel
   * @see TransactionConfig#getTimeout
   * @see TransactionConfig#isReadOnly
   */
  public Transaction beginTransaction(@Nullable TransactionConfig config) {
    if (transaction != null) {
      throw new InvalidDataAccessApiUsageException("Transaction require commit or rollback");
    }
    setRollbackOnClose(false);
    return this.transaction = manager.getTransactionManager().getTransaction(config);
  }

  /**
   * 确定 JDBC 连接是否开启了事务。
   *
   * @return 连接是否开启了事务
   */
  public boolean isTransactional() {
    return transaction != null;
  }

  public Transaction getTransaction() {
    Assert.state(transaction != null, "JDBC Connection is not transactional");
    return transaction;
  }

  /**
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   */
  public RepositoryManager rollback() {
    rollback(true);
    return manager;
  }

  /**
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   */
  public JdbcConnection rollback(boolean closeConnection) {
    try {
      if (transaction != null) {
        transaction.rollback();
        if (closeConnection) {
          transaction.close();
        }
        this.transaction = null;
      }
      else if (closeConnection) {
        closeConnection();
      }
    }
    catch (SQLException ex) {
      throw translateException("JDBC rollback", ex);
    }
    return this;
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * if this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   */
  public void commit() {
    commit(true);
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @param closeConnection close connection
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * if this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   * @see TransactionStatus#setRollbackOnly
   */
  public void commit(boolean closeConnection) {
    try {
      if (transaction != null) {
        transaction.commit();
        if (closeConnection) {
          transaction.close();
        }
        this.transaction = null;
      }
      else if (closeConnection) {
        closeConnection();
      }
    }
    catch (SQLException ex) {
      throw translateException("JDBC commit", ex);
    }
  }

  void registerStatement(Statement statement) {
    statements.add(statement);
  }

  void removeStatement(Statement statement) {
    statements.remove(statement);
  }

  void onException() {
    if (rollbackOnException) {
      rollback(autoClose);
    }
  }
  // Closeable

  @Override
  public void close() {
    boolean connectionIsClosed;
    Connection connection = root;
    try {
      connectionIsClosed = connection != null && connection.isClosed();
    }
    catch (SQLException e) {
      throw translateException("trying to determine whether the connection is closed.", e);
    }

    if (!connectionIsClosed) {
      for (Statement statement : statements) {
        try {
          statement.close();
        }
        catch (SQLException ex) {
          if (manager.isCatchResourceCloseErrors()) {
            throw translateException("Trying to close statement", ex);
          }
          else {
            log.warn("Could not close statement. statement: {}", statement, ex);
          }
        }
      }
      statements.clear();

      boolean rollback = rollbackOnClose;
      if (rollback && connection != null) {
        try {
          rollback = !connection.getAutoCommit();
        }
        catch (SQLException e) {
          log.warn("Could not determine connection auto commit mode.", e);
        }
      }

      // if in transaction, rollback, otherwise just close
      if (rollback) {
        rollback(true);
      }
      else {
        closeConnection();
      }
    }
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  void createConnection() {
    try {
      if (transaction != null) {
        this.root = transaction.getConnection();
      }
      else {
        this.root = connectionSource.getConnection();
      }
    }
    catch (SQLException e) {
      throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", e);
    }
  }

  private void closeConnection() {
    Connection con = root;
    try {
      if (transaction != null) {
        transaction.close();
      }
      else if (con != null) {
        connectionSource.releaseConnection(con);
      }
    }
    catch (SQLException ex) {
      if (manager.isCatchResourceCloseErrors()) {
        throw translateException("Trying to close connection", ex);
      }
      else {
        log.warn("Could not close connection: {}", con, ex);
      }
    }
  }

  public void setRollbackOnException(boolean rollbackOnException) {
    this.rollbackOnException = rollbackOnException;
  }

  public void setRollbackOnClose(boolean rollbackOnClose) {
    this.rollbackOnClose = rollbackOnClose;
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public Connection getJdbcConnection() {
    if (root == null) {
      createConnection();
      return root;
    }

    try {
      if (root.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw translateException("Retrieves Connection status is closed", e);
    }
    return root;
  }

  public RepositoryManager getManager() {
    return manager;
  }

  private DataAccessException translateException(String task, SQLException ex) {
    return manager.translateException(task, null, ex);
  }

}
