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

package cn.taketoday.polaris.query.parsing.ast;

import cn.taketoday.polaris.query.parsing.ExpressionVisitor;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/28 15:44
 */
public class GroupByExpression implements Expression {

  public final ExpressionList groupByExpressions;

  public final boolean withRollup;

  public GroupByExpression(ExpressionList groupByExpressions, boolean withRollup) {
    this.groupByExpressions = groupByExpressions;
    this.withRollup = withRollup;
  }

  @Override
  public String toString() {
    if (withRollup) {
      return "GROUP BY " + groupByExpressions + " WITH ROLLUP";
    }
    return "GROUP BY " + groupByExpressions;
  }

  @Override
  public void accept(ExpressionVisitor visitor) {
    visitor.visit(this);
  }

}
