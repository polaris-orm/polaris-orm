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

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/9/11 21:49
 */
public class NestedQueryCondition extends QueryCondition {

  private final QueryCondition chain;

  public NestedQueryCondition(QueryCondition chain) {
    this.chain = chain;
  }

  @Override
  protected boolean matches() {
    return chain != null;
  }

  @Override
  protected int setParameter(PreparedStatement ps, int idx) throws SQLException {
    int nextIdx = chain.setParameter(ps, idx);
    if (nextNode != null) {
      nextIdx = nextNode.setParameter(ps, nextIdx);
    }
    return nextIdx;
  }

  @Override
  protected int setParameterInternal(PreparedStatement ps, int idx) throws SQLException {
    int nextIdx = chain.setParameterInternal(ps, idx);
    if (nextNode != null) {
      nextIdx = nextNode.setParameterInternal(ps, nextIdx);
    }
    return nextIdx;
  }

  @Override
  protected void renderInternal(StringBuilder sql) {
    sql.append(' ');
    sql.append('(');

    chain.render(sql);

    sql.append(' ');
    sql.append(')');
  }

}
