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

import cn.taketoday.polaris.util.Nullable;

/**
 * 事务管理器
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/21 16:55
 */
public interface TransactionManager {

  /**
   * @see TransactionConfig#getPropagationBehavior
   * @see TransactionConfig#getIsolationLevel
   * @see TransactionConfig#getTimeout
   * @see TransactionConfig#isReadOnly
   */
  Transaction getTransaction(@Nullable TransactionConfig config);

}
