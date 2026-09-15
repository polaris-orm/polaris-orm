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
import cn.taketoday.polaris.query.parsing.ast.ArithmeticExpression;
import cn.taketoday.polaris.query.parsing.ast.Between;
import cn.taketoday.polaris.query.parsing.ast.ColumnExpression;
import cn.taketoday.polaris.query.parsing.ast.ComparisonExpression;
import cn.taketoday.polaris.query.parsing.ast.ExistsExpression;
import cn.taketoday.polaris.query.parsing.ast.Expression;
import cn.taketoday.polaris.query.parsing.ast.ExpressionList;
import cn.taketoday.polaris.query.parsing.ast.FunctionExpression;
import cn.taketoday.polaris.query.parsing.ast.GroupByExpression;
import cn.taketoday.polaris.query.parsing.ast.HashParameter;
import cn.taketoday.polaris.query.parsing.ast.HavingExpression;
import cn.taketoday.polaris.query.parsing.ast.InExpression;
import cn.taketoday.polaris.query.parsing.ast.IndexParameter;
import cn.taketoday.polaris.query.parsing.ast.IsExpression;
import cn.taketoday.polaris.query.parsing.ast.IsNullExpression;
import cn.taketoday.polaris.query.parsing.ast.LikeExpression;
import cn.taketoday.polaris.query.parsing.ast.LiteralExpression;
import cn.taketoday.polaris.query.parsing.ast.NamedParameter;
import cn.taketoday.polaris.query.parsing.ast.NotExpression;
import cn.taketoday.polaris.query.parsing.ast.OrExpression;
import cn.taketoday.polaris.query.parsing.ast.ParenExpression;
import cn.taketoday.polaris.query.parsing.ast.UnaryExpression;
import cn.taketoday.polaris.query.parsing.ast.VariableRef;
import cn.taketoday.polaris.query.parsing.ast.WhereExpression;
import cn.taketoday.polaris.query.parsing.ast.XorExpression;
import cn.taketoday.polaris.util.Nullable;

/**
 * A handwritten recursive descent parser for {@code SELECT} statements.
 *
 * <p>Only the {@code WHERE} / {@code GROUP BY} / {@code HAVING} clauses are modelled as an
 * expression tree, the select list and the trailing clauses (for example {@code ORDER BY}
 * or {@code LIMIT}) are preserved verbatim so that the original statement can be rendered
 * back without any loss.
 *
 * <p>The supported condition grammar (from the lowest to the highest precedence) is:
 *
 * <pre>{@code
 * orExpr        := andExpr (OR andExpr)*
 * andExpr       := xorExpr (AND xorExpr)*
 * xorExpr       := notExpr (XOR notExpr)*
 * notExpr       := (NOT | '!') notExpr | condition
 * condition     := EXISTS '(' subquery ')'
 *                | '(' orExpr ')' predicate?
 *                | value predicate?
 * predicate     := comparison value
 *                | IS [NOT] (NULL | TRUE | FALSE | UNKNOWN)
 *                | [NOT] (BETWEEN value AND value | IN '(' valueList ')'
 *                         | LIKE | REGEXP | RLIKE)
 * value         := additive
 * additive      := multiplicative (('+' | '-') multiplicative)*
 * multiplicative:= unary (('*' | '/' | '%') unary)*
 * unary         := ('+' | '-') unary | primary
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 22:25
 */
public class SelectParser {

  private final String selectSQL;

  private final List<Token> tokenStream;

  private final int tokenStreamLength;

  private int tokenStreamPointer;

  SelectParser(String selectSQL) {
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
    int selectEndIndex = selectSQL.length();
    int clauseIndex = tokenStreamLength;

    int parenLayer = 0;
    for (int i = tokenStreamPointer; i < tokenStreamLength; i++) {
      Token t = tokenStream.get(i);
      if (t.kind == TokenKind.LPAREN) {
        parenLayer++;
      }
      else if (t.kind == TokenKind.RPAREN) {
        if (parenLayer > 0) {
          parenLayer--;
        }
      }
      else if (parenLayer == 0 && t.isIdentifier() && isClauseKeyword(t.stringValue())) {
        clauseIndex = i;
        selectEndIndex = t.startPos;
        break;
      }
    }
    tokenStreamPointer = clauseIndex;

    WhereExpression whereExpression = null;
    if (peekIdentifierToken("where")) {
      Token whereToken = takeToken();
      whereExpression = eatWhereExpression();
      if (whereExpression == null) {
        throw parsingException(whereToken.endPos, "Where clause not found");
      }
    }

    GroupByExpression groupBy = maybeEatGroupByExpression();
    HavingExpression having = maybeEatHavingExpression();

    // ORDER BY, LIMIT, ... are kept verbatim
    String other = null;
    Token remaining = peekToken();
    if (remaining != null) {
      other = selectSQL.substring(remaining.startPos);
    }

    return new SelectExpression(selectSQL.substring(0, selectEndIndex),
            whereExpression, groupBy, having, other);
  }

  private static boolean isClauseKeyword(String identifier) {
    return identifier.equalsIgnoreCase("where")
            || identifier.equalsIgnoreCase("group")
            || identifier.equalsIgnoreCase("having")
            || identifier.equalsIgnoreCase("order")
            || identifier.equalsIgnoreCase("limit")
            || identifier.equalsIgnoreCase("offset")
            || identifier.equalsIgnoreCase("union");
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
    ArrayList<Expression> expressions = new ArrayList<>();
    expressions.add(eatValueExpression());
    while (peekToken(TokenKind.COMMA)) {
      takeToken();
      expressions.add(eatValueExpression());
    }
    return new ExpressionList(expressions);
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

  // logicalAndExpression : logicalXorExpression (AND^ logicalXorExpression)*;
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

  // logicalXorExpression : notExpression (XOR^ notExpression)*;

  @Nullable
  private Expression eatLogicalXorExpression() {
    Expression expr = eatNotExpression();
    while (peekIdentifierToken("xor")) {
      Token t = takeToken();  // consume 'XOR'
      Expression rhExpr = eatNotExpression();
      checkOperands(t, expr, rhExpr);
      expr = new XorExpression(expr, rhExpr);
    }
    return expr;
  }

  // notExpression : (NOT | '!') notExpression | conditionExpression;

  @Nullable
  private Expression eatNotExpression() {
    if (peekToken() == null) {
      return null;
    }
    if (peekIdentifierToken("not") || peekToken(TokenKind.BANG)) {
      Token t = takeToken();
      Expression expression = eatNotExpression();
      checkRightOperand(t, expression);
      return new NotExpression(expression);
    }
    return eatConditionExpression();
  }

  @Nullable
  private Expression eatConditionExpression() {
    if (peekToken() == null) {
      return null;
    }

    Expression existsExpression = maybeEatExistsExpression();
    if (existsExpression != null) {
      return existsExpression;
    }

    // a parenthesized expression is a plain primary value: it may be a boolean group
    // like (a = 1), an arithmetic group like (1 + 2), or a subquery
    Expression left = eatValueExpression();
    Expression expression = maybeEatPredicateOperator(left);
    return expression != null ? expression : left;
  }

  @Nullable
  private Expression maybeEatExistsExpression() {
    if (!peekIdentifierToken("exists")) {
      return null;
    }
    Token existsToken = takeToken();
    eatToken(TokenKind.LPAREN);

    Token selectToken = peekToken();
    if (selectToken == null || !selectToken.isIdentifier("select")) {
      throw parsingException(existsToken.endPos, "Subquery expected after EXISTS");
    }
    takeToken();  // consume SELECT

    Expression subquery = eatSubqueryExpression(selectToken.startPos, false);
    eatToken(TokenKind.RPAREN);
    return new ExistsExpression(subquery);
  }

  @Nullable
  private Expression maybeEatPredicateOperator(Expression left) {
    Token operator = peekToken();
    if (operator == null) {
      return null;
    }
    return switch (operator.kind) {
      case LE, LT, GE, GT, EQ, NE, NULL_SAFE_EQ -> {
        takeToken();
        Expression right = eatValueExpression();
        yield new ComparisonExpression(new String(operator.kind.tokenChars), left, right);
      }
      case IDENTIFIER -> maybeEatIdentifierOperatorExpression(operator, left);
      default -> null;
    };
  }

  @Nullable
  private Expression maybeEatIdentifierOperatorExpression(Token operatorT, Expression left) {
    boolean not = false;
    String operator = operatorT.stringValue();
    if (operator.equalsIgnoreCase("not")) {
      Token next = peekTokenAhead(1);
      if (next == null || !isNotOperator(next)) {
        // 'not' is not part of a supported operator here
        return null;
      }
      takeToken();  // consume NOT
      not = true;
      operatorT = next;
      operator = next.stringValue();
    }

    if (operator.equalsIgnoreCase("between")) {
      takeToken();
      // between value1 and value2
      Expression start = eatValueExpression();
      Token andToken = eatToken(TokenKind.IDENTIFIER);
      if (!andToken.isIdentifier("and")) {
        throw parsingException(andToken.startPos,
                "Unexpected token. Expected 'and' but was '%s'".formatted(andToken.stringValue()));
      }
      Expression end = eatValueExpression();
      return new Between(left, not, start, end);
    }
    else if (operator.equalsIgnoreCase("in")) {
      takeToken();
      if (!peekToken(TokenKind.LPAREN)) {
        throw parsingException(selectSQL.length(), "Syntax error, '(' expected after IN");
      }
      // IN (1, 2, ?, :age, 5) | IN (select ...)
      Expression expression = eatArgumentsExpression();
      return new InExpression(left, not, expression);
    }
    else if (operator.equalsIgnoreCase("like")
            || operator.equalsIgnoreCase("regexp")
            || operator.equalsIgnoreCase("rlike")) {
      takeToken();
      boolean binary = false;
      if (peekIdentifierToken("binary")) {
        takeToken();
        // like binary '' | not like binary ''
        binary = true;
      }
      // like '' | not like ''
      Expression right = eatValueExpression();
      Expression escape = null;
      if (peekIdentifierToken("escape")) {
        // like '/%/_%_' ESCAPE '/' | not like '/%/_%_' ESCAPE '/'
        takeToken();
        escape = eatValueExpression();
      }
      return new LikeExpression(left, not, right, binary, operator, escape);
    }
    else if (operator.equalsIgnoreCase("is")) {
      takeToken();
      // is null | is not null | is [not] true|false|unknown
      return eatIsExpression(left);
    }
    return null;
  }

  private static boolean isNotOperator(Token token) {
    return token.isIdentifier("between")
            || token.isIdentifier("in")
            || token.isIdentifier("like")
            || token.isIdentifier("regexp")
            || token.isIdentifier("rlike");
  }

  private Expression eatIsExpression(Expression left) {
    boolean not = false;
    Token t = takeToken();
    if (t.isIdentifier("not")) {
      t = takeToken();
      not = true;
    }
    if (t.isIdentifier("null")) {
      return new IsNullExpression(left, not);
    }
    if (t.isIdentifier("true") || t.isIdentifier("false") || t.isIdentifier("unknown")) {
      return new IsExpression(left, not, new LiteralExpression(t.stringValue()));
    }
    throw parsingException(t.startPos, "Not a valid operator token: ''%s''".formatted(toString(t)));
  }

  // value expression (arithmetic), used by conditions, arguments, group by, ...

  private Expression eatValueExpression() {
    return eatAdditiveExpression();
  }

  private Expression eatAdditiveExpression() {
    Expression expr = eatMultiplicativeExpression();
    while (true) {
      Token t = peekToken();
      if (t == null || (t.kind != TokenKind.PLUS && t.kind != TokenKind.MINUS)) {
        break;
      }
      takeToken();
      Expression right = eatMultiplicativeExpression();
      expr = new ArithmeticExpression(new String(t.kind.tokenChars), expr, right);
    }
    return expr;
  }

  private Expression eatMultiplicativeExpression() {
    Expression expr = eatUnaryExpression();
    while (true) {
      Token t = peekToken();
      if (t == null || (t.kind != TokenKind.STAR && t.kind != TokenKind.SLASH && t.kind != TokenKind.PERCENT)) {
        break;
      }
      takeToken();
      Expression right = eatUnaryExpression();
      expr = new ArithmeticExpression(new String(t.kind.tokenChars), expr, right);
    }
    return expr;
  }

  private Expression eatUnaryExpression() {
    Token t = peekToken();
    if (t != null && (t.kind == TokenKind.PLUS || t.kind == TokenKind.MINUS)) {
      takeToken();
      return new UnaryExpression(new String(t.kind.tokenChars), eatUnaryExpression());
    }
    return eatPrimaryExpression();
  }

  private Expression eatPrimaryExpression() {
    Token value = takeToken();
    return switch (value.kind) {
      case LITERAL_STRING, LITERAL_REAL, LITERAL_HEXINT, LITERAL_HEXLONG,
           LITERAL_LONG, LITERAL_INT, LITERAL_REAL_FLOAT -> new LiteralExpression(value.stringValue());
      case STAR -> new LiteralExpression(selectSQL.substring(value.startPos, value.endPos));
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
      case LPAREN -> eatParenExpression();
      default -> eatIdentifierExpression(value);
    };
  }

  private Expression eatParenExpression() {
    Token next = peekToken();
    if (next != null && next.isIdentifier("select")) {
      // (select ...)
      takeToken();
      Expression subquery = eatSubqueryExpression(next.startPos, false);
      eatToken(TokenKind.RPAREN);
      return new ParenExpression(subquery);
    }
    if (peekToken(TokenKind.RPAREN)) {
      // ()
      takeToken();
      return new ParenExpression(new ExpressionList(new ArrayList<>()));
    }
    Expression expr = eatLogicalOrExpression();
    eatToken(TokenKind.RPAREN);
    return new ParenExpression(expr);
  }

  private Expression eatIdentifierExpression(Token value) {
    if (value.isIdentifier("select")) {
      // maybe a subquery IN(select a from b), in((select 1), 2, 3)
      return eatSubqueryExpression(value.startPos, true);
    }

    if (value.isIdentifier("true") || value.isIdentifier("false") || value.isIdentifier("null")) {
      return new LiteralExpression(value.stringValue());
    }

    boolean binary = false;
    if (value.isIdentifier("binary")) {
      Token next = peekToken();
      if (next == null || !next.isIdentifier()) {
        // 'binary' used as a plain column name
        return new ColumnExpression(value.stringValue(), false, false);
      }
      value = takeToken();
      binary = true;
    }

    if (value.isIdentifier()) {
      if (peekToken(TokenKind.LPAREN)) {
        // function func(c)
        Expression args = eatArgumentsExpression();
        return new FunctionExpression(value.stringValue(), args, binary);
      }
      // column
      return eatColumnExpression(value, binary);
    }
    throw parsingException(value.startPos, "Unsupported value expression '%s'".formatted(toString(value)));
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

  // (1, 2, :xxx, func(1, 2), (select 1), false, '1', @var)
  private Expression eatArgumentsExpression() {
    eatToken(TokenKind.LPAREN);
    ArrayList<Expression> args = new ArrayList<>();

    if (!peekToken(TokenKind.RPAREN)) {
      while (true) {
        args.add(eatValueExpression());
        if (peekToken(TokenKind.COMMA)) {
          takeToken();
        }
        else {
          break;
        }
      }
    }
    eatToken(TokenKind.RPAREN);

    return new ParenExpression(new ExpressionList(args));
  }

  /**
   * Parse a (possibly nested) subquery. The {@code SELECT} token is expected to be already
   * consumed, the token stream pointer points to the first token of the subquery body.
   *
   * @param startPos the offset of the {@code SELECT} token
   * @param stopAtComma whether a top-level comma terminates the subquery (a bare subquery
   * used inside an argument list), otherwise only the matching {@code ')'} does
   */
  private Expression eatSubqueryExpression(int startPos, boolean stopAtComma) {
    int parenLayer = 0;
    int boundary = tokenStreamLength;
    for (int i = tokenStreamPointer; i < tokenStreamLength; i++) {
      Token t = tokenStream.get(i);
      if (t.kind == TokenKind.LPAREN) {
        parenLayer++;
      }
      else if (t.kind == TokenKind.RPAREN) {
        if (parenLayer == 0) {
          boundary = i;
          break;
        }
        parenLayer--;
      }
      else if (stopAtComma && parenLayer == 0 && t.kind == TokenKind.COMMA) {
        boundary = i;
        break;
      }
    }

    int endPos = boundary < tokenStreamLength ? tokenStream.get(boundary).startPos : selectSQL.length();
    String subquerySQL = selectSQL.substring(startPos, endPos);
    tokenStreamPointer = boundary;

    return new SelectParser(subquerySQL).parse();
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

  @Nullable
  private Token peekTokenAhead(int offset) {
    int index = this.tokenStreamPointer + offset;
    if (index < 0 || index >= this.tokenStreamLength) {
      return null;
    }
    return this.tokenStream.get(index);
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