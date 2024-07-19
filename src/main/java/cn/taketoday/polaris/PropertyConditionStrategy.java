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

import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.sql.Restriction;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/24 23:58
 */
public interface PropertyConditionStrategy {

  @Nullable
  Condition resolve(EntityProperty entityProperty, Object propertyValue);

  class Condition implements Restriction {

    public final Object propertyValue;

    public final Restriction restriction;

    public final EntityProperty entityProperty;

    public Condition(Object propertyValue, Restriction restriction, EntityProperty entityProperty) {
      this.propertyValue = propertyValue;
      this.restriction = restriction;
      this.entityProperty = entityProperty;
    }

    /**
     * Render the restriction into the SQL buffer
     */
    @Override
    public void render(StringBuilder sqlBuffer) {
      restriction.render(sqlBuffer);
    }

    /**
     * <p>Sets the value of the designated parameter using the given object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @return Returns next parameterIndex
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     * this method is called on a closed {@code PreparedStatement}
     * or the type of the given object is ambiguous
     */
    public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
      entityProperty.setParameter(ps, parameterIndex++, propertyValue);
      return parameterIndex;
    }

  }
}
