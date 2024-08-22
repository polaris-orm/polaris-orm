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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/21 17:33
 */
public class LikeExpression extends BinaryExpression implements Expression {

  private final boolean not;

  private boolean useBinary = false;

  public LikeExpression(Expression leftExpression, boolean not, Expression rightExpression) {
    super(leftExpression, rightExpression);
    this.not = not;
  }

  @Override
  public String getStringExpression() {
    return "LIKE";
  }

  public void setUseBinary(boolean useBinary) {
    this.useBinary = useBinary;
  }

  @Override
  public String toString() {
    return leftExpression + " " + (not ? "NOT " : "")
            + (useBinary ? "BINARY " : "") + "LIKE " + rightExpression;
  }

}