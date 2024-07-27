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

package cn.taketoday.polaris.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.polaris.util.Nullable;

/**
 * JDBC 连接源
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/20 10:58
 */
public interface ConnectionSource {

  /**
   * 获取一个 JDBC 连接
   * <p>
   * 可以是全新的 JDBC 连接。也可以是某次会话中的连接，在如何获取取决于底层
   *
   * @return JDBC 连接
   * @throws SQLException JDBC 连接获取异常, 该异常可以用 {@code SQLExceptionTranslator} 处理
   * @see cn.taketoday.polaris.jdbc.support.SQLExceptionTranslator
   */
  default Connection getConnection() throws SQLException {
    return createConnection();
  }

  /**
   * 创建一个全新的连接, 不同于 {@link #getConnection()} 获取逻辑要取决于底层策略
   *
   * @return 一个全新的 JDBC 连接
   * @throws SQLException JDBC 连接创建异常, 该异常可以用 {@code SQLExceptionTranslator} 处理
   */
  Connection createConnection() throws SQLException;

  /**
   * 释放 JDBC 连接
   *
   * @param connection 要释放的 JDBC 连接
   * @throws SQLException 释放过程中发生异常
   */
  default void releaseConnection(Connection connection) throws SQLException {
    connection.close();
  }

  /**
   * 获取底层的数据源
   *
   * @return 底层的数据源
   */
  @Nullable
  default DataSource getDataSource() {
    return null;
  }

  // Static Factory Methods

  /**
   * 使用指定数据源{@link DataSource} 获取连接
   *
   * @see DataSourceConnectionSource
   */
  static ConnectionSource forDataSource(DataSource source) {
    return new DataSourceConnectionSource(source);
  }

  /**
   * 每次都从给定的连接信息获取一个全新的连接的 ConnectionSource
   *
   * @see java.sql.DriverManager#getConnection(String, Properties)
   */
  static ConnectionSource of(String url, Properties info) {
    return new DriverManagerConnectionSource(url, info);
  }

  /**
   * 每次都从给定的连接信息获取一个全新的连接的 ConnectionSource
   *
   * @param url JDBC 数据库连接地址
   * @param user 数据库用户名
   * @param password 数据库用户密码
   * @see java.sql.DriverManager#getConnection(String, Properties)
   */
  static ConnectionSource of(String url, String user, String password) {
    return new DriverManagerConnectionSource(url, user, password);
  }

  /**
   * connection
   */
  static ConnectionSource valueOf(final Connection connection) {
    final class NestedConnectionSource implements ConnectionSource {
      @Override
      public Connection createConnection() {
        return connection;
      }
    }
    return new NestedConnectionSource();
  }

}
