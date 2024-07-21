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

import cn.taketoday.polaris.sql.Select;

/**
 * Render select columns from table
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 18:10
 */
public abstract class ColumnsQueryStatement implements QueryStatement {

  @Override
  public StatementSequence render(EntityMetadata metadata) {
    Select select = new Select();
    StringBuilder selectClause = new StringBuilder();

    boolean first = true;
    for (EntityProperty property : metadata.entityProperties) {
      if (first) {
        first = false;
        selectClause.append('`');
      }
      else {
        selectClause.append(", `");
      }
      selectClause.append(property.columnName)
              .append('`');
    }

    select.setSelectClause(selectClause);
    select.setFromClause(metadata.tableName);

    renderInternal(metadata, select);
    return select;
  }

  protected abstract void renderInternal(EntityMetadata metadata, Select select);

}
