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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Properties;

/**
 * 使用 DriverManager 建立连接
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
final class DriverManagerConnectionSource implements ConnectionSource {

  private final String url;

  private final Properties properties;

  DriverManagerConnectionSource(String url, Properties properties) {
    this.url = fixUrl(url);
    this.properties = properties;
  }

  DriverManagerConnectionSource(String url, String user, String password) {
    this(url, new Properties());
    if (user != null) {
      properties.put("user", user);
    }
    if (password != null) {
      properties.put("password", password);
    }
  }

  private static String fixUrl(String url) {
    if (url.startsWith("jdbc")) {
      return url;
    }
    return "jdbc:".concat(url);
  }

  /**
   * Attempts to establish a connection to the given database URL.
   * The <code>DriverManager</code> attempts to select an appropriate driver from
   * the set of registered JDBC drivers.
   * <p>
   * <B>Note:</B> If a property is specified as part of the {@code url} and
   * is also specified in the {@code Properties} object, it is
   * implementation-defined as to which value will take precedence.
   * For maximum portability, an application should only specify a
   * property once.
   *
   * @return a Connection to the URL
   * @throws SQLException if a database access error occurs or the url is
   * {@code null}
   * @throws SQLTimeoutException when the driver has determined that the
   * timeout value specified by the {@code setLoginTimeout} method
   * has been exceeded and has at least tried to cancel the
   * current database connection attempt
   */
  @Override
  public Connection createConnection() throws SQLException {
    return DriverManager.getConnection(url, properties);
  }

}