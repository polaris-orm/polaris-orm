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
 * @since 1.0 2024/8/21 16:01
 */
public class Between implements Expression {

  public final Expression leftExpression;

  public final boolean not;

  public final Expression start;

  public final Expression end;

  public Between(Expression leftExpression, boolean not, Expression start, Expression end) {
    this.leftExpression = leftExpression;
    this.not = not;
    this.start = start;
    this.end = end;
  }

  @Override
  public String toString() {
    return leftExpression + " " + (not ? "NOT " : "") + "BETWEEN " + start
            + " AND " + end;
  }

  @Override
  public void accept(ExpressionVisitor visitor) {
    visitor.visit(this);
  }

}
