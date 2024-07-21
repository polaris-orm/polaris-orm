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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Generic utility methods for working with JDBC. Mainly for internal use
 * within the framework, but also useful for custom JDBC access code.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public abstract class JdbcUtils {

  private static final Logger log = LoggerFactory.getLogger(JdbcUtils.class);

  /**
   * Determine the column name to use. The column name is determined based on a
   * lookup using ResultSetMetaData.
   * <p>This method implementation takes into account recent clarifications
   * expressed in the JDBC 4.0 specification:
   * <p><i>columnLabel - the label for the column specified with the SQL AS clause.
   * If the SQL AS clause was not specified, then the label is the name of the column</i>.
   *
   * @param resultSetMetaData the current meta-data to use
   * @param columnIndex the index of the column for the look up
   * @return the column name to use
   * @throws SQLException in case of lookup failure
   */
  public static String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
    String name = resultSetMetaData.getColumnLabel(columnIndex);
    if (StringUtils.isEmpty(name)) {
      name = resultSetMetaData.getColumnName(columnIndex);
    }
    return name;
  }

  /**
   * Close the given JDBC Connection and ignore any thrown exception.
   * This is useful for typical finally blocks in manual JDBC code.
   *
   * @param con the JDBC Connection to close (may be {@code null})
   */
  public static void closeConnection(@Nullable Connection con) {
    if (con != null) {
      try {
        con.close();
      }
      catch (SQLException ex) {
        log.debug("Could not close JDBC Connection", ex);
      }
      catch (Throwable ex) {
        // We don't trust the JDBC driver: It might throw RuntimeException or Error.
        log.debug("Unexpected exception on closing JDBC Connection", ex);
      }
    }
  }

  /**
   * Close the given JDBC Statement and ignore any thrown exception.
   * This is useful for typical finally blocks in manual JDBC code.
   *
   * @param stmt the JDBC Statement to close (may be {@code null})
   */
  public static void closeStatement(@Nullable Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      }
      catch (SQLException ex) {
        log.trace("Could not close JDBC Statement", ex);
      }
      catch (Throwable ex) {
        // We don't trust the JDBC driver: It might throw RuntimeException or Error.
        log.trace("Unexpected exception on closing JDBC Statement", ex);
      }
    }
  }

  /**
   * Close the given JDBC ResultSet and ignore any thrown exception.
   * This is useful for typical finally blocks in manual JDBC code.
   *
   * @param rs the JDBC ResultSet to close (may be {@code null})
   */
  public static void closeResultSet(@Nullable ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      }
      catch (SQLException ex) {
        log.trace("Could not close JDBC ResultSet", ex);
      }
      catch (Throwable ex) {
        // We don't trust the JDBC driver: It might throw RuntimeException or Error.
        log.trace("Unexpected exception on closing JDBC ResultSet", ex);
      }
    }
  }

  /**
   * Close a <code>Connection</code>, avoid closing if null.
   *
   * @param conn Connection to close.
   * @throws SQLException If a database access error occurs
   */
  public static void close(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      conn.close();
    }
  }

  /**
   * Close a <code>ResultSet</code>, avoid closing if null.
   *
   * @param rs ResultSet to close.
   * @throws SQLException If a database access error occurs
   */
  public static void close(@Nullable ResultSet rs) throws SQLException {
    if (rs != null) {
      rs.close();
    }
  }

  /**
   * Close a <code>Statement</code>, avoid closing if null.
   *
   * @param stmt Statement to close.
   * @throws SQLException If a database access error occurs
   */
  public static void close(@Nullable Statement stmt) throws SQLException {
    if (stmt != null) {
      stmt.close();
    }
  }

  /**
   * Close a <code>Connection</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param conn Connection to close.
   */
  public static void closeQuietly(@Nullable Connection conn) {
    try {
      close(conn);
    }
    catch (SQLException e) {
      log.warn("Could not close connection. connection: {}", conn, e);
    }
  }

  /**
   * Close a <code>Connection</code>, <code>Statement</code> and
   * <code>ResultSet</code>. Avoid closing if null and hide any SQLExceptions that
   * occur.
   *
   * @param conn Connection to close.
   * @param stmt Statement to close.
   * @param rs ResultSet to close.
   */
  public static void closeQuietly(@Nullable Connection conn, @Nullable Statement stmt, @Nullable ResultSet rs) {
    try {
      closeQuietly(rs);
    }
    finally {
      try {
        closeQuietly(stmt);
      }
      finally {
        closeQuietly(conn);
      }
    }
  }

  /**
   * Close a <code>ResultSet</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param rs ResultSet to close.
   */
  public static void closeQuietly(@Nullable ResultSet rs) {
    try {
      close(rs);
    }
    catch (SQLException e) {
      log.warn("Could not close ResultSet. result-set: {}", rs, e);
    }
  }

  /**
   * Close a <code>Statement</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param stmt Statement to close.
   */
  public static void closeQuietly(@Nullable Statement stmt) {
    try {
      close(stmt);
    }
    catch (SQLException e) {
      log.warn("Could not close statement. statement: {}", stmt, e);
    }
  }

  /**
   * Commits a <code>Connection</code> then closes it, avoid closing if null.
   *
   * @param conn Connection to close.
   * @throws SQLException If a database access error occurs
   */
  public static void commitAndClose(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      try (conn) {
        conn.commit();
      }
    }
  }

  /**
   * Commits a <code>Connection</code> then closes it, avoid closing if null and
   * hide any SQLExceptions that occur.
   *
   * @param conn Connection to close.
   */
  public static void commitAndCloseQuietly(@Nullable Connection conn) {
    try {
      commitAndClose(conn);
    }
    catch (SQLException e) {
      log.warn("Could not commit and close. Connection: {}", conn, e);
    }
  }

  /**
   * Print the stack trace for a SQLException to STDERR.
   *
   * @param e SQLException to print stack trace of
   */
  public static void printStackTrace(SQLException e) {
    printStackTrace(e, new PrintWriter(System.err));
  }

  /**
   * Print the stack trace for a SQLException to a specified PrintWriter.
   *
   * @param e SQLException to print stack trace of
   * @param pw PrintWriter to print to
   */
  public static void printStackTrace(SQLException e, PrintWriter pw) {
    SQLException next = e;
    while (next != null) {
      next.printStackTrace(pw);
      next = next.getNextException();
      if (next != null) {
        pw.println("Next SQLException:");
      }
    }
  }

  /**
   * Print warnings on a Connection to STDERR.
   *
   * @param conn Connection to print warnings from
   */
  public static void printWarnings(Connection conn) {
    printWarnings(conn, new PrintWriter(System.err));
  }

  /**
   * Print warnings on a Connection to a specified PrintWriter.
   *
   * @param conn Connection to print warnings from
   * @param pw PrintWriter to print to
   */
  public static void printWarnings(@Nullable Connection conn, PrintWriter pw) {
    if (conn != null) {
      try {
        printStackTrace(conn.getWarnings(), pw);
      }
      catch (SQLException e) {
        printStackTrace(e, pw);
      }
    }
  }

  /**
   * Rollback any changes made on the given connection.
   *
   * @param conn Connection to rollback. A null value is legal.
   * @throws SQLException If a database access error occurs
   */
  public static void rollback(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      conn.rollback();
    }
  }

  /**
   * Performs a rollback on the <code>Connection</code> then closes it, avoid
   * closing if null.
   *
   * @param conn Connection to rollback. A null value is legal.
   * @throws SQLException If a database access error occurs
   */
  public static void rollbackAndClose(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      try (conn) {
        conn.rollback();
      }
    }
  }

}
