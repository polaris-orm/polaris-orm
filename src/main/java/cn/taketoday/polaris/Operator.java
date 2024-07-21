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

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/9/10 23:04
 */
public interface Operator {

  Operator GRATE_THAN = plain(" > ?");
  Operator GRATE_EQUALS = plain(" >= ?");
  Operator LESS_THAN = plain(" < ?");
  Operator LESS_EQUALS = plain(" <= ?");

  Operator EQUALS = plain(" = ?");
  Operator NOT_EQUALS = plain(" <> ?");

  Operator IS_NULL = plain(" is null");
  Operator IS_NOT_NULL = plain(" is not null");

  Operator LIKE = plain(" like concat('%', ?, '%')");
  Operator SUFFIX_LIKE = plain(" like concat('%', ?)");
  Operator PREFIX_LIKE = plain(" like concat(?, '%')");

  Operator BETWEEN = plain(" BETWEEN ? AND ?");
  Operator NOT_BETWEEN = plain(" NOT BETWEEN ? AND ?");

  Operator IN = in(false);
  Operator NOT_IN = in(true);

  /**
   * Render this operator and value-sequence to StringBuilder
   * <p> sql snippet must start with a space
   *
   * @param sql SQL appender
   * @param value parameter to test
   * @param valueLength parameter length
   */
  void render(StringBuilder sql, @Nullable Object value, int valueLength);

  // Static Factory Methods

  static Operator plain(String placeholder) {
    return new Plain(placeholder);
  }

  static Operator in(boolean notIn) {
    return new In(notIn);
  }

  /**
   * column_name operator value;
   */
  record Plain(String placeholder) implements Operator {

    @Override
    public void render(StringBuilder sql, Object value, int valueLength) {
      sql.append(placeholder);
    }

  }

  class In implements Operator {

    private final boolean notIn;

    public In(boolean notIn) {
      this.notIn = notIn;
    }

    @Override
    public void render(StringBuilder sql, @Nullable Object value, int valueLength) {
      if (notIn) {
        sql.append(" NOT");
      }
      sql.append(" IN (");
      for (int i = 0; i < valueLength; i++) {
        if (i == 0) {
          sql.append('?');
        }
        else {
          sql.append(",?");
        }
      }
      sql.append(')');
    }

  }

}
