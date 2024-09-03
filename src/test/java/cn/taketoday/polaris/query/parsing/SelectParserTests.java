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
import cn.taketoday.polaris.query.parsing.ast.Between;
import cn.taketoday.polaris.query.parsing.ast.ColumnExpression;
import cn.taketoday.polaris.query.parsing.ast.ComparisonExpression;
import cn.taketoday.polaris.query.parsing.ast.Expression;
import cn.taketoday.polaris.query.parsing.ast.ExpressionList;
import cn.taketoday.polaris.query.parsing.ast.FunctionExpression;
import cn.taketoday.polaris.query.parsing.ast.GroupByExpression;
import cn.taketoday.polaris.query.parsing.ast.HashParameter;
import cn.taketoday.polaris.query.parsing.ast.HavingExpression;
import cn.taketoday.polaris.query.parsing.ast.InExpression;
import cn.taketoday.polaris.query.parsing.ast.IndexParameter;
import cn.taketoday.polaris.query.parsing.ast.IsNullExpression;
import cn.taketoday.polaris.query.parsing.ast.LikeExpression;
import cn.taketoday.polaris.query.parsing.ast.LiteralExpression;
import cn.taketoday.polaris.query.parsing.ast.NamedParameter;
import cn.taketoday.polaris.query.parsing.ast.OrExpression;
import cn.taketoday.polaris.query.parsing.ast.ParenExpression;
import cn.taketoday.polaris.query.parsing.ast.VariableRef;
import cn.taketoday.polaris.query.parsing.ast.WhereExpression;
import cn.taketoday.polaris.query.parsing.ast.XorExpression;
import cn.taketoday.polaris.util.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 23:06
 */
class SelectParserTests {

  @Test
  void test() {
    String sql = """
            SELECT `category`, `content`, `copyright`, `cover`, `create_at`, `id`, `markdown`, `password`, `pv`, `status`, `summary`, `title`, `update_at`, `uri`
            FROM article WHERE article.`category` = #category
            and (`title` like @q OR `content` like '%#q%' )
            and (status = :status) and 1=1
            and create_at between :create_at[0] and :create_at[1]
            or status is not null
            and status not like 's'
            and TRIM(status) = 'YHJ'
            or status not in (?, :status, 3, 4, '5', :`d`)
            or find_in_set(status, 'd')
            and status in(1, true, false)
            and status in(func(status, 'd'))
            and binary status = 1
            and status in ((select 1 where 1 = 1 order by col desc), 2)
            and status in (select 1, 2)
            and status in ((select 1), 2)
            and status in (1, (select col where (1 = 1) and (2 = 1 or 1 = 2) order by col desc), 2)
            and status in (binary find_in_set(status, 'd'), 2)
            and status in (binary status, 2)
            and status in (select 1 where col = 1 and 1 = 1)
            and status like binary '/%/_%_' ESCAPE '/'
            and status rlike binary '/%/_%_' ESCAPE '/'
            and status REGEXP binary '/%/_%_' ESCAPE '/'
            xor article.status = :status AND article_label.label_id IN (
                  SELECT label_id FROM article_label
                  WHERE label_id = (SELECT id FROM label WHERE name = #name)
            )
            xor status and status
            order by update_at DESC, create_at DESC LIMIT 20""";

    SelectExpression expression = SelectParser.parse(sql);

    String render = expression.render();
    System.out.println(render);

    ToStringVisitor visitor = new ToStringVisitor();
    expression.accept(visitor);

    System.out.println(visitor);
  }

  @Test
  void noWhere() {
    SelectExpression expression = SelectParser.parse("select * from article");
    assertThat(expression.getSelect()).isEqualTo("select * from article");
    assertThat(expression.getOther()).isNull();
    assertThat(expression.getWhere()).isNull();
  }

  @Test
  void noOther() {
    SelectExpression selectExpr = SelectParser.parse("select * from article where id = 1");
    assertThat(selectExpr.getSelect()).isEqualTo("select * from article ");
    assertThat(selectExpr.getOther()).isNull();
    WhereExpression where = selectExpr.getWhere();
    assertThat(where).isNotNull();
    assertThat(where.expression).isNotNull();

    assertThat(where.expression).isInstanceOf(ComparisonExpression.class);

    ComparisonExpression expression = (ComparisonExpression) where.expression;
    assertThat(expression.operator).isEqualTo("=");
    assertThat(expression.rightExpression).isInstanceOf(LiteralExpression.class);
    assertThat(expression.leftExpression).isInstanceOf(ColumnExpression.class);

    LiteralExpression leftExpression = (LiteralExpression) expression.rightExpression;
    assertThat(leftExpression.value).isEqualTo("1");

    ColumnExpression columnName = (ColumnExpression) expression.leftExpression;

    assertThat(columnName.name).isEqualTo("id");
    assertThat(columnName.dotName).isFalse();
    assertThat(columnName.binary).isFalse();
  }

  @Test
  void func() {
    SelectExpression selectExpr = SelectParser.parse("SELECT * FROM article WHERE trim(binary a.b)");
    assertThat(selectExpr.getSelect()).isEqualTo("SELECT * FROM article ");
    assertThat(selectExpr.getOther()).isNull();
    WhereExpression where = selectExpr.getWhere();

    assertThat(where).isNotNull();
    assertThat(where.expression).isNotNull();
    assertThat(where.expression).isInstanceOf(FunctionExpression.class);

    FunctionExpression functionExpr = (FunctionExpression) where.expression;
    assertThat(functionExpr.toString()).isEqualTo("trim(BINARY a.b)");

    assertThat(functionExpr.name).isEqualTo("trim");
    assertThat(functionExpr.args).isInstanceOf(ParenExpression.class);
    ParenExpression parenExpr = (ParenExpression) functionExpr.args;
    assertThat(parenExpr.expression).isInstanceOf(ExpressionList.class);
    ExpressionList expressionList = (ExpressionList) parenExpr.expression;
    assertThat(expressionList.expressions).hasSize(1);

    Expression arg = expressionList.expressions.get(0);
    assertThat(arg).isInstanceOf(ColumnExpression.class);

    ColumnExpression columnNameArg = (ColumnExpression) arg;
    assertThat(columnNameArg.name).isEqualTo("a.b");
    assertThat(columnNameArg.dotName).isTrue();
    assertThat(columnNameArg.binary).isTrue();
  }

  @Test
  void groupBy() {
    String query = "SELECT coalesce(name, '总数'), SUM(signin) as signin_count FROM employee group by name WITH ROLLUP";
    var expression = SelectParser.parse(query);

    assertThat(expression.groupBy).isNotNull();

    assertThat(expression.groupBy.withRollup).isTrue();
    assertThat(expression.groupBy.groupByExpressions.expressions).hasSize(1);
    assertThat(expression.groupBy.groupByExpressions.expressions.get(0)).isInstanceOf(ColumnExpression.class);
    ColumnExpression columnExpression = (ColumnExpression) expression.groupBy.groupByExpressions.expressions.get(0);

    assertThat(columnExpression.name).isEqualTo("name");
    assertThat(columnExpression.dotName).isFalse();
    assertThat(columnExpression.binary).isFalse();

    assertThat(expression.having).isNull();

    query = "SELECT coalesce(name, '总数'), SUM(signin) as signin_count FROM employee group by name WITH ROLLUP having sum(signin) = sum(signin)";
    expression = SelectParser.parse(query);

    assertThat(expression.groupBy).isNotNull();

    GroupByExpression groupBy = expression.groupBy;

    assertThat(groupBy.withRollup).isTrue();
    assertThat(groupBy.groupByExpressions.expressions).hasSize(1);
    assertThat(groupBy.groupByExpressions.expressions.get(0)).isInstanceOf(ColumnExpression.class);

    columnExpression = (ColumnExpression) groupBy.groupByExpressions.expressions.get(0);

    assertThat(columnExpression.name).isEqualTo("name");
    assertThat(columnExpression.dotName).isFalse();
    assertThat(columnExpression.binary).isFalse();

    HavingExpression having = expression.having;
    assertThat(having).isNotNull();
    assertThat(having.expression).isNotNull();
    assertThat(having.expression).isInstanceOf(ComparisonExpression.class);

    ComparisonExpression comparisonExpression = (ComparisonExpression) having.expression;
    assertThat(comparisonExpression.leftExpression).isInstanceOf(FunctionExpression.class);
    assertThat(comparisonExpression.operator).isEqualTo("=");
    assertThat(comparisonExpression.rightExpression).isInstanceOf(FunctionExpression.class);

  }

  @Test
  void whereNoOperator() {
    SelectExpression expression = SelectParser.parse("select * from article where 1");
    assertThat(expression.select).startsWith("select * from article");
    assertThat(expression.other).isNull();
    assertThat(expression.where).isNotNull();
    assertThat(expression.where.expression).isInstanceOf(LiteralExpression.class);
    LiteralExpression literalExpression = (LiteralExpression) expression.where.expression;
    assertThat(literalExpression.value).isEqualTo("1");

    expression = SelectParser.parse("select * from article where col");
    assertThat(expression.select).startsWith("select * from article");
    assertThat(expression.other).isNull();
    assertThat(expression.where).isNotNull();
    assertThat(expression.where.expression).isInstanceOf(ColumnExpression.class);
    ColumnExpression columnExpression = (ColumnExpression) expression.where.expression;
    assertThat(columnExpression.name).isEqualTo("col");
  }

  @Test
  void syntaxError() {
    assertThatThrownBy(() -> SelectParser.parse("Update article set status = 1 where status = 2"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [Update article set status = 1 where status = 2]: Not a select statement");

    assertThatThrownBy(() -> SelectParser.parse("SELECT * FROM article WHERE"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [SELECT * FROM article WHERE] @27: Where clause not found");

    assertThatThrownBy(() -> SelectParser.parse("SELECT * FROM article WHERE 1 = 1 >"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [SELECT * FROM article WHERE 1 = 1 >] @34: Syntax error");

    assertThatThrownBy(() -> SelectParser.parse("SELECT * FROM article WHERE 1 is c"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [SELECT * FROM article WHERE 1 is c] @33: Not a valid operator token: ''c''");

  }

  @Test
  void visitor() {
    ToStringVisitor visitor = new ToStringVisitor();
    var selectExpr = SelectParser.parse("select * from article where id = 1 and name = 'n' ");

    selectExpr.accept(visitor);

    System.out.println(visitor.buffer);
  }

  static class ToStringVisitor implements ExpressionVisitor {

    final StringBuilder buffer = new StringBuilder();

    @Override
    public String toString() {
      return buffer.toString();
    }

    @Override
    public void visit(Between between) {
      between.leftExpression.accept(this);
      if (between.not) {
        buffer.append(" NOT");
      }
      buffer.append(" BETWEEN");

      between.start.accept(this);
      buffer.append(" AND");
      between.end.accept(this);
    }

    @Override
    public void visit(ColumnExpression column) {
      if (column.binary) {
        buffer.append(" BINARY");
      }
      buffer.append(' ');
      buffer.append(column.name);
    }

    @Override
    public void visit(ComparisonExpression comparison) {
      comparison.leftExpression.accept(this);
      buffer.append(' ');
      buffer.append(comparison.operator);
      buffer.append(' ');
      comparison.rightExpression.accept(this);
    }

    @Override
    public void visit(String expression) {
      buffer.append(expression);
    }

    @Override
    public void visit(ExpressionList expressionList) {
      boolean comma = false;
      for (Expression expression : expressionList.expressions) {
        if (comma) {
          buffer.append(",");
        }
        else {
          comma = true;
        }
        expression.accept(this);
      }
    }

    @Override
    public void visit(FunctionExpression function) {
      if (function.binary) {
        buffer.append(" BINARY");
      }
      buffer.append(' ');
      buffer.append(function.name);
      function.args.accept(this);
    }

    @Override
    public void visit(GroupByExpression groupBy) {
      buffer.append(" GROUP BY");
      visit(groupBy.groupByExpressions);
      if (groupBy.withRollup) {
        buffer.append(" WITH ROLLUP");
      }
    }

    @Override
    public void visit(HavingExpression having) {
      buffer.append(" HAVING");
      having.expression.accept(this);
    }

    @Override
    public void visit(IndexParameter indexParameter) {
      buffer.append(" ?");
    }

    @Override
    public void visit(InExpression inExpression) {
      inExpression.leftExpression.accept(this);

      if (inExpression.not) {
        buffer.append(" NOT");
      }
      buffer.append(" IN");

      inExpression.parenExpression.accept(this);
    }

    @Override
    public void visit(IsNullExpression isNullExpression) {
      isNullExpression.leftExpression.accept(this);

      buffer.append(" IS");

      if (isNullExpression.not) {
        buffer.append(" NOT");
      }

      buffer.append(" NULL");
    }

    @Override
    public void visit(LikeExpression likeExpression) {
      likeExpression.leftExpression.accept(this);

      if (likeExpression.not) {
        buffer.append(" NOT");
      }
      buffer.append(likeExpression.type);

      if (likeExpression.binary) {
        buffer.append(" BINARY");
      }

      likeExpression.rightExpression.accept(this);

      if (likeExpression.escape != null) {
        buffer.append(" ESCAPE ");
        buffer.append(likeExpression.escape);
      }
    }

    @Override
    public void visit(LiteralExpression literal) {
      buffer.append(literal.value);
    }

    @Override
    public void visit(ParenExpression paren) {
      buffer.append(" (");
      paren.expression.accept(this);
      buffer.append(')');
    }

    @Override
    public void visit(HashParameter hash) {
      buffer.append(" #");
      buffer.append(hash.name);

      appendArrayIndex(hash.arrayIndex);
    }

    @Override
    public void visit(NamedParameter named) {
      buffer.append(" :");
      buffer.append(named.name);

      appendArrayIndex(named.arrayIndex);
    }

    private void appendArrayIndex(@Nullable Integer arrayIndex) {
      if (arrayIndex != null) {
        buffer.append('[')
                .append(arrayIndex)
                .append(']');
      }
    }

    @Override
    public void visit(VariableRef variableRef) {
      buffer.append(" @");
      buffer.append(variableRef.name);

      appendArrayIndex(variableRef.arrayIndex);
    }

    @Override
    public void visit(WhereExpression whereExpression) {
      buffer.append(" WHERE");
      whereExpression.expression.accept(this);
    }

    @Override
    public void visit(AndExpression andExpression) {
      andExpression.leftExpression.accept(this);
      buffer.append(" AND");
      andExpression.rightExpression.accept(this);
    }

    @Override
    public void visit(OrExpression orExpression) {
      orExpression.leftExpression.accept(this);
      buffer.append(" OR");
      orExpression.rightExpression.accept(this);
    }

    @Override
    public void visit(XorExpression xorExpression) {
      xorExpression.leftExpression.accept(this);
      buffer.append(" XOR");
      xorExpression.rightExpression.accept(this);
    }

    @Override
    public void visit(SelectExpression select) {
      visit(select.select);

      if (select.where != null) {
        visit(select.where);
      }
      if (select.groupBy != null) {
        select.groupBy.accept(this);
      }
      if (select.having != null) {
        select.having.accept(this);
      }
      if (select.other != null) {
        visit(select.other);
      }
    }
  }
}
