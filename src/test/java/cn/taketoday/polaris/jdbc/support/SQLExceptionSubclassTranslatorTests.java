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

import org.junit.jupiter.api.Test;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;

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
import cn.taketoday.polaris.jdbc.BadSqlGrammarException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Risberg
 */
public class SQLExceptionSubclassTranslatorTests {

  @Test
  public void exceptionClassTranslation() {
    doTest(new SQLDataException("", "", 0), DataIntegrityViolationException.class);
    doTest(new SQLFeatureNotSupportedException("", "", 0), InvalidDataAccessApiUsageException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "", 0), DataIntegrityViolationException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23505", 0), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 1), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 1062), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 2601), DuplicateKeyException.class);
    doTest(new SQLIntegrityConstraintViolationException("", "23000", 2627), DuplicateKeyException.class);
    doTest(new SQLInvalidAuthorizationSpecException("", "", 0), PermissionDeniedDataAccessException.class);
    doTest(new SQLNonTransientConnectionException("", "", 0), DataAccessResourceFailureException.class);
    doTest(new SQLRecoverableException("", "", 0), RecoverableDataAccessException.class);
    doTest(new SQLSyntaxErrorException("", "", 0), BadSqlGrammarException.class);
    doTest(new SQLTimeoutException("", "", 0), QueryTimeoutException.class);
    doTest(new SQLTransactionRollbackException("", "", 0), PessimisticLockingFailureException.class);
    doTest(new SQLTransactionRollbackException("", "40001", 0), CannotAcquireLockException.class);
    doTest(new SQLTransientConnectionException("", "", 0), TransientDataAccessResourceException.class);
  }

  @Test
  public void fallbackStateTranslation() {
    // Test fallback. We assume that no database will ever return this error code,
    // but 07xxx will be bad grammar picked up by the fallback SQLState translator
    doTest(new SQLException("", "07xxx", 666666666), BadSqlGrammarException.class);
    // and 08xxx will be data resource failure (non-transient) picked up by the fallback SQLState translator
    doTest(new SQLException("", "08xxx", 666666666), DataAccessResourceFailureException.class);
  }

  private void doTest(SQLException ex, Class<?> dataAccessExceptionType) {
    SQLExceptionTranslator translator = new SQLExceptionSubclassTranslator();
    DataAccessException dax = translator.translate("task", "SQL", ex);

    assertThat(dax).as("Specific translation must not result in null").isNotNull();
    assertThat(dax).as("Wrong DataAccessException type returned").isExactlyInstanceOf(dataAccessExceptionType);
    assertThat(dax.getCause()).as("The exact same original SQLException must be preserved").isSameAs(ex);
  }

}