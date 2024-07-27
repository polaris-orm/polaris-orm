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
 * 如果无法检索到某些预期的数据，例如在通过已知标识符查找特定数据时，将抛出此异常。
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public class DataRetrievalFailureException extends NonTransientDataAccessException {

  /**
   * Constructor for DataRetrievalFailureException.
   *
   * @param msg the detail message
   */
  public DataRetrievalFailureException(String msg) {
    super(msg);
  }

  /**
   * Constructor for DataRetrievalFailureException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public DataRetrievalFailureException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
