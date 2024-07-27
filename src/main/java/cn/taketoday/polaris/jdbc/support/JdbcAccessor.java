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

import java.sql.SQLException;

import cn.taketoday.polaris.DataAccessException;
import cn.taketoday.polaris.format.SqlStatementLogger;
import cn.taketoday.polaris.jdbc.UncategorizedSQLException;
import cn.taketoday.polaris.jdbc.datasource.ConnectionSource;
import cn.taketoday.polaris.logging.Logger;
import cn.taketoday.polaris.logging.LoggerFactory;
import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/20 10:03
 */
public abstract class JdbcAccessor {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  protected final ConnectionSource connectionSource;

  @Nullable
  private volatile SQLExceptionTranslator exceptionTranslator;

  protected JdbcAccessor(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  public ConnectionSource getConnectionSource() {
    return this.connectionSource;
  }

  /**
   * 设置 SQL 异常转换器, 可以将对应的 {@link SQLException} 异常转化为运行时异常
   */
  public void setExceptionTranslator(@Nullable SQLExceptionTranslator exceptionTranslator) {
    this.exceptionTranslator = exceptionTranslator;
  }

  /**
   * 返回异常转换器
   * <p>
   * 如果用户没有手动设计将返回 PolarisSQLExceptionTranslator
   *
   * @see #getConnectionSource()
   */
  public SQLExceptionTranslator getExceptionTranslator() {
    SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
    if (exceptionTranslator != null) {
      return exceptionTranslator;
    }
    synchronized(this) {
      exceptionTranslator = this.exceptionTranslator;
      if (exceptionTranslator == null) {
        exceptionTranslator = new PolarisSQLExceptionTranslator();
        this.exceptionTranslator = exceptionTranslator;
      }
      return exceptionTranslator;
    }
  }

  public void setStatementLogger(SqlStatementLogger stmtLogger) {
    Assert.notNull(stmtLogger, "SqlStatementLogger is required");
    this.stmtLogger = stmtLogger;
  }

  /**
   * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
   *
   * @param task readable text describing the task being attempted
   * @param sql the SQL query or update that caused the problem (may be {@code null})
   * @param ex the offending {@code SQLException}
   * @return a DataAccessException wrapping the {@code SQLException} (never {@code null})
   * @see #getExceptionTranslator()
   */
  public RuntimeException translateException(String task, @Nullable String sql, SQLException ex) {
    RuntimeException dae = getExceptionTranslator().translate(task, sql, ex);
    return dae != null ? dae : new UncategorizedSQLException(task, sql, ex);
  }

}
