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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import cn.taketoday.jdbc.core.ResultSetExtractor;

/**
 * User: dimzon Date: 4/7/14 Time: 12:02 AM
 */
public interface ResultSetHandlerFactory<T> {

  /**
   * Get one row ResultSetExtractor
   */
  ResultSetExtractor<T> getResultSetHandler(ResultSetMetaData resultSetMetaData) throws SQLException;
}
