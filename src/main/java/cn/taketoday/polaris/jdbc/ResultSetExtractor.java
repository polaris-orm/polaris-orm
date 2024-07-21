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

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.dao.DataAccessException;

/**
 * 数据提取器
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/20 09:50
 */
public interface ResultSetExtractor<T> {

  /**
   * 从 ResultSet 里读取数据
   * <p>
   * 子类实现不应该关闭 ResultSet，它将在外部关闭。
   *
   * @param rs 从 ResultSet 中提取数据。
   * @return 任意结果对象，如果没有则为空(在后一种情况下提取器通常是有状态的)。
   * @throws SQLException 在读取数据的时候发生异常
   */
  T extractData(ResultSet rs) throws SQLException, DataAccessException;

}
