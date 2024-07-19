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

package cn.taketoday.polaris.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.lang.Assert;

/**
 * <p>
 * Use {@link Enum#name()}
 * </p>
 *
 * @author Clinton Begin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public class EnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  private final Class<E> type;

  public EnumTypeHandler(Class<E> type) {
    Assert.notNull(type, "Type argument cannot be null");
    this.type = type;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter) throws SQLException {
    ps.setString(i, parameter.name());
  }

  @Override
  public E getResult(ResultSet rs, String columnName) throws SQLException {
    String s = rs.getString(columnName);
    return s == null ? null : Enum.valueOf(type, s);
  }

  @Override
  public E getResult(ResultSet rs, int columnIndex) throws SQLException {
    String s = rs.getString(columnIndex);
    return s == null ? null : Enum.valueOf(type, s);
  }

  @Override
  public E getResult(CallableStatement cs, int columnIndex) throws SQLException {
    String s = cs.getString(columnIndex);
    return s == null ? null : Enum.valueOf(type, s);
  }
}
