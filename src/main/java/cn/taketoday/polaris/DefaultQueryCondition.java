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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;

import cn.taketoday.polaris.type.ObjectTypeHandler;
import cn.taketoday.polaris.type.TypeHandler;
import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.CollectionUtils;
import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/9/10 21:34
 */
public class DefaultQueryCondition extends QueryCondition {

  protected final boolean nullable;

  @SuppressWarnings({ "rawtypes" })
  protected TypeHandler typeHandler = ObjectTypeHandler.sharedInstance;

  protected final String columnName;

  protected final Operator operator;

  @Nullable
  protected final Object parameterValue; // Object, array, list

  protected final int valueLength;

  public DefaultQueryCondition(String columnName,
          Operator operator, @Nullable Object parameterValue) {
    this(columnName, operator, parameterValue, false);
  }

  /**
   * @param nullable parameter-value match null
   */
  public DefaultQueryCondition(String columnName,
          Operator operator, @Nullable Object parameterValue, boolean nullable) {
    Assert.notNull(operator, "operator is required");
    Assert.notNull(columnName, "columnName is required");
    this.parameterValue = parameterValue;
    this.operator = operator;
    this.columnName = columnName;
    this.valueLength = getLength(parameterValue);
    this.nullable = nullable;
  }

  public void setTypeHandler(TypeHandler<?> typeHandler) {
    Assert.notNull(typeHandler, "typeHandler is required");
    this.typeHandler = typeHandler;
  }

  @Override
  protected boolean matches() {
    return nullable || parameterValue != null; // TODO
  }

  /**
   * @param ps PreparedStatement
   * @param idx current parameter-index
   * @throws SQLException if parameterIndex does not correspond to a parameter
   * marker in the SQL statement; if a database access error occurs;
   * this method is called on a closed {@code PreparedStatement}
   * or the type of the given object is ambiguous
   */
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected int setParameterInternal(PreparedStatement ps, int idx) throws SQLException {
    int valueLength = this.valueLength;
    if (valueLength != 1) {
      // array, collection
      final Object parameterValue = this.parameterValue;
      final TypeHandler typeHandler = this.typeHandler;
      if (parameterValue instanceof Object[] array) {
        for (int i = 0; i < valueLength; i++) {
          typeHandler.setParameter(ps, idx + i, array[i]);
        }
      }
      else if (parameterValue != null) {
        int i = idx;
        for (Object parameter : (Iterable<Object>) parameterValue) {
          typeHandler.setParameter(ps, i++, parameter);
        }
      }
      return idx + valueLength;
    }
    else {
      Object parameterValue = this.parameterValue;
      if (parameterValue instanceof Collection<?> coll) {
        parameterValue = CollectionUtils.firstElement(coll);
      }
      else if (parameterValue instanceof Object[] array) {
        parameterValue = array[0];
      }
      typeHandler.setParameter(ps, idx, parameterValue);
      return idx + 1;
    }
  }

  @Override
  protected void renderInternal(StringBuilder sql) {
    // column_name
    sql.append(" `");
    sql.append(columnName);
    sql.append('`');

    // operator and value

    operator.render(sql, parameterValue, valueLength);
  }

  //

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DefaultQueryCondition that))
      return false;
    return Objects.equals(typeHandler, that.typeHandler)
            && Objects.equals(columnName, that.columnName)
            && Objects.equals(parameterValue, that.parameterValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeHandler, columnName, parameterValue);
  }

  @Override
  public String toString() {
    return "DefaultQueryCondition{columnName='%s', parameterValue=%s}".formatted(columnName, parameterValue);
  }
}
