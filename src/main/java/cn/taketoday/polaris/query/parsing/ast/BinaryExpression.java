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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/21 15:59
 */
public abstract class BinaryExpression implements Expression {

  protected Expression leftExpression;

  protected Expression rightExpression;

  public BinaryExpression() {

  }

  public BinaryExpression(Expression leftExpression, Expression rightExpression) {
    this.leftExpression = leftExpression;
    this.rightExpression = rightExpression;
  }

  public Expression getLeftExpression() {
    return leftExpression;
  }

  public void setLeftExpression(Expression expression) {
    leftExpression = expression;
  }

  public Expression getRightExpression() {
    return rightExpression;
  }

  public void setRightExpression(Expression expression) {
    rightExpression = expression;
  }

  public abstract String getStringExpression();

  @Override
  public String toString() {
    return // (not ? "NOT " : "") +
            getLeftExpression() + " " + getStringExpression() + " " + getRightExpression();
  }

  public static Expression build(Class<? extends BinaryExpression> clz, Expression... expressions)
          throws NoSuchMethodException, InvocationTargetException, InstantiationException,
          IllegalAccessException {
    switch (expressions.length) {
      case 0:
        return null;
      case 1:
        return expressions[0];
      default:
        Iterator<Expression> it = Arrays.stream(expressions).iterator();

        Expression leftExpression = it.next();
        Expression rightExpression = it.next();
        BinaryExpression binaryExpression =
                clz.getConstructor(Expression.class, Expression.class)
                        .newInstance(leftExpression, rightExpression);

        while (it.hasNext()) {
          rightExpression = it.next();
          binaryExpression = clz.getConstructor(Expression.class, Expression.class)
                  .newInstance(binaryExpression, rightExpression);
        }
        return binaryExpression;
    }
  }

}
