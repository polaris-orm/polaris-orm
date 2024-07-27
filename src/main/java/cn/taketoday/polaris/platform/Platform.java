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

package cn.taketoday.polaris.platform;

import java.util.regex.Pattern;

import cn.taketoday.polaris.util.ClassUtils;
import cn.taketoday.polaris.util.StringUtils;

/**
 * SQL generate strategy
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0
 */
public abstract class Platform {

  /**
   * Characters used as opening for quoting SQL identifiers
   */
  public static final String QUOTE = "`\"[";

  /**
   * Characters used as closing for quoting SQL identifiers
   */
  public static final String CLOSED_QUOTE = "`\"]";
//  private static final Pattern SINGLE_QUOTE_PATTERN = Pattern.compile(
//          "'",
//          Pattern.LITERAL
//  );

  private static final Pattern ESCAPE_CLOSING_COMMENT_PATTERN = Pattern.compile("\\*/");
  private static final Pattern ESCAPE_OPENING_COMMENT_PATTERN = Pattern.compile("/\\*");

  public static CharSequence escapeComment(CharSequence comment) {
    if (StringUtils.isNotEmpty(comment)) {
      final String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher(comment).replaceAll("*\\\\/");
      return ESCAPE_OPENING_COMMENT_PATTERN.matcher(escaped).replaceAll("/\\\\*");
    }
    return comment;
  }

  /**
   * determine the appropriate for update fragment to use.
   *
   * @return The appropriate for update fragment.
   */
  public String getForUpdateString() {
    return " for update";
  }

  /**
   * The fragment used to insert a row without specifying any column values.
   * This is not possible on some databases.
   *
   * @return The appropriate empty values clause.
   */
  public String getNoColumnsInsertString() {
    return "VALUES ( )";
  }

  public static Platform forClasspath() {
    if (ClassUtils.isPresent("com.mysql.cj.jdbc.Driver")) {
      return new MySQLPlatform();
    }
    else if (ClassUtils.isPresent("oracle.jdbc.driver.OracleDriver")) {
      return new OraclePlatform();
    }
    else if (ClassUtils.isPresent("org.postgresql.Driver")) {
      return new PostgreSQLPlatform();
    }
    throw new IllegalStateException("Cannot determine database platform");
  }

}
