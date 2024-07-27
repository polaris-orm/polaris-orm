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

import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.DataAccessException;

/**
 * SQLException 转化为运行时异常
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/22 21:16
 */
public class PolarisSQLRuntimeException extends DataAccessException {

  /** SQL that led to the problem. */

  @Nullable
  private final String sql;

  public PolarisSQLRuntimeException(String task, @Nullable String sql, SQLException cause) {
    super(task + " failed", cause);
    this.sql = sql;
  }

  /**
   * Return the underlying SQLException.
   */
  public SQLException getSQLException() {
    return (SQLException) getCause();
  }

  /**
   * Return the SQL that led to the problem (if known).
   */
  @Nullable
  public String getSql() {
    return this.sql;
  }

}
