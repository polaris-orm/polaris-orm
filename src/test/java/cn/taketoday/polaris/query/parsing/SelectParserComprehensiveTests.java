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

import org.junit.jupiter.api.Test;

import cn.taketoday.polaris.query.parsing.ast.AndExpression;
import cn.taketoday.polaris.query.parsing.ast.ArithmeticExpression;
import cn.taketoday.polaris.query.parsing.ast.ColumnExpression;
import cn.taketoday.polaris.query.parsing.ast.ComparisonExpression;
import cn.taketoday.polaris.query.parsing.ast.ExistsExpression;
import cn.taketoday.polaris.query.parsing.ast.ExpressionList;
import cn.taketoday.polaris.query.parsing.ast.FunctionExpression;
import cn.taketoday.polaris.query.parsing.ast.InExpression;
import cn.taketoday.polaris.query.parsing.ast.IsExpression;
import cn.taketoday.polaris.query.parsing.ast.NotExpression;
import cn.taketoday.polaris.query.parsing.ast.ParenExpression;
import cn.taketoday.polaris.query.parsing.ast.UnaryExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the extended condition grammar of {@link SelectParser}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0
 */
class SelectParserComprehensiveTests {

  @Test
  void prefixNot() {
    SelectExpression expression = SelectParser.parse("select * from t where not (a = 1)");
    assertThat(expression.where.expression).isInstanceOf(NotExpression.class);

    NotExpression not = (NotExpression) expression.where.expression;
    assertThat(not.expression).isInstanceOf(ParenExpression.class);

    ParenExpression paren = (ParenExpression) not.expression;
    assertThat(paren.expression).isInstanceOf(ComparisonExpression.class);
  }

  @Test
  void bangNot() {
    SelectExpression expression = SelectParser.parse("select * from t where !a");
    assertThat(expression.where.expression).isInstanceOf(NotExpression.class);
    NotExpression not = (NotExpression) expression.where.expression;
    assertThat(not.expression).isInstanceOf(ColumnExpression.class);
  }

  @Test
  void notEqualsAndNullSafeEquals() {
    SelectExpression expression = SelectParser.parse("select * from t where a != 1 and b <=> c");

    assertThat(expression.where.expression).isInstanceOf(AndExpression.class);
    AndExpression and = (AndExpression) expression.where.expression;

    ComparisonExpression left = (ComparisonExpression) and.leftExpression;
    assertThat(left.operator).isEqualTo("<>");

    ComparisonExpression right = (ComparisonExpression) and.rightExpression;
    assertThat(right.operator).isEqualTo("<=>");
    assertThat(right.rightExpression).isInstanceOf(ColumnExpression.class);
  }

  @Test
  void unaryMinus() {
    SelectExpression expression = SelectParser.parse("select * from t where a = -1");
    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    assertThat(comparison.rightExpression).isInstanceOf(UnaryExpression.class);

    UnaryExpression unary = (UnaryExpression) comparison.rightExpression;
    assertThat(unary.operator).isEqualTo("-");
  }

  @Test
  void arithmetic() {
    SelectExpression expression = SelectParser.parse("select * from t where a + 2 = b * 3");
    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;

    assertThat(comparison.leftExpression).isInstanceOf(ArithmeticExpression.class);
    ArithmeticExpression left = (ArithmeticExpression) comparison.leftExpression;
    assertThat(left.operator).isEqualTo("+");

    assertThat(comparison.rightExpression).isInstanceOf(ArithmeticExpression.class);
    ArithmeticExpression right = (ArithmeticExpression) comparison.rightExpression;
    assertThat(right.operator).isEqualTo("*");
  }

  @Test
  void isTrue() {
    SelectExpression expression = SelectParser.parse("select * from t where flag is not true");
    assertThat(expression.where.expression).isInstanceOf(IsExpression.class);

    IsExpression is = (IsExpression) expression.where.expression;
    assertThat(is.not).isTrue();
    assertThat(is.rightExpression.toString()).isEqualTo("true");
  }

  @Test
  void existsSubquery() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where exists (select 1 from u where u.id = t.id)");

    assertThat(expression.where.expression).isInstanceOf(ExistsExpression.class);
    ExistsExpression exists = (ExistsExpression) expression.where.expression;
    assertThat(exists.subquery).isInstanceOf(SelectExpression.class);

    SelectExpression subquery = (SelectExpression) exists.subquery;
    assertThat(subquery.where).isNotNull();
    assertThat(subquery.where.expression).isInstanceOf(ComparisonExpression.class);
  }

  @Test
  void nestedSubqueryWhere() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where a = (select max(x) from u where u.id = t.id)");

    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    assertThat(comparison.rightExpression).isInstanceOf(ParenExpression.class);

    ParenExpression paren = (ParenExpression) comparison.rightExpression;
    assertThat(paren.expression).isInstanceOf(SelectExpression.class);

    SelectExpression subquery = (SelectExpression) paren.expression;
    assertThat(subquery.where).isNotNull();
    assertThat(subquery.where.expression).isInstanceOf(ComparisonExpression.class);
  }

  @Test
  void emptyFunctionArguments() {
    SelectExpression expression = SelectParser.parse("select * from t where now()");
    assertThat(expression.where.expression).isInstanceOf(FunctionExpression.class);

    FunctionExpression function = (FunctionExpression) expression.where.expression;
    assertThat(function.name).isEqualTo("now");
    ParenExpression args = (ParenExpression) function.args;
    assertThat(((ExpressionList) args.expression).expressions).isEmpty();
  }

  @Test
  void emptyInList() {
    SelectExpression expression = SelectParser.parse("select * from t where a in ()");
    assertThat(expression.where.expression).isInstanceOf(InExpression.class);

    InExpression in = (InExpression) expression.where.expression;
    ParenExpression list = (ParenExpression) in.parenExpression;
    assertThat(((ExpressionList) list.expression).expressions).isEmpty();
  }

  @Test
  void betweenRequiresAnd() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a between 1 or 2"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Expected 'and'");
  }

  @Test
  void preserveTrailingClausesWithWhere() {
    SelectExpression expression = SelectParser.parse("select * from t where a = 1 order by a desc limit 10");

    assertThat(expression.getSelect()).isEqualTo("select * from t ");
    assertThat(expression.getWhere()).isNotNull();
    assertThat(expression.getOther()).isEqualTo("order by a desc limit 10");
    assertThat(expression.render()).endsWith("order by a desc limit 10");
  }

  @Test
  void preserveTrailingClausesWithoutWhere() {
    SelectExpression expression = SelectParser.parse("select * from t order by a limit 5");

    assertThat(expression.getSelect()).isEqualTo("select * from t ");
    assertThat(expression.getWhere()).isNull();
    assertThat(expression.getOther()).isEqualTo("order by a limit 5");
  }

  @Test
  void renderGroupByAndHaving() {
    SelectExpression expression = SelectParser.parse(
            "select a, count(*) from t group by a having count(*) > 1");

    assertThat(expression.groupBy).isNotNull();
    assertThat(expression.having).isNotNull();

    String render = expression.render();
    assertThat(render).contains("GROUP BY a");
    assertThat(render).contains("HAVING count(*) > 1");
  }

}