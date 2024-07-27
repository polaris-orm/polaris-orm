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
 * 未分类的数据访问异常
 * <p>
 * 当我们无法区分具体的异常的超类：例如，我们无法更精确地确定的来自 JDBC 的 SQLException。
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public abstract class UncategorizedDataAccessException extends NonTransientDataAccessException {

  /**
   * Constructor for UncategorizedDataAccessException.
   *
   * @param msg the detail message
   * @param cause the exception thrown by underlying data access API
   */
  public UncategorizedDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
