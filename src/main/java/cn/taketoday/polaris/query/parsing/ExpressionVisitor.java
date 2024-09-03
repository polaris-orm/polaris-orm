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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/9/2 22:36
 */
public interface ExpressionVisitor {

  default void visit(Expression expression) {

  }

  default void visit(AndExpression andExpression) {

  }

  default void visit(ColumnExpression column) {

  }

  default void visit(Between between) {

  }

  default void visit(ComparisonExpression comparison) {

  }

  default void visit(ExpressionList expressionList) {

  }

  default void visit(FunctionExpression function) {

  }

  default void visit(GroupByExpression groupBy) {

  }

  default void visit(HavingExpression having) {

  }

  default void visit(IndexParameter indexParameter) {

  }

  default void visit(HashParameter hash) {

  }

  default void visit(InExpression inExpression) {

  }

  default void visit(IsNullExpression isNullExpression) {

  }

  default void visit(LikeExpression likeExpression) {

  }

  default void visit(LiteralExpression literal) {

  }

  default void visit(NamedParameter named) {

  }

  default void visit(OrExpression orExpression) {

  }

  default void visit(ParenExpression paren) {

  }

  default void visit(VariableRef variableRef) {

  }

  default void visit(WhereExpression whereExpression) {

  }

  default void visit(XorExpression xorExpression) {

  }

  default void visit(String expression) {

  }

  default void visit(SelectExpression select) {

  }

}
