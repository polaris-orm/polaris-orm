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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.polaris.query.parsing.ast.AndExpression;
import cn.taketoday.polaris.query.parsing.ast.Between;
import cn.taketoday.polaris.query.parsing.ast.ColumnExpression;
import cn.taketoday.polaris.query.parsing.ast.ComparisonOperator;
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

/**
 * Select statement parser
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 22:25
 */
public class SelectParser {

  private final String selectSQL;

  private final List<Token> tokenStream;

  private final int tokenStreamLength;

  private int tokenStreamPointer;

  public SelectParser(String selectSQL) {
    this.selectSQL = selectSQL;
    this.tokenStream = new Tokenizer(selectSQL).process();
    this.tokenStreamLength = tokenStream.size();
  }

  public SelectExpression parse() {
    if (peekIdentifierToken("SELECT")) {
      return eatSelectExpression();
    }
    throw parsingException(0, "Not a select statement");
  }

  private SelectExpression eatSelectExpression() {
    Token whereToken = null;
    Token next = peekToken();
    int selectEndIndex = selectSQL.length();
    while (next != null) {
      if (next.isIdentifier("WHERE")) {
        // where token
        whereToken = next;
        selectEndIndex = whereToken.startPos;
        break;
      }
      else if (next.isIdentifier("group")) {
        // group token
        tokenStreamPointer--;
        selectEndIndex = next.startPos;
        break;
      }
      next = nextToken();
    }

    // todo 嵌套 where

    WhereExpression whereExpression = null;
    if (whereToken != null) {
      whereExpression = eatWhereExpression();
      if (whereExpression == null) {
        throw parsingException(whereToken.endPos, "Where clause not found");
      }
    }

    GroupByExpression groupBy = maybeEatGroupByExpression();
    HavingExpression having = maybeEatHavingExpression();

    return new SelectExpression(selectSQL.substring(0, selectEndIndex),
            whereExpression, groupBy, having, null);
  }

  @Nullable
  private GroupByExpression maybeEatGroupByExpression() {
    if (peekIdentifierToken("group")) {
      takeToken();
      eatIdentifier("by");

      ExpressionList expression = eatExpressionList();

      boolean withRollup = false;
      if (peekIdentifierToken("with")) {
        takeToken();
        eatIdentifier("rollup");
        withRollup = true;
      }
      return new GroupByExpression(expression, withRollup);
    }
    return null;
  }

  @Nullable
  private HavingExpression maybeEatHavingExpression() {
    if (peekIdentifierToken("having")) {
      takeToken();
      Expression expression = eatLogicalOrExpression();
      if (expression != null) {
        Token token = peekToken();
        if (token == null || token.isIdentifier() || token.kind == TokenKind.RPAREN) {
          return new HavingExpression(expression);
        }
        throw parsingException(token.startPos, "Syntax error");
      }
    }
    return null;
  }

  private ExpressionList eatExpressionList() {
    Token t = takeToken();
    ArrayList<Expression> args = new ArrayList<>();

    for (; t.kind == TokenKind.COMMA; t = takeToken()) {
      Expression expr = eatValueExpression();
      args.add(expr);
    }

    return new ExpressionList(args);
  }

  @Nullable
  private WhereExpression eatWhereExpression() {
    Expression expression = eatLogicalOrExpression();
    if (expression != null) {
      Token token = peekToken();
      if (token == null || token.isIdentifier() || token.kind == TokenKind.RPAREN) {
        return new WhereExpression(expression);
      }
      throw parsingException(token.startPos, "Syntax error");
    }
    return null;
  }

  // logicalOrExpression : logicalAndExpression (OR^ logicalAndExpression)*;

  @Nullable
  private Expression eatLogicalOrExpression() {
    Expression expr = eatLogicalAndExpression();
    while (peekIdentifierToken("or")) {
      Token t = takeToken();  //consume OR
      Expression rhExpr = eatLogicalAndExpression();
      checkOperands(t, expr, rhExpr);
      expr = new OrExpression(expr, rhExpr);
    }
    return expr;
  }

  // logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
  @Nullable
  private Expression eatLogicalAndExpression() {
    Expression expr = eatLogicalXorExpression();
    while (peekIdentifierToken("and")) {
      Token t = takeToken();  // consume 'AND'
      Expression rhExpr = eatLogicalXorExpression();
      checkOperands(t, expr, rhExpr);
      expr = new AndExpression(expr, rhExpr);
    }
    return expr;
  }

  // logicalXorExpression : relationalExpression (XOR^ relationalExpression)*;

  @Nullable
  private Expression eatLogicalXorExpression() {
    Expression expr = eatConditionExpression();
    while (peekIdentifierToken("xor")) {
      Token t = takeToken();  // consume 'XOR'
      Expression rhExpr = eatConditionExpression();
      checkOperands(t, expr, rhExpr);
      expr = new XorExpression(expr, rhExpr);
    }
    return expr;
  }

  @Nullable
  private Expression eatConditionExpression() {
    Expression parenExpr = maybeEatParenExpression();
    if (parenExpr != null) {
      return parenExpr;
    }

    // binary col = val
    boolean binary = false;
    if (peekIdentifierToken("binary")) {
      takeToken();
      binary = true;
    }

    Expression funcExpr = maybeEatFunctionExpression(binary);
    if (funcExpr != null) {
      Expression expression = maybeEatOperatorExpression(funcExpr);
      if (expression != null) {
        return expression;
      }
      return funcExpr;
    }

    // column
    Token columnT = nextToken();
    if (columnT != null) {
      // col | literal
      Expression left;
      if (columnT.isLiteral()) {
        left = new LiteralExpression(columnT.stringValue());
      }
      else if (columnT.isIdentifier()) {
        left = eatColumnExpression(columnT, binary);
      }
      else {
        return null;
      }
      Token operator = peekToken();
      if (operator == null) {
        throw parsingException(selectSQL.length(), "Syntax error, operator token expected");
      }

      // operator
      Expression expression = maybeEatOperatorExpression(left);
      if (expression == null) {
        throw parsingException(operator.startPos, "Unsupported operator '%s'".formatted(toString(operator)));
      }
      return expression;
    }
    return null;
  }

  @Nullable
  private Expression maybeEatOperatorExpression(Expression left) {
    Token operator = peekToken();
    if (operator == null) {
      return null;
    }
    return switch (operator.kind) {
      case LE, LT, GE, GT, EQ, NE -> {
        takeToken();
        Expression right = eatValueExpression();
        yield new ComparisonOperator(new String(operator.kind.tokenChars), left, right);
      }
      case IDENTIFIER -> maybeEatIdentifierOperatorExpression(operator, left);
      default -> null;
    };
  }

  private Expression eatColumnExpression(Token columnToken, boolean binary) {
    boolean dotName = false;
    String columnName;
    if (peekToken(TokenKind.DOT)) {
      nextToken();
      Token token = takeToken();
      columnName = selectSQL.substring(columnToken.startPos, token.endPos);
      dotName = true;
    }
    else {
      columnName = columnToken.stringValue();
    }

    return new ColumnExpression(columnName, dotName, binary);
  }

  // funcExpr

  @Nullable
  private Expression maybeEatFunctionExpression(boolean binary) {
    if (peekTokens(TokenKind.IDENTIFIER, TokenKind.LPAREN)) {
      // function func(c)
      Token nameToken = eatToken(TokenKind.IDENTIFIER);

      // func(1, 2, ?, :age, 5)
      Expression args = eatArgumentsExpression();
      return new FunctionExpression(nameToken.stringValue(), args, binary);
    }
    return null;
  }

  @Nullable
  private Expression maybeEatIdentifierOperatorExpression(Token operatorT, Expression left) {
    boolean not = false;
    String operator = operatorT.stringValue();
    if (operator.equalsIgnoreCase("not")) {
      takeToken();
      not = true;
      operatorT = peekToken();
      if (operatorT == null) {
        throw parsingException(selectSQL.length(), "Syntax error, operator token expected");
      }
      operator = operatorT.stringValue();
    }

    if (operator.equalsIgnoreCase("between")) {
      takeToken();
      // between value1 and value2
      Expression start = eatValueExpression();
      eatToken(TokenKind.IDENTIFIER);
      Expression end = eatValueExpression();
      return new Between(left, not, start, end);
    }
    else if (operator.equalsIgnoreCase("in")) {
      takeToken();
      // IN (1, 2, ?, :age, 5)
      Expression expression = eatArgumentsExpression();
      return new InExpression(left, not, expression);
    }
    else if (operator.equalsIgnoreCase("like")
            || operator.equalsIgnoreCase("regexp")
            || operator.equalsIgnoreCase("rlike")) {
      takeToken();
      Expression escape = null;
      boolean binary = false;
      if (peekIdentifierToken("binary")) {
        takeToken();
        // like binary '' | not like binary ''
        binary = true;
      }
      // like '' | not like ''
      Expression right = eatValueExpression();
      if (peekIdentifierToken("escape")) {
        // like '/%/_%_' ESCAPE '/' | not like '/%/_%_' ESCAPE '/'
        takeToken();
        escape = eatValueExpression();
      }
      return new LikeExpression(left, not, right, binary, operator, escape);
    }
    else if (operator.equalsIgnoreCase("is")) {
      takeToken();
      // is null | is not null
      return eatNullExpression(left);
    }
    else {
      return null;
    }
  }

  private Expression eatValueExpression() {
    Token value = takeToken();
    boolean inParen = value.kind == TokenKind.LPAREN;
    if (inParen) {
      value = takeToken();
    }
    Expression expression = switch (value.kind) {
      case LITERAL_STRING, LITERAL_REAL, LITERAL_HEXINT, LITERAL_HEXLONG,
           LITERAL_LONG, LITERAL_INT, LITERAL_REAL_FLOAT, STAR -> new LiteralExpression(value.stringValue());
      case COLON -> {
        Token nameT = eatToken(TokenKind.IDENTIFIER);
        Integer arrayIndex = maybeEatArrayExpression();
        yield new NamedParameter(nameT.stringValue(), arrayIndex);
      }
      case QMARK -> new IndexParameter();
      case HASH -> {
        Token nameT = eatToken(TokenKind.IDENTIFIER);
        Integer arrayIndex = maybeEatArrayExpression();
        yield new HashParameter(nameT.stringValue(), arrayIndex);
      }
      case VARIABLE_REF -> {
        Token nameT = eatToken(TokenKind.IDENTIFIER);
        Integer arrayIndex = maybeEatArrayExpression();
        yield new VariableRef(nameT.stringValue(), arrayIndex);
      }
      default -> {
        // maybe a subquery IN(select a from b), in((select 1), 2, 3)
        if (value.isIdentifier("select")) {
          yield eatSubqueryExpression(value.startPos, inParen);
        }

        if (value.isIdentifier("true") || value.isIdentifier("false")) {
          yield new LiteralExpression(value.stringValue());
        }

        boolean binary = false;
        if (value.isIdentifier("binary")) {
          value = takeToken();
          binary = true;
        }

        if (value.isIdentifier()) {
          // function func(c)
          if (peekToken(TokenKind.LPAREN)) {
            // func(1, 2, ?, :age, 5)
            Expression args = eatArgumentsExpression();
            yield new FunctionExpression(value.stringValue(), args, binary);
          }

          yield eatColumnExpression(value, binary);
        }
        throw parsingException(value.startPos, "Unsupported value expression '%s'".formatted(toString(value)));
      }
    };

    if (inParen) {
      eatToken(TokenKind.RPAREN);
      return new ParenExpression(expression);
    }
    return expression;
  }

  // (1, 2, :xxx, func(1, 2), (select 1), false, '1', @var)
  private Expression eatArgumentsExpression() {
    Token t = eatToken(TokenKind.LPAREN);
    ArrayList<Expression> args = new ArrayList<>();

    for (; t.kind != TokenKind.RPAREN; t = takeToken()) {
      Expression expr = eatValueExpression();
      args.add(expr);
    }

    return new ParenExpression(new ExpressionList(args));
  }

  // (select * from a)
  private Expression eatSubqueryExpression(int startPos, boolean inParen) {
    int parenLayer = inParen ? 1 : 0;

    int selectEndPos = 0;

    Token whereToken = null;
    Token next = nextToken();
    while (next != null) {
      if (next.isIdentifier("WHERE")) {
        // where token
        whereToken = next;
        selectEndPos = whereToken.startPos;
        break;
      }

      if (next.kind == TokenKind.LPAREN) {
        parenLayer++;
      }
      else if (next.kind == TokenKind.RPAREN) {
        parenLayer--;
        if (parenLayer == 0) {
          selectEndPos = next.startPos;
          break;
        }
      }
      else if (next.kind == TokenKind.COMMA && !inParen) {
        selectEndPos = next.startPos;
        break;
      }

      next = nextToken();
    }

    String other = null;
    WhereExpression whereExpression = null;
    if (whereToken != null) {
      whereExpression = eatWhereExpression();
      if (whereExpression == null) {
        throw parsingException(whereToken.startPos, "Where clause not found");
      }

      Token t = takeToken();
      next = t;
      if (next.kind != TokenKind.RPAREN) {
        other = eatOtherExpression(parenLayer, next, t);
      }
    }

    tokenStreamPointer -= 1;
    return new SelectExpression(selectSQL.substring(startPos, selectEndPos), whereExpression, null, null, other);
  }

  @Nullable
  private String eatOtherExpression(int parenLayer, Token next, Token t) {
    while (next != null) {
      if (next.kind == TokenKind.LPAREN) {
        parenLayer++;
      }
      else if (next.kind == TokenKind.RPAREN) {
        parenLayer--;
        if (parenLayer == 0) {
          return selectSQL.substring(t.startPos, next.startPos);
        }
      }
      next = nextToken();
    }
    return null;
  }

  @Nullable
  private Integer maybeEatArrayExpression() {
    if (peekToken(TokenKind.LSQUARE)) {
      takeToken();
      Token intT = eatToken(TokenKind.LITERAL_INT);
      eatToken(TokenKind.RSQUARE);
      return Integer.parseInt(intT.stringValue());
    }
    return null;
  }

  private Expression eatNullExpression(Expression left) {
    // is null | is not null
    boolean not = false;
    Token t = takeToken();
    if (t.isIdentifier("not")) {
      t = takeToken();
      not = true;
    }
    if (t.isIdentifier("null")) {
      return new IsNullExpression(left, not);
    }
    throw parsingException(t.startPos, "Not a valid operator token: ''%s''".formatted(toString(t)));
  }

  // parenExpr : LPAREN! expression RPAREN!;

  @Nullable
  private Expression maybeEatParenExpression() {
    if (peekToken(TokenKind.LPAREN)) {
      Token t = nextToken();
      if (t == null) {
        return null;
      }
      Expression expr = eatLogicalOrExpression();
      if (expr == null) {
        throw parsingException(t.startPos, "Unexpectedly ran out of input");
      }
      eatToken(TokenKind.RPAREN);
      return new ParenExpression(expr);
    }
    return null;
  }

  private Token eatIdentifier(String identifier) {
    Token t = eatToken(TokenKind.IDENTIFIER);
    if (t.isIdentifier(identifier)) {
      return t;
    }
    throw parsingException(t.startPos,
            "Unexpected token. Expected '%s' but was '%s'".formatted(identifier, t.stringValue()));
  }

  private Token eatToken(TokenKind expectedKind) {
    Token t = nextToken();
    if (t == null) {
      int pos = this.selectSQL.length();
      throw parsingException(pos, "Unexpectedly ran out of input");
    }
    if (t.kind != expectedKind) {
      throw parsingException(t.startPos, "Unexpected token. Expected '%s' but was '%s'".formatted(
              expectedKind.toString().toLowerCase(), t.kind.toString().toLowerCase()));
    }
    return t;
  }

  private boolean peekToken(TokenKind possible1) {
    Token t = peekToken();
    if (t == null) {
      return false;
    }
    return t.kind == possible1;
  }

  private boolean peekTokens(TokenKind possible1, TokenKind possible2) {
    if (tokenStreamPointer + 1 >= tokenStreamLength) {
      return false;
    }
    Token t = this.tokenStream.get(tokenStreamPointer);
    if (t == null || t.kind != possible1) {
      return false;
    }
    t = this.tokenStream.get(tokenStreamPointer + 1);
    if (t == null) {
      return false;
    }
    return t.kind == possible2;
  }

  private boolean peekIdentifierToken(String identifierString) {
    Token t = peekToken();
    if (t == null) {
      return false;
    }
    return t.isIdentifier(identifierString);
  }

  private Token takeToken() {
    if (this.tokenStreamPointer >= this.tokenStreamLength) {
      throw parsingException(selectSQL.length(), "Unexpectedly ran out of input");
    }
    return this.tokenStream.get(this.tokenStreamPointer++);
  }

  @Nullable
  private Token nextToken() {
    if (this.tokenStreamPointer >= this.tokenStreamLength) {
      return null;
    }
    return this.tokenStream.get(this.tokenStreamPointer++);
  }

  @Nullable
  private Token peekToken() {
    if (this.tokenStreamPointer >= this.tokenStreamLength) {
      return null;
    }
    return this.tokenStream.get(this.tokenStreamPointer);
  }

  private String toString(@Nullable Token t) {
    if (t == null) {
      return "";
    }
    if (t.kind.hasPayload()) {
      return t.stringValue();
    }
    return t.kind.toString().toLowerCase();
  }

  private void checkOperands(Token token, @Nullable Expression left, @Nullable Expression right) {
    checkLeftOperand(token, left);
    checkRightOperand(token, right);
  }

  private void checkLeftOperand(Token token, @Nullable Expression operandExpression) {
    if (operandExpression == null) {
      throw parsingException(token.startPos, "Problem parsing left operand");
    }
  }

  private void checkRightOperand(Token token, @Nullable Expression operandExpression) {
    if (operandExpression == null) {
      throw parsingException(token.startPos, "Problem parsing right operand");
    }
  }

  private ParsingException parsingException(int startPos, String message) {
    if (startPos > 0) {
      String happenPos = selectSQL.substring(startPos);
      if (happenPos.length() > 32) {
        happenPos = happenPos.substring(0, 32);
      }
      if (happenPos.length() >= 4) {
        return new ParsingException("Statement [%s] @%s: %s, near : '%s'".formatted(selectSQL, startPos, message, happenPos));
      }
      return new ParsingException("Statement [%s] @%s: %s".formatted(selectSQL, startPos, message));
    }
    return new ParsingException("Statement [%s]: %s".formatted(selectSQL, message));
  }

  // Static factory methods

  public static SelectExpression parse(String selectSQL) {
    return new SelectParser(selectSQL).parse();
  }

}
