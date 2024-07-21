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
import java.util.List;

import cn.taketoday.logging.LogMessage;
import cn.taketoday.polaris.sql.OrderByClause;
import cn.taketoday.polaris.sql.Restriction;
import cn.taketoday.polaris.sql.Select;

/**
 * resolving {@link OrderByClause} from entity
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/20 17:03
 */
final class NoConditionsQuery extends ColumnsQueryStatement implements ConditionStatement, DebugDescriptive {

  static final NoConditionsQuery instance = new NoConditionsQuery();

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    // noop
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    // noop
  }

  @Override
  public String getDescription() {
    return "Query entities without conditions";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format(getDescription());
  }

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    // noop
  }

}
