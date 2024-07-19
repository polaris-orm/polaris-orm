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

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.sql.Select;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/11 21:51
 */
public abstract class QueryCondition extends ColumnsQueryStatement implements QueryStatement, DebugDescriptive {

  @Nullable
  protected String logic;

  @Nullable
  protected QueryCondition nextNode;

  @Nullable
  protected QueryCondition preNode;

  protected abstract boolean matches();

  /**
   * @param ps PreparedStatement
   * @return nextIdx
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  public int setParameter(PreparedStatement ps) throws SQLException {
    return setParameter(ps, 1);
  }

  protected int setParameter(PreparedStatement ps, int idx) throws SQLException {
    int nextIdx = setParameterInternal(ps, idx);
    if (nextNode != null) {
      nextIdx = nextNode.setParameter(ps, nextIdx);
    }
    return nextIdx;
  }

  protected abstract int setParameterInternal(PreparedStatement ps, int idx) throws SQLException;

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    StringBuilder whereClause = new StringBuilder();
    render(whereClause);
    select.setWhereClause(whereClause);
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    setParameter(statement);
  }

  /**
   * append sql
   *
   * @param sql sql append to
   * @return if rendered to sql StringBuilder
   */
  public boolean render(StringBuilder sql) {
    if (matches()) {
      // format: column_name operator value;
      if (preNode != null && logic != null) {
        // not first condition
        sql.append(' ');
        sql.append(logic);
      }

      renderInternal(sql);

      if (nextNode != null) {
        nextNode.render(sql);
      }
      return true;
    }
    return false;
  }

  protected abstract void renderInternal(StringBuilder sql);

  /**
   * <p>
   * This Method should use once
   *
   * @param next Next condition
   * @return this
   */
  public QueryCondition and(QueryCondition next) {
    next.logic = "AND";
    setNext(next);
    return this;
  }

  /**
   * <p>
   * This Method should use once
   *
   * @param next Next condition
   * @return this
   */
  public QueryCondition or(QueryCondition next) {
    next.logic = "OR";
    setNext(next);
    return this;
  }

  protected void setNext(QueryCondition next) {
    this.nextNode = next;
    next.preNode = this;
  }

  @Override
  public String getDescription() {
    return "Query entities with condition";
  }

  // Static factory methods

  public static cn.taketoday.polaris.DefaultQueryCondition of(String columnName, cn.taketoday.polaris.Operator operator, Object value) {
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, operator, value);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition isEqualsTo(String columnName, Object value) {
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.EQUALS, value);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition between(String columnName, Object array) {
    Assert.isTrue(getLength(array) == 2, "BETWEEN expression must have left and right value");
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.BETWEEN, array);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition between(String columnName, Object left, Object right) {
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.BETWEEN, new Object[] { left, right });
  }

  public static cn.taketoday.polaris.DefaultQueryCondition notBetween(String columnName, Object array) {
    Assert.isTrue(getLength(array) == 2, "BETWEEN expression must have left and right value");
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.BETWEEN, array);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition notBetween(String columnName, Object left, Object right) {
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.NOT_BETWEEN, new Object[] { left, right });
  }

  public static cn.taketoday.polaris.DefaultQueryCondition isNotNull(String columnName) {
    return new NullQueryCondition(columnName, cn.taketoday.polaris.Operator.IS_NOT_NULL);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition isNull(String columnName) {
    return new NullQueryCondition(columnName, cn.taketoday.polaris.Operator.IS_NULL);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition nullable(String columnName, Object value) {
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.EQUALS, value, true);
  }

  public static cn.taketoday.polaris.DefaultQueryCondition isNotEmpty(String columnName, Object value) {
    return new cn.taketoday.polaris.DefaultQueryCondition(columnName, cn.taketoday.polaris.Operator.EQUALS, value) {
      @Override
      public boolean matches() {
        return ObjectUtils.isNotEmpty(parameterValue);
      }
    };
  }

  /**
   * <pre>
   *   {@code
   *     QueryCondition condition = QueryCondition.nested(
   *             QueryCondition.isEqualsTo("name", "TODAY")
   *                     .or(QueryCondition.equalsTo("age", 10))
   *     ).and(
   *             QueryCondition.nested(
   *                     QueryCondition.isEqualsTo("gender", Gender.MALE)
   *                             .and(QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday"))
   *             )
   *     );
   *
   *    // will produce SQL:
   *     ( `name` = ? OR `age` = ? ) AND ( `gender` = ? AND `email` like concat(?, '%') )
   *
   *   }
   * </pre>
   *
   * @param condition normal condition
   * @return nested condition
   */
  public static cn.taketoday.polaris.NestedQueryCondition nested(QueryCondition condition) {
    return new NestedQueryCondition(condition);
  }

  //

  static int getLength(@Nullable Object value) {
    if (ObjectUtils.isArray(value)) {
      return Array.getLength(value);
    }
    else if (value instanceof Collection<?> collection) {
      return collection.size();
    }
    else {
      return 1;
    }
  }

  static class NullQueryCondition extends DefaultQueryCondition {

    public NullQueryCondition(String columnName, Operator operator) {
      super(columnName, operator, null, true);
    }

    @Override
    protected boolean matches() {
      return true;
    }

    @Override
    protected int setParameterInternal(PreparedStatement ps, int idx) {
      return idx;
    }

  }

}
