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

/**
 * 当更新过程中似乎发生了意外情况，但事务尚未回滚时，抛出数据访问异常。
 * <p>
 * 例如，当我们想更新 RDBMS 中的 1 行，但实际上更新了 3 行时，就会抛出该异常。
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/22 11:37
 */
public class IncorrectUpdateSemanticsDataAccessException extends InvalidDataAccessResourceUsageException {

  /**
   * Constructor for IncorrectUpdateSemanticsDataAccessException.
   *
   * @param msg the detail message
   */
  public IncorrectUpdateSemanticsDataAccessException(String msg) {
    super(msg);
  }

  /**
   * Constructor for IncorrectUpdateSemanticsDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause from the underlying API, such as JDBC
   */
  public IncorrectUpdateSemanticsDataAccessException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * 返回数据是否已更新。如果此方法返回 false，则无可回滚。
   * <p>
   * 默认实现始终返回 true。这可以在子类中重写。
   */
  public boolean wasDataUpdated() {
    return true;
  }

}
