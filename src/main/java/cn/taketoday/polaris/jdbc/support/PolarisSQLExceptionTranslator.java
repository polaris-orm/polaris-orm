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

/**
 * 默认实现
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/22 11:44
 */
public class PolarisSQLExceptionTranslator implements SQLExceptionTranslator {

  @Override
  public RuntimeException translate(String task, String sql, SQLException ex) {

    return null;
  }

}
