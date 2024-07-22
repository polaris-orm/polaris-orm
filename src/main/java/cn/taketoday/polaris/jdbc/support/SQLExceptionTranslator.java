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

import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.DataAccessException;

/**
 * 转换 {@link SQLException SQLExceptions} 到 {@link DataAccessException} 的策略接口
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/20 10:03
 */
public interface SQLExceptionTranslator {

  /**
   * 将给定的 {@link SQLException} 转换为通用的 {@link DataAccessException}。
   *
   * @param task 描述尝试执行的任务
   * @param sql 造成异常的 SQL，如果不清楚则传递 {@code null}
   * @param ex 原始 {@code SQLException}
   * @return 包装了 SQLException 的 DataAccessException，或者如果没有应用特定的转换，则为 {@code null}。
   * @see DataAccessException#getRootCause()
   */
  @Nullable
  RuntimeException translate(String task, @Nullable String sql, SQLException ex);

}
