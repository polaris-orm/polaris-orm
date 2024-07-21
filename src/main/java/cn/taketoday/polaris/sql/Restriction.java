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

package cn.taketoday.polaris.sql;

import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * A restriction (predicate) to be applied to a query
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public interface Restriction {

  /**
   * Render the restriction into the SQL buffer
   */
  void render(StringBuilder sqlBuffer);

  // Static Factory Methods

  static Restriction plain(CharSequence sequence) {
    return new Plain(sequence);
  }

  /**
   * equal
   */
  static Restriction equal(String columnName) {
    return new ComparisonRestriction(columnName, " = ", "?");
  }

  /**
   * equal
   */
  static Restriction equal(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " = ", rhs);
  }

  /**
   * not equal
   */
  static Restriction notEqual(String columnName) {
    return new ComparisonRestriction(columnName, " <> ", "?");
  }

  /**
   * not equal
   */
  static Restriction notEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <> ", rhs);
  }

  static Restriction graterThan(String columnName) {
    return graterThan(columnName, "?");
  }

  static Restriction graterThan(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " > ", rhs);
  }

  static Restriction graterEqual(String columnName) {
    return graterEqual(columnName, "?");
  }

  static Restriction graterEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " >= ", rhs);
  }

  static Restriction lessThan(String columnName) {
    return lessThan(columnName, "?");
  }

  static Restriction lessThan(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " < ", rhs);
  }

  static Restriction lessEqual(String columnName) {
    return lessEqual(columnName, "?");
  }

  static Restriction lessEqual(String lhs, String rhs) {
    return new ComparisonRestriction(lhs, " <= ", rhs);
  }

  static Restriction forOperator(String lhs, String operator, String rhs) {
    return new ComparisonRestriction(lhs, operator, rhs);
  }

  /**
   * Null-ness restriction - IS (NOT)? NULL
   */
  static Restriction isNull(String columnName) {
    return new NullnessRestriction(columnName, true);
  }

  /**
   * Null-ness restriction - IS (NOT)? NULL
   */
  static Restriction isNotNull(String columnName) {
    return new NullnessRestriction(columnName, false);
  }

  /**
   * Render the restriction into the SQL buffer
   */
  static void render(@Nullable Collection<? extends Restriction> restrictions, StringBuilder buf) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      buf.append(" WHERE ");
      renderWhereClause(restrictions, buf);
    }
  }

  /**
   * Render the restriction into the SQL buffer
   */
  @Nullable
  static StringBuilder renderWhereClause(@Nullable Collection<? extends Restriction> restrictions) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      StringBuilder buf = new StringBuilder(restrictions.size() * 10);
      renderWhereClause(restrictions, buf);
      return buf;
    }
    return null;
  }

  /**
   * Render the restriction into the SQL buffer
   */
  static void renderWhereClause(Collection<? extends Restriction> restrictions, StringBuilder buf) {
    boolean appended = false;
    for (Restriction restriction : restrictions) {
      if (appended) {
        buf.append(" AND ");
      }
      else {
        appended = true;
      }
      restriction.render(buf);
    }
  }

  class Plain implements Restriction {

    private final CharSequence sequence;

    Plain(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public void render(StringBuilder sqlBuffer) {
      sqlBuffer.append(sequence);
    }

  }

}
