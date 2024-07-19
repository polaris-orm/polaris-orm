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

package cn.taketoday.polaris.jdbc.format;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import cn.taketoday.polaris.format.DDLSQLFormatter;
import cn.taketoday.polaris.format.SqlStatementLogger;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/9/12 19:38
 */
class SqlStatementLoggerTests {
  SqlStatementLogger logger = new SqlStatementLogger(true, true, true, 20);

  @Test
  void log() {

    logger.logStatement("SELECT * FROM t_user where id = ?");
    logger.logStatement("SELECT * FROM t_user where id = ?", DDLSQLFormatter.INSTANCE);
    logger.logSlowQuery("SELECT * FROM t_user where id = ?", System.nanoTime() - TimeUnit.MINUTES.toNanos(2));

    logger.logStatement(
            "create table issue5table(id int identity primary key, val integer)", DDLSQLFormatter.INSTANCE);

  }

}