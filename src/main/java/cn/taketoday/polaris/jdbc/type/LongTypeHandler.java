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

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public class LongTypeHandler extends BaseTypeHandler<Long> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Long parameter) throws SQLException {
    ps.setLong(i, parameter);
  }

  @Override
  public Long getResult(ResultSet rs, String columnName) throws SQLException {
    long result = rs.getLong(columnName);
    return result == 0L && rs.wasNull() ? null : result;
  }

  @Override
  public Long getResult(ResultSet rs, int columnIndex) throws SQLException {
    long result = rs.getLong(columnIndex);
    return result == 0L && rs.wasNull() ? null : result;
  }

  @Override
  public Long getResult(CallableStatement cs, int columnIndex) throws SQLException {
    long result = cs.getLong(columnIndex);
    return result == 0L && cs.wasNull() ? null : result;
  }
}
