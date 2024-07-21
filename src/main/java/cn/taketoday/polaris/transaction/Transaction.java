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

/**
 * 事物
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/21 10:52
 */
public interface Transaction {

  /**
   * 获取 JDBC 连接
   *
   * @return DataBase connection
   * @throws SQLException 连接获取失败
   */
  Connection getConnection() throws SQLException;

  /**
   * 提交事物
   *
   * @throws SQLException 提交错误
   */
  void commit() throws SQLException;

  /**
   * 回滚事物
   *
   * @throws SQLException 回滚错误
   */
  void rollback() throws SQLException;

  /**
   * 关闭 JDBC 连接
   *
   * @throws SQLException 关闭错误
   */
  void close() throws SQLException;

}
