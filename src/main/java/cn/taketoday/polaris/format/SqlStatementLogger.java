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

package cn.taketoday.polaris.format;

import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import cn.taketoday.polaris.logging.LogFormatUtils;
import cn.taketoday.polaris.logging.Logger;
import cn.taketoday.polaris.logging.LoggerFactory;
import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.PolarisProperties;

/**
 * Centralize logging for SQL statements.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/9/12 19:19
 */
public class SqlStatementLogger {

  private static final Logger sqlLogger = LoggerFactory.getLogger("polaris.SQL");

  private static final Logger slowLogger = LoggerFactory.getLogger("polaris.SQL_SLOW");

  public static final SqlStatementLogger sharedInstance = new SqlStatementLogger(
          PolarisProperties.getFlag("polaris.sql.logToStdout", false),
          PolarisProperties.getFlag("polaris.sql.format", true),
          PolarisProperties.getFlag("polaris.sql.highlight", true),
          PolarisProperties.getFlag("polaris.sql.stdoutOnly", false),
          PolarisProperties.getLong("polaris.sql.logSlowQuery", 0)
  );

  private final boolean format;
  private final boolean logToStdout;
  private final boolean stdoutOnly;
  private final boolean highlight;

  /**
   * Configuration value that indicates slow query. (In milliseconds) 0 - disabled.
   */
  private final long logSlowQuery;

  /**
   * Constructs a new SqlStatementLogger instance.
   */
  public SqlStatementLogger() {
    this(false, false, false);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   */
  public SqlStatementLogger(boolean logToStdout, boolean format) {
    this(logToStdout, format, false);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight) {
    this(logToStdout, format, highlight, 0);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   * @param logSlowQuery Should we logs query which executed slower than specified milliseconds. 0 - disabled.
   */
  public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
    this(logToStdout, format, highlight, false, logSlowQuery);
  }

  /**
   * Constructs a new SqlStatementLogger instance.
   *
   * @param logToStdout Should we log to STDOUT in addition to our internal logger.
   * @param format Should we format the statements in the console and log
   * @param highlight Should we highlight the statements in the console
   * @param stdoutOnly just log to std out
   * @param logSlowQuery Should we logs query which executed slower than specified milliseconds. 0 - disabled.
   */
  public SqlStatementLogger(boolean logToStdout, boolean format,
          boolean highlight, boolean stdoutOnly, long logSlowQuery) {
    this.logToStdout = logToStdout;
    this.format = format;
    this.highlight = highlight;
    this.stdoutOnly = stdoutOnly;
    this.logSlowQuery = logSlowQuery;
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isDebugEnabled() {
    return sqlLogger.isDebugEnabled();
  }

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return True if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public boolean isSlowDebugEnabled() {
    return slowLogger.isDebugEnabled();
  }

  /**
   * Log a SQL statement string.
   *
   * @param statement The SQL statement.
   */
  public void logStatement(String statement) {
    logStatement(null, statement);
  }

  /**
   * Log a SQL statement string.
   *
   * @param desc description of this SQL
   * @param statement The SQL statement.
   */
  public void logStatement(@Nullable Object desc, CharSequence statement) {
    // for now just assume a DML log for formatting
    logStatement(desc, statement, BasicSQLFormatter.INSTANCE);
  }

  /**
   * Log a SQL statement string using the specified formatter
   *
   * @param statement The SQL statement.
   * @param formatter The formatter to use.
   */
  public void logStatement(String statement, SQLFormatter formatter) {
    logStatement(null, statement, formatter);
  }

  /**
   * Log a SQL statement string using the specified formatter
   *
   * @param desc description of this SQL
   * @param statement The SQL statement.
   * @param formatter The formatter to use.
   */
  public void logStatement(@Nullable Object desc, CharSequence statement, SQLFormatter formatter) {
    if (format) {
      statement = formatter.format(statement.toString());
    }
    if (highlight) {
      statement = HighlightingSQLFormatter.INSTANCE.format(statement.toString());
    }

    if (!stdoutOnly) {
      if (desc != null) {
        String sql = statement.toString();
        LogFormatUtils.traceDebug(sqlLogger,
                traceOn -> LogFormatUtils.formatValue(desc, !traceOn) + ", SQL: " + sql);
      }
      else {
        sqlLogger.debug(statement);
      }
    }

    if (stdoutOnly || logToStdout) {
      String prefix = highlight ? "\u001b[35m[polaris-orm]\u001b[0m " : "polaris-orm: ";
      System.out.println(prefix + statement);
    }
  }

  /**
   * Log a slow SQL query
   *
   * @param statement SQL statement.
   * @param startTimeNanos Start time in nanoseconds.
   */
  public void logSlowQuery(Statement statement, long startTimeNanos) {
    if (logSlowQuery < 1) {
      return;
    }
    logSlowQuery(statement.toString(), startTimeNanos);
  }

  /**
   * Log a slow SQL query
   *
   * @param sql The SQL query.
   * @param startTimeNanos Start time in nanoseconds.
   */
  public void logSlowQuery(String sql, long startTimeNanos) {
    if (logSlowQuery < 1) {
      return;
    }
    if (startTimeNanos <= 0) {
      throw new IllegalArgumentException("startTimeNanos [%d] should be greater than 0!".formatted(startTimeNanos));
    }

    long queryExecutionMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);

    if (queryExecutionMillis > logSlowQuery) {
      String logData = "SlowQuery: %d milliseconds. SQL: '%s'".formatted(queryExecutionMillis, sql);
      slowLogger.info(logData);
      if (logToStdout) {
        System.out.println(logData);
      }
    }
  }

  @Override
  public String toString() {
    return "SqlStatementLogger{format=%s, logToStdout=%s, stdoutOnly=%s, highlight=%s, logSlowQuery=%d}"
            .formatted(format, logToStdout, stdoutOnly, highlight, logSlowQuery);
  }

}
