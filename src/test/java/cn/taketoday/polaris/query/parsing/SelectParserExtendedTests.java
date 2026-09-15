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

import java.util.List;

import org.junit.jupiter.api.Test;

import cn.taketoday.polaris.query.parsing.ast.AndExpression;
import cn.taketoday.polaris.query.parsing.ast.ArithmeticExpression;
import cn.taketoday.polaris.query.parsing.ast.Between;
import cn.taketoday.polaris.query.parsing.ast.ColumnExpression;
import cn.taketoday.polaris.query.parsing.ast.ComparisonExpression;
import cn.taketoday.polaris.query.parsing.ast.ExistsExpression;
import cn.taketoday.polaris.query.parsing.ast.Expression;
import cn.taketoday.polaris.query.parsing.ast.ExpressionList;
import cn.taketoday.polaris.query.parsing.ast.FunctionExpression;
import cn.taketoday.polaris.query.parsing.ast.HashParameter;
import cn.taketoday.polaris.query.parsing.ast.InExpression;
import cn.taketoday.polaris.query.parsing.ast.IndexParameter;
import cn.taketoday.polaris.query.parsing.ast.IsNullExpression;
import cn.taketoday.polaris.query.parsing.ast.LikeExpression;
import cn.taketoday.polaris.query.parsing.ast.LiteralExpression;
import cn.taketoday.polaris.query.parsing.ast.NamedParameter;
import cn.taketoday.polaris.query.parsing.ast.NotExpression;
import cn.taketoday.polaris.query.parsing.ast.OrExpression;
import cn.taketoday.polaris.query.parsing.ast.ParenExpression;
import cn.taketoday.polaris.query.parsing.ast.UnaryExpression;
import cn.taketoday.polaris.query.parsing.ast.VariableRef;
import cn.taketoday.polaris.query.parsing.ast.XorExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Extended tests covering operator variants, precedence, literals, parameters,
 * subqueries, clause preservation and error handling.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0
 */
class SelectParserExtendedTests {

  // ---------- precedence ----------

  @Test
  void andBindsTighterThanOr() {
    SelectExpression expression = SelectParser.parse("select * from t where a = 1 or b = 2 and c = 3");

    assertThat(expression.where.expression).isInstanceOf(OrExpression.class);
    OrExpression or = (OrExpression) expression.where.expression;
    assertThat(or.leftExpression).isInstanceOf(ComparisonExpression.class);
    assertThat(or.rightExpression).isInstanceOf(AndExpression.class);
  }

  @Test
  void xorBindsTighterThanOr() {
    SelectExpression expression = SelectParser.parse("select * from t where a = 1 or c = 3 xor d = 4");

    OrExpression or = (OrExpression) expression.where.expression;
    assertThat(or.rightExpression).isInstanceOf(XorExpression.class);
    XorExpression xor = (XorExpression) or.rightExpression;
    assertThat(xor.leftExpression.toString()).isEqualTo("c = 3");
    assertThat(xor.rightExpression.toString()).isEqualTo("d = 4");
  }

  @Test
  void parenChangesPrecedence() {
    SelectExpression expression = SelectParser.parse("select * from t where (a = 1 or b = 2) and c = 3");

    assertThat(expression.where.expression).isInstanceOf(AndExpression.class);
    AndExpression and = (AndExpression) expression.where.expression;
    assertThat(and.leftExpression).isInstanceOf(ParenExpression.class);
    ParenExpression paren = (ParenExpression) and.leftExpression;
    assertThat(paren.expression).isInstanceOf(OrExpression.class);
  }

  @Test
  void notBindsLooserThanComparison() {
    SelectExpression expression = SelectParser.parse("select * from t where not a = 1 and b = 2");

    assertThat(expression.where.expression).isInstanceOf(AndExpression.class);
    AndExpression and = (AndExpression) expression.where.expression;
    assertThat(and.leftExpression).isInstanceOf(NotExpression.class);
    NotExpression not = (NotExpression) and.leftExpression;
    assertThat(not.expression).isInstanceOf(ComparisonExpression.class);
  }

  @Test
  void doubleNegation() {
    SelectExpression expression = SelectParser.parse("select * from t where not not a");

    NotExpression not = (NotExpression) expression.where.expression;
    assertThat(not.expression).isInstanceOf(NotExpression.class);
  }

  @Test
  void arithmeticPrecedence() {
    SelectExpression expression = SelectParser.parse("select * from t where a * b + 1 = c");

    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    assertThat(comparison.leftExpression).isInstanceOf(ArithmeticExpression.class);

    // (a * b) + 1
    ArithmeticExpression add = (ArithmeticExpression) comparison.leftExpression;
    assertThat(add.operator).isEqualTo("+");
    assertThat(add.leftExpression).isInstanceOf(ArithmeticExpression.class);
    ArithmeticExpression multiply = (ArithmeticExpression) add.leftExpression;
    assertThat(multiply.operator).isEqualTo("*");
  }

  @Test
  void parenArithmetic() {
    SelectExpression expression = SelectParser.parse("select * from t where (1 + 2) * 3 = 9");

    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    ArithmeticExpression multiply = (ArithmeticExpression) comparison.leftExpression;
    assertThat(multiply.operator).isEqualTo("*");
    assertThat(multiply.leftExpression).isInstanceOf(ParenExpression.class);

    ParenExpression paren = (ParenExpression) multiply.leftExpression;
    assertThat(paren.expression).isInstanceOf(ArithmeticExpression.class);
  }

  @Test
  void unaryMinusComparisonOnLeft() {
    SelectExpression expression = SelectParser.parse("select * from t where -1 < a");

    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    assertThat(comparison.operator).isEqualTo("<");
    assertThat(comparison.leftExpression).isInstanceOf(UnaryExpression.class);
    UnaryExpression unary = (UnaryExpression) comparison.leftExpression;
    assertThat(unary.operator).isEqualTo("-");
    assertThat(unary.expression.toString()).isEqualTo("1");
  }

  @Test
  void moduloAndDivision() {
    SelectExpression expression = SelectParser.parse("select * from t where a % 2 = 0 and b / 2 = 3");

    AndExpression and = (AndExpression) expression.where.expression;

    ComparisonExpression left = (ComparisonExpression) and.leftExpression;
    ArithmeticExpression modulo = (ArithmeticExpression) left.leftExpression;
    assertThat(modulo.operator).isEqualTo("%");

    ComparisonExpression right = (ComparisonExpression) and.rightExpression;
    ArithmeticExpression division = (ArithmeticExpression) right.leftExpression;
    assertThat(division.operator).isEqualTo("/");
  }

  // ---------- operators ----------

  @Test
  void comparisonOperators() {
    assertThat(operatorOf("select * from t where a < 1")).isEqualTo("<");
    assertThat(operatorOf("select * from t where a <= 1")).isEqualTo("<=");
    assertThat(operatorOf("select * from t where a > 1")).isEqualTo(">");
    assertThat(operatorOf("select * from t where a >= 1")).isEqualTo(">=");
    assertThat(operatorOf("select * from t where a <> 1")).isEqualTo("<>");
    assertThat(operatorOf("select * from t where a != 1")).isEqualTo("<>");
    assertThat(operatorOf("select * from t where a = 1")).isEqualTo("=");
    assertThat(operatorOf("select * from t where a <=> b")).isEqualTo("<=>");
  }

  private static String operatorOf(String sql) {
    SelectExpression expression = SelectParser.parse(sql);
    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    return comparison.operator;
  }

  @Test
  void likeVariants() {
    SelectExpression expression = SelectParser.parse("select * from t where a like 'x%'");
    LikeExpression like = (LikeExpression) expression.where.expression;
    assertThat(like.type).isEqualTo("like");
    assertThat(like.not).isFalse();
    assertThat(like.binary).isFalse();
    assertThat(like.escape).isNull();
  }

  @Test
  void notLikeBinary() {
    SelectExpression expression = SelectParser.parse("select * from t where a not like binary 'x%'");
    LikeExpression like = (LikeExpression) expression.where.expression;
    assertThat(like.not).isTrue();
    assertThat(like.binary).isTrue();
  }

  @Test
  void rlikeAndRegexp() {
    assertThat(typeOfLike("select * from t where a rlike '^x'")).isEqualTo("rlike");
    assertThat(typeOfLike("select * from t where a regexp '^x'")).isEqualTo("regexp");
    assertThat(typeOfLike("select * from t where a not regexp '^x'")).isEqualTo("regexp");
  }

  private static String typeOfLike(String sql) {
    LikeExpression like = (LikeExpression) SelectParser.parse(sql).where.expression;
    return like.type;
  }

  @Test
  void likeEscape() {
    SelectExpression expression = SelectParser.parse("select * from t where a like '/%/_%_' escape '/'");
    LikeExpression like = (LikeExpression) expression.where.expression;
    assertThat(like.escape).isNotNull();
    assertThat(like.escape.toString()).isEqualTo("'/'");
  }

  @Test
  void notBetween() {
    SelectExpression expression = SelectParser.parse("select * from t where a not between 1 and 2");
    Between between = (Between) expression.where.expression;
    assertThat(between.not).isTrue();
    assertThat(between.start.toString()).isEqualTo("1");
    assertThat(between.end.toString()).isEqualTo("2");
  }

  @Test
  void notIn() {
    SelectExpression expression = SelectParser.parse("select * from t where a not in (1, 2)");
    InExpression in = (InExpression) expression.where.expression;
    assertThat(in.not).isTrue();
    ExpressionList list = (ExpressionList) ((ParenExpression) in.parenExpression).expression;
    assertThat(list.expressions).hasSize(2);
  }

  @Test
  void inSubquery() {
    SelectExpression expression = SelectParser.parse("select * from t where a in (select id from u)");
    InExpression in = (InExpression) expression.where.expression;
    ParenExpression paren = (ParenExpression) in.parenExpression;
    ExpressionList list = (ExpressionList) paren.expression;
    assertThat(list.expressions).hasSize(1);
    assertThat(list.expressions.get(0)).isInstanceOf(SelectExpression.class);
  }

  @Test
  void isNullVariants() {
    assertThat(SelectParser.parse("select * from t where a is null").where.expression)
            .isInstanceOf(IsNullExpression.class);
    IsNullExpression notNull = (IsNullExpression) SelectParser.parse("select * from t where a is not null").where.expression;
    assertThat(notNull.not).isTrue();
  }

  @Test
  void notExists() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where not exists (select 1 from u where u.id = t.id)");

    assertThat(expression.where.expression).isInstanceOf(NotExpression.class);
    NotExpression not = (NotExpression) expression.where.expression;
    assertThat(not.expression).isInstanceOf(ExistsExpression.class);
  }

  @Test
  void existsToStringHasParens() {
    ExistsExpression exists = (ExistsExpression) SelectParser
            .parse("select * from t where exists (select 1 from u)").where.expression;
    assertThat(exists.toString()).isEqualTo("EXISTS (select 1 from u)");
  }

  // ---------- subqueries ----------

  @Test
  void comparisonWithSubquery() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where a = (select max(b) from u where c = 1)");

    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    ParenExpression paren = (ParenExpression) comparison.rightExpression;
    SelectExpression subquery = (SelectExpression) paren.expression;
    assertThat(subquery.where.expression).isInstanceOf(ComparisonExpression.class);
  }

  @Test
  void nestedSubqueries() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where a = (select 1 where b = (select 2 where c = (select 3)))");

    ParenExpression paren = (ParenExpression) ((ComparisonExpression) expression.where.expression).rightExpression;
    SelectExpression first = (SelectExpression) paren.expression;
    ParenExpression innerParen = (ParenExpression) ((ComparisonExpression) first.where.expression).rightExpression;
    SelectExpression second = (SelectExpression) innerParen.expression;
    ParenExpression innermostParen = (ParenExpression) ((ComparisonExpression) second.where.expression).rightExpression;
    SelectExpression third = (SelectExpression) innermostParen.expression;

    assertThat(first.where).isNotNull();
    assertThat(second.where).isNotNull();
    assertThat(third.where).isNull();
    assertThat(third.select).isEqualTo("select 3");
  }

  @Test
  void subqueryWithGroupHavingOrder() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where a in (select b from u group by b having count(*) > 1 order by b)");

    InExpression in = (InExpression) expression.where.expression;
    ExpressionList list = (ExpressionList) ((ParenExpression) in.parenExpression).expression;
    SelectExpression subquery = (SelectExpression) list.expressions.get(0);

    assertThat(subquery.groupBy).isNotNull();
    assertThat(subquery.having).isNotNull();
    assertThat(subquery.other).isEqualTo("order by b");
  }

  @Test
  void deepExistsInsideSubquery() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where x = (select max(y) from u where u.id = t.id and "
                    + "exists (select 1 from v where v.x = u.x))");

    ParenExpression paren = (ParenExpression) ((ComparisonExpression) expression.where.expression).rightExpression;
    SelectExpression subquery = (SelectExpression) paren.expression;
    assertThat(subquery.where.expression).isInstanceOf(AndExpression.class);

    AndExpression and = (AndExpression) subquery.where.expression;
    assertThat(and.rightExpression).isInstanceOf(ExistsExpression.class);
  }

  // ---------- parameters & literals ----------

  @Test
  void parameterKinds() {
    SelectExpression expression = SelectParser.parse(
            "select * from t where a in (#h, @v[1], ?, :p)");

    InExpression in = (InExpression) expression.where.expression;
    ExpressionList list = (ExpressionList) ((ParenExpression) in.parenExpression).expression;

    assertThat(list.expressions.get(0)).isInstanceOf(HashParameter.class);

    assertThat(list.expressions.get(1)).isInstanceOf(VariableRef.class);
    assertThat(((VariableRef) list.expressions.get(1)).arrayIndex).isEqualTo(1);

    assertThat(list.expressions.get(2)).isInstanceOf(IndexParameter.class);

    assertThat(list.expressions.get(3)).isInstanceOf(NamedParameter.class);
    assertThat(((NamedParameter) list.expressions.get(3)).name).isEqualTo("p");
  }

  @Test
  void arrayIndexParameter() {
    SelectExpression expression = SelectParser.parse("select * from t where a = :p[0]");
    ComparisonExpression comparison = (ComparisonExpression) expression.where.expression;
    NamedParameter parameter = (NamedParameter) comparison.rightExpression;
    assertThat(parameter.name).isEqualTo("p");
    assertThat(parameter.arrayIndex).isZero();
  }

  @Test
  void literalKinds() {
    for (String literal : List.of("17L", "1.5e3", "0xFF", "1.5f", "1.5d")) {
      SelectExpression expression = SelectParser.parse("select * from t where a = " + literal);
      LiteralExpression pure = (LiteralExpression) ((ComparisonExpression) expression.where.expression).rightExpression;
      assertThat(pure).as(literal).isInstanceOf(LiteralExpression.class);
    }
  }

  @Test
  void booleanAndNullLiterals() {
    assertThat(rightLiteralOf("select * from t where a = true")).isEqualTo("true");
    assertThat(rightLiteralOf("select * from t where a = false")).isEqualTo("false");
    assertThat(rightLiteralOf("select * from t where a = null")).isEqualTo("null");
  }

  private static String rightLiteralOf(String sql) {
    return ((LiteralExpression) ((ComparisonExpression) SelectParser.parse(sql).where.expression).rightExpression).value;
  }

  @Test
  void escapedStringLiteralKeepsQuotes() {
    SelectExpression expression = SelectParser.parse("select * from t where a = 'it''s'");
    LiteralExpression literal = (LiteralExpression) ((ComparisonExpression) expression.where.expression).rightExpression;
    assertThat(literal.value).isEqualTo("'it''s'");
  }

  @Test
  void quotedReservedWordAsColumn() {
    SelectExpression expression = SelectParser.parse("select * from t where `select` = 1");
    ColumnExpression column = (ColumnExpression) ((ComparisonExpression) expression.where.expression).leftExpression;
    assertThat(column.name).isEqualTo("`select`");
  }

  @Test
  void keywordInsideStringLiteral() {
    SelectExpression expression = SelectParser.parse("select * from t where a = 'order by'");
    assertThat(expression.where.expression).isInstanceOf(ComparisonExpression.class);
    assertThat(expression.getOther()).isNull();
  }

  // ---------- case insensitivity ----------

  @Test
  void caseInsensitiveKeywords() {
    SelectExpression expression = SelectParser.parse(
            "SELECT * FROM T WHERE A = 1 GROUP BY X HAVING COUNT(*) > 0 ORDER BY X LIMIT 10");

    assertThat(expression.getWhere()).isNotNull();
    assertThat(expression.groupBy).isNotNull();
    assertThat(expression.having).isNotNull();
    assertThat(expression.getOther()).isEqualTo("ORDER BY X LIMIT 10");
  }

  @Test
  void mixedCaseOperators() {
    SelectExpression expression = SelectParser.parse(
            "select * from t wHeRe a iN (1, 2) AnD b LiKe 'x' xOr c = 3");

    // XOR binds tighter than AND:  a IN (1, 2) AND (b LIKE 'x' XOR c = 3)
    assertThat(expression.where.expression).isInstanceOf(AndExpression.class);
    AndExpression and = (AndExpression) expression.where.expression;
    assertThat(and.rightExpression).isInstanceOf(XorExpression.class);
    XorExpression xor = (XorExpression) and.rightExpression;
    assertThat(xor.leftExpression.toString()).isEqualToIgnoringCase("b LIKE 'x'");
    assertThat(xor.rightExpression.toString()).isEqualTo("c = 3");
  }

  // ---------- group by / having ----------

  @Test
  void groupByMultipleWithRollup() {
    SelectExpression expression = SelectParser.parse(
            "select a, b from t group by a, b with rollup");

    assertThat(expression.groupBy.withRollup).isTrue();
    assertThat(expression.groupBy.groupByExpressions.expressions).hasSize(2);
  }

  @Test
  void groupByFunction() {
    SelectExpression expression = SelectParser.parse(
            "select * from t group by substr(name, 1, 2)");

    Expression groupExpression = expression.groupBy.groupByExpressions.expressions.get(0);
    assertThat(groupExpression).isInstanceOf(FunctionExpression.class);
    FunctionExpression function = (FunctionExpression) groupExpression;
    assertThat(function.name).isEqualTo("substr");
  }

  @Test
  void havingWithoutGroupBy() {
    SelectExpression expression = SelectParser.parse(
            "select a, count(*) from t having count(*) > 2");

    assertThat(expression.groupBy).isNull();
    assertThat(expression.having).isNotNull();
  }

  // ---------- clause preservation ----------

  @Test
  void unionPreserved() {
    SelectExpression expression = SelectParser.parse(
            "select * from a where x = 1 union select * from b");

    assertThat(expression.getOther()).isEqualTo("union select * from b");
  }

  @Test
  void limitOffsetWithoutWhere() {
    SelectExpression expression = SelectParser.parse("select * from t limit 10 offset 5");
    assertThat(expression.getSelect()).isEqualTo("select * from t ");
    assertThat(expression.getWhere()).isNull();
    assertThat(expression.getOther()).isEqualTo("limit 10 offset 5");
  }

  // ---------- error handling ----------

  @Test
  void missingOperand() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a ="))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Unexpectedly ran out of input");
  }

  @Test
  void danglingAnd() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a = 1 and"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Problem parsing right operand");
  }

  @Test
  void unclosedParen() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where (a = 1"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Unexpectedly ran out of input");
  }

  @Test
  void betweenInvalid() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a between 1 or 2"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Expected 'and'");
  }

  @Test
  void isInvalidOperand() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a is 1"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [select * from t where a is 1] @27: Not a valid operator token: ''1''");
  }

  @Test
  void existsWithoutSubquery() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where exists (1)"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Subquery expected after EXISTS");
  }

  @Test
  void inWithoutParens() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a in 1"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("expected after IN");
  }

  @Test
  void danglingOperator() {
    assertThatThrownBy(() -> SelectParser.parse("select * from t where a +"))
            .isInstanceOf(ParsingException.class)
            .hasMessageContaining("Unexpectedly ran out of input");
  }

  // ---------- render round-trip ----------

  @Test
  void renderRoundTrip() {
    List<String> statements = List.of(
            "select * from t where a = 1",
            "select * from t where a = :p[0] and b = ?",
            "select * from t where (a = 1 or b = 2) and c = 3",
            "select * from t where a not between 1 and 2",
            "select * from t where a not in (1, 2, 3)",
            "select * from t where a like '/%/_%_' escape '/' and b not like 'n%'",
            "select * from t where a rlike '^x' and b regexp 'x$'",
            "select * from t where a is null or b is not null",
            "select * from t where flag is not true",
            "select * from t where not a = 1 and not (b = 2 or c = 3)",
            "select * from t where a = -1 and b = (1 + 2) * 3",
            "select * from t where a % 2 = 0",
            "select * from t where now() and trim(a) = 'x'",
            "select * from t where a in (select id from u)",
            "select * from t where a in ((select 1), 2, 3)",
            "select * from t where a = (select max(b) from u where c = 1)",
            "select * from t where exists (select 1 from u where u.id = t.id)",
            "select * from t where not exists (select 1 from u)",
            "select * from t order by a desc limit 10",
            "select * from t where a = 1 order by a limit 10 offset 5",
            "select a, count(*) from t group by a, b with rollup having count(*) > 1 order by a",
            "select a, count(*) from t group by substr(name, 1, 2) having sum(x) > 3",
            "select * from t where a = 1 union select * from u",
            "select * from t where x = (select max(y) from u where u.id = t.id and exists (select 1 from v where v.x = u.x))"
    );

    for (String sql : statements) {
      String rendered = SelectParser.parse(sql).render();
      // parsing the rendered statement must succeed and be stable
      String renderedAgain = SelectParser.parse(rendered).render();
      assertThat(renderedAgain).as("render round-trip for [%s]", sql).isEqualTo(rendered);
    }
  }

  @Test
  void renderPreservesColumns() {
    SelectExpression expression = SelectParser.parse(
            "select id, `name` from t where `type` = 1 and article.status = 'A'");
    assertThat(expression.render())
            .contains("`type` = 1")
            .contains("article.status = 'A'");
  }
}