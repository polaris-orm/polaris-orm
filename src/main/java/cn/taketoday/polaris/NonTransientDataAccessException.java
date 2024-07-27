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

package cn.taketoday.polaris;

import cn.taketoday.polaris.util.Nullable;

/**
 * 该异常被认为是非瞬时的异常
 *
 * <p>如果异常的原因没有被纠正，那么对相同操作的重试将会失败。
 *
 * @author Thomas Risberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.sql.SQLNonTransientException
 * @since 1.0
 */
public abstract class NonTransientDataAccessException extends DataAccessException {

  /**
   * Constructor for NonTransientDataAccessException.
   *
   * @param msg the detail message
   */
  public NonTransientDataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for NonTransientDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using a underlying
   * data access API such as JDBC)
   */
  public NonTransientDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
