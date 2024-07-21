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

package cn.taketoday.polaris.jdbc.support;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;

import cn.taketoday.dao.CannotAcquireLockException;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.DataIntegrityViolationException;
import cn.taketoday.dao.DuplicateKeyException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.PermissionDeniedDataAccessException;
import cn.taketoday.dao.PessimisticLockingFailureException;
import cn.taketoday.dao.QueryTimeoutException;
import cn.taketoday.dao.RecoverableDataAccessException;
import cn.taketoday.dao.TransientDataAccessResourceException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.jdbc.BadSqlGrammarException;

/**
 * {@link SQLExceptionTranslator} implementation which analyzes the specific
 * {@link SQLException} subclass thrown by the JDBC driver.
 *
 * <p>Falls back to a standard {@link SQLStateSQLExceptionTranslator} if the JDBC
 * driver does not actually expose JDBC 4 compliant {@code SQLException} subclasses.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SQLTransientException
 * @see SQLTransientException
 * @see SQLRecoverableException
 * @since 1.0
 */
public class SQLExceptionSubclassTranslator extends AbstractFallbackSQLExceptionTranslator {

  public SQLExceptionSubclassTranslator() {
    setFallbackTranslator(new SQLStateSQLExceptionTranslator());
  }

  @Override
  @Nullable
  protected DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
    if (ex instanceof SQLTransientException) {
      if (ex instanceof SQLTransientConnectionException) {
        return new TransientDataAccessResourceException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLTransactionRollbackException) {
        if ("40001".equals(ex.getSQLState())) {
          return new CannotAcquireLockException(buildMessage(task, sql, ex), ex);
        }
        return new PessimisticLockingFailureException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLTimeoutException) {
        return new QueryTimeoutException(buildMessage(task, sql, ex), ex);
      }
    }
    else if (ex instanceof SQLNonTransientException) {
      if (ex instanceof SQLNonTransientConnectionException) {
        return new DataAccessResourceFailureException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLDataException) {
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLIntegrityConstraintViolationException) {
        if (SQLStateSQLExceptionTranslator.indicatesDuplicateKey(ex.getSQLState(), ex.getErrorCode())) {
          return new DuplicateKeyException(buildMessage(task, sql, ex), ex);
        }
        return new DataIntegrityViolationException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLInvalidAuthorizationSpecException) {
        return new PermissionDeniedDataAccessException(buildMessage(task, sql, ex), ex);
      }
      if (ex instanceof SQLSyntaxErrorException) {
        return new BadSqlGrammarException(task, (sql != null ? sql : ""), ex);
      }
      if (ex instanceof SQLFeatureNotSupportedException) {
        return new InvalidDataAccessApiUsageException(buildMessage(task, sql, ex), ex);
      }
    }
    else if (ex instanceof SQLRecoverableException) {
      return new RecoverableDataAccessException(buildMessage(task, sql, ex), ex);
    }

    // Fallback to Infra own SQL state translation...
    return null;
  }

}
