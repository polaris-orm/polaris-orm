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

package cn.taketoday.polaris.sql;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.Pair;
import cn.taketoday.lang.Assert;
import cn.taketoday.polaris.Order;
import cn.taketoday.util.StringUtils;

/**
 * OrderBy Clause
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 12:39
 */
public interface OrderByClause {

  CharSequence toClause();

  boolean isEmpty();

  // Static Factory Methods

  static cn.taketoday.polaris.sql.MutableOrderByClause forMap(Map<String, Order> sortKeys) {
    cn.taketoday.polaris.sql.MutableOrderByClause clause = new cn.taketoday.polaris.sql.MutableOrderByClause(sortKeys.size());
    for (Map.Entry<String, Order> entry : sortKeys.entrySet()) {
      clause.orderBy(entry.getKey(), entry.getValue());
    }
    return clause;
  }

  @SafeVarargs
  static cn.taketoday.polaris.sql.MutableOrderByClause valueOf(Pair<String, Order>... sortKeys) {
    Assert.notNull(sortKeys, "sortKeys is required");
    return new cn.taketoday.polaris.sql.MutableOrderByClause(List.of(sortKeys));
  }

  static OrderByClause plain(CharSequence sequence) {
    return new Plain(sequence);
  }

  static cn.taketoday.polaris.sql.MutableOrderByClause mutable() {
    return new MutableOrderByClause();
  }

  /**
   * Plain
   */
  class Plain implements OrderByClause {

    final CharSequence sequence;

    Plain(CharSequence sequence) {
      this.sequence = sequence;
    }

    @Override
    public CharSequence toClause() {
      return sequence;
    }

    @Override
    public boolean isEmpty() {
      return StringUtils.isBlank(sequence);
    }

  }

}
