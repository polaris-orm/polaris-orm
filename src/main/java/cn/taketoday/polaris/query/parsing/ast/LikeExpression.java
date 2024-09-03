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
import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/21 17:33
 */
public class LikeExpression extends BinaryExpression implements Expression {

  public final boolean not;

  public final boolean binary;

  public final String type;

  @Nullable
  public final Expression escape;

  public LikeExpression(Expression leftExpression, boolean not,
          Expression rightExpression, boolean binary, String type, @Nullable Expression escape) {
    super(leftExpression, rightExpression);
    this.not = not;
    this.binary = binary;
    this.type = type;
    this.escape = escape;
  }

  @Override
  public String getStringExpression() {
    return type;
  }

  @Override
  public String toString() {
    return leftExpression + " " + (not ? "NOT " : "")
            + type + " " + (binary ? "BINARY " : "") + rightExpression
            + (escape != null ? " ESCAPE " + escape : "");
  }

  @Override
  public void accept(ExpressionVisitor visitor) {
    visitor.visit(this);
  }

}
