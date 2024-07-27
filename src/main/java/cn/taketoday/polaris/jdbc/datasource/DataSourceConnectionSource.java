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

import javax.sql.DataSource;

import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.Nullable;

/**
 * 默认实现，从指定数据源{@link DataSource} 获取连接
 */
final class DataSourceConnectionSource implements ConnectionSource {

  private final DataSource dataSource;

  /**
   * Creates a ConnectionSource that gets connection from specified
   * {@link DataSource}
   *
   * @param dataSource a DataSource to get connections from
   */
  DataSourceConnectionSource(DataSource dataSource) {
    Assert.notNull(dataSource, "DataSource is required");
    this.dataSource = dataSource;
  }

  /**
   * <p>Attempts to establish a connection with the data source that
   * this {@code DataSource} object represents.
   *
   * @return a connection to the data source
   * @throws SQLException if a database access error occurs
   * @throws java.sql.SQLTimeoutException when the driver has determined that the
   * timeout value specified by the {@code setLoginTimeout} method
   * has been exceeded and has at least tried to cancel the
   * current database connection attempt
   */
  @Override
  public Connection createConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Nullable
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

}