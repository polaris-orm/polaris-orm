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

package cn.taketoday.polaris.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import cn.taketoday.polaris.jdbc.ParameterBinder;

/**
 * optimized Query-Parameter resolving
 *
 * @author TODAY 2021/8/22 10:15
 * @since 4.0
 */
public final class QueryParameter {
  private final String name;

  private ParameterBinder setter;
  private ParameterIndexHolder applier;

  public QueryParameter(String name, ParameterIndexHolder indexHolder) {
    this.name = name;
    this.applier = indexHolder;
  }

  /**
   * set value to given statement
   *
   * @param statement statement
   * @throws SQLException any parameter setting error
   */
  public void setTo(final PreparedStatement statement) throws SQLException {
    if (setter != null) {
      applier.bind(setter, statement);
    }
  }

  public void setHolder(ParameterIndexHolder applier) {
    this.applier = applier;
  }

  public void setSetter(ParameterBinder setter) {
    this.setter = setter;
  }

  public ParameterIndexHolder getHolder() {
    return applier;
  }

  public ParameterBinder getBinder() {
    return setter;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final QueryParameter parameter))
      return false;
    return Objects.equals(name, parameter.name)
            && Objects.equals(setter, parameter.setter)
            && Objects.equals(applier, parameter.applier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, setter, applier);
  }

  @Override
  public String toString() {
    return "QueryParameter: '" + name + "' setter: " + setter;
  }

}
