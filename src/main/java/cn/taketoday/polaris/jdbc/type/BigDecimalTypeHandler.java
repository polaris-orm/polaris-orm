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

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public class BigDecimalTypeHandler extends BaseTypeHandler<BigDecimal> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, BigDecimal parameter) throws SQLException {
    ps.setBigDecimal(i, parameter);
  }

  @Override
  public BigDecimal getResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getBigDecimal(columnName);
  }

  @Override
  public BigDecimal getResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getBigDecimal(columnIndex);
  }

  @Override
  public BigDecimal getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getBigDecimal(columnIndex);
  }
}
