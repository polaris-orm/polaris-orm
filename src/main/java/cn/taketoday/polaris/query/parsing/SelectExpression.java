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

package cn.taketoday.polaris.query.parsing;

import cn.taketoday.polaris.StatementSequence;
import cn.taketoday.polaris.platform.Platform;
import cn.taketoday.polaris.query.parsing.ast.Expression;
import cn.taketoday.polaris.query.parsing.ast.GroupByExpression;
import cn.taketoday.polaris.query.parsing.ast.HavingExpression;
import cn.taketoday.polaris.query.parsing.ast.WhereExpression;
import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 22:26
 */
public class SelectExpression implements Expression, StatementSequence {

  public final String select;

  @Nullable
  public final WhereExpression where;

  @Nullable
  public final GroupByExpression groupBy;

  @Nullable
  public final HavingExpression having;

  @Nullable
  public final String other;

  public SelectExpression(String select, @Nullable WhereExpression where,
          @Nullable GroupByExpression groupBy, @Nullable HavingExpression having, @Nullable String other) {
    this.select = select;
    this.where = where;
    this.groupBy = groupBy;
    this.having = having;
    this.other = other;
  }

  @Override
  public String toString() {
    return render();
  }

  public String getSelect() {
    return select;
  }

  @Nullable
  public WhereExpression getWhere() {
    return where;
  }

  @Nullable
  public String getOther() {
    return other;
  }

  public String render() {
    StringBuilder builder = new StringBuilder();
    render(builder);
    return builder.toString();
  }

  public void render(StringBuilder selectSQL) {
    selectSQL.append(select);
    if (where != null) {
      appendSeparator(selectSQL);
      where.render(selectSQL);
    }
    if (groupBy != null) {
      appendSeparator(selectSQL);
      selectSQL.append(groupBy);
    }
    if (having != null) {
      appendSeparator(selectSQL);
      selectSQL.append(having);
    }
    if (other != null && !other.isEmpty()) {
      appendSeparator(selectSQL);
      selectSQL.append(other);
    }
  }

  private static void appendSeparator(StringBuilder builder) {
    if (!builder.isEmpty() && !Character.isWhitespace(builder.codePointBefore(builder.length()))) {
      builder.append(' ');
    }
  }

  @Override
  public void accept(ExpressionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toStatementString(Platform platform) {
    return toString();
  }

}
