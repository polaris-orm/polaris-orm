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
import cn.taketoday.polaris.query.parsing.ast.ColumnNameExpression;
import cn.taketoday.polaris.query.parsing.ast.ComparisonOperator;
import cn.taketoday.polaris.query.parsing.ast.Expression;
import cn.taketoday.polaris.query.parsing.ast.HashParameter;
import cn.taketoday.polaris.query.parsing.ast.InExpression;
import cn.taketoday.polaris.query.parsing.ast.IndexParameter;
import cn.taketoday.polaris.query.parsing.ast.IsNullExpression;
import cn.taketoday.polaris.query.parsing.ast.LikeExpression;
import cn.taketoday.polaris.query.parsing.ast.LiteralStringExpression;
import cn.taketoday.polaris.query.parsing.ast.NamedParameter;
import cn.taketoday.polaris.query.parsing.ast.OrExpression;
import cn.taketoday.polaris.query.parsing.ast.ParenExpression;
import cn.taketoday.polaris.query.parsing.ast.SelectNode;
import cn.taketoday.polaris.query.parsing.ast.VariableRef;
import cn.taketoday.polaris.query.parsing.ast.WhereNode;
import cn.taketoday.polaris.util.Nullable;

/**
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
    SelectNode ast = eatExpression();
    return new SelectExpression(ast);
  }

  private SelectNode eatExpression() {
    if (peekIdentifierToken("SELECT")) {
      Token whereToken = null;
      Token next = nextToken();
      while (next != null) {
        if (next.isIdentifier("WHERE")) {
          // where token
          whereToken = next;
          break;
        }
        next = nextToken();
      }
      if (whereToken != null) {
        WhereNode whereNode = eatWhereExpression();
        Token token = peekToken();
        if (token != null) {
          return new SelectNode(selectSQL.substring(0, whereToken.startPos), whereNode,
                  selectSQL.substring(token.startPos));
        }
        return new SelectNode(selectSQL.substring(0, whereToken.startPos), whereNode, null);
      }
      else {
        return new SelectNode(selectSQL, null, null);
      }
    }
    throw parsingException(0, "Not a select statement");
  }

  private WhereNode eatWhereExpression() {
    Expression expression = eatLogicalOrExpression();
    Token token = peekToken();
    if (token == null || token.isIdentifier()) {
      return new WhereNode(expression);
    }
    throw parsingException(token.startPos, "Syntax error");
  }

  // logicalOrExpression : logicalAndExpression (OR^ logicalAndExpression)*;

  @Nullable
  private Expression eatLogicalOrExpression() {
    Expression expr = eatLogicalAndExpression();
    while (peekIdentifierToken("or")) {
      Token t = takeToken();  //consume OR
      Expression rhExpr = eatLogicalAndExpression();
      checkOperands(t, expr, rhExpr);
      expr = new OrExpression(expr, rhExpr, t.startPos, t.endPos);
    }
    return expr;
  }

  // logicalAndExpression : relationalExpression (AND^ relationalExpression)*;
  private Expression eatLogicalAndExpression() {
    Expression expr = eatRelationalExpression();
    while (peekIdentifierToken("and")) {
      Token t = takeToken();  // consume 'AND'
      Expression rhExpr = eatRelationalExpression();
      checkOperands(t, expr, rhExpr);
      expr = new AndExpression(expr, rhExpr, t.startPos, t.endPos);
    }
    return expr;
  }

  // ALL	      TRUE if all of the subquery values meet the condition
  // AND	      TRUE if all the conditions separated by AND is TRUE
  // ANY	      TRUE if any of the subquery values meet the condition
  // BETWEEN	  TRUE if the operand is within the range of comparisons
  // EXISTS	    TRUE if the subquery returns one or more records
  // IN	        TRUE if the operand is equal to one of a list of expressions
  // LIKE	      TRUE if the operand matches a pattern
  // NOT	      Displays a record if the condition(s) is NOT TRUE
  // OR	        TRUE if any of the conditions separated by OR is TRUE
  // SOME	      TRUE if any of the subquery values meet the condition

  private Expression eatRelationalExpression() {
    Expression parenExpr = maybeEatParenExpression();
    if (parenExpr != null) {
      return parenExpr;
    }

    // column
    boolean dotName = false;
    boolean maybeFunction = false;
    String columnName;
    Token columnToken = eatToken(TokenKind.IDENTIFIER);
    if (peekToken(TokenKind.DOT)) {
      nextToken();
      Token token = takeToken();
      columnName = selectSQL.substring(columnToken.startPos, token.endPos);
      dotName = true;
    }
    else if (peekToken(TokenKind.LPAREN)) {
      Token ft = takeToken();
      while (ft.kind != TokenKind.RPAREN) {
        ft = takeToken();
      }

      int endPos = ft.endPos;
      int startPos = columnToken.startPos;
      columnName = selectSQL.substring(startPos, endPos);
      maybeFunction = true;
    }
    else {
      columnName = columnToken.stringValue();
    }

    ColumnNameExpression left = new ColumnNameExpression(columnName, maybeFunction, dotName);

    // operator
    Token operator = takeToken("Syntax error, operator token expected");
    return switch (operator.kind) {
      case LE, LT, GE, GT, EQ, NE -> {
        Expression right = eatValueExpression();
        yield new ComparisonOperator(new String(operator.kind.tokenChars), left, right);
      }
      case IDENTIFIER -> {
        boolean not = false;
        String stringValue = operator.stringValue();
        if (stringValue.equalsIgnoreCase("not")) {
          not = true;
          operator = takeToken();
          stringValue = operator.stringValue();
        }

        if (stringValue.equalsIgnoreCase("between")) {
          // between value1 and value2
          Expression start = eatValueExpression();
          eatToken(TokenKind.IDENTIFIER);
          Expression end = eatValueExpression();
          yield new Between(left, not, start, end);
        }
        else if (stringValue.equalsIgnoreCase("in")) {
          // IN (1, 2, ?, :age, 5)
          eatToken(TokenKind.LPAREN);
          List<Expression> expressionList = new ArrayList<>();
          do {
            Expression expression = eatValueExpression();
            expressionList.add(expression);
            if (!peekToken(TokenKind.COMMA)) {
              // maybe a subquery IN(select a from b)

              break;
            }
            eatToken(TokenKind.COMMA);
          }
          while (!peekToken(TokenKind.RPAREN));
          eatToken(TokenKind.RPAREN);

          yield new InExpression(left, not, expressionList);
        }
        else if (stringValue.equalsIgnoreCase("like")) {
          // like '' | not like ''
          Expression right = eatValueExpression();
          yield new LikeExpression(left, not, right);
        }
        else if (stringValue.equalsIgnoreCase("is")) {
          // is null | is not null
          yield eatNullExpression(left);
        }
        else {
          throw parsingException(operator.startPos, "Not a valid operator token: '%s'".formatted(toString(operator)));
        }
      }
      default -> throw new ParsingException("Unsupported operator '%s'".formatted(toString(operator)));
    };
  }

  private Expression eatValueExpression() {
    Token value = takeToken();
    return switch (value.kind) {
      case LITERAL_STRING, LITERAL_REAL, LITERAL_HEXINT, LITERAL_HEXLONG,
           LITERAL_LONG, LITERAL_INT, LITERAL_REAL_FLOAT -> new LiteralStringExpression(value.stringValue());
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
      default -> throw new ParsingException("Unsupported value expression '%s'".formatted(toString(value)));
    };
  }

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
    Expression expr;
    // is null | is not null
    boolean notNull = false;
    Token t = takeToken();
    if (t.isIdentifier("not")) {
      t = takeToken();
      notNull = true;
    }
    if (t.isIdentifier("null")) {
      expr = new IsNullExpression(left, notNull);
    }
    else {
      throw new ParsingException("Not a valid operator token: ''%s''".formatted(toString(t)));
    }
    return expr;
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

  private boolean peekToken(TokenKind possible1, TokenKind possible2) {
    Token t = peekToken();
    if (t == null) {
      return false;
    }
    return (t.kind == possible1 || t.kind == possible2);
  }

  private boolean peekToken(TokenKind possible1, TokenKind possible2, TokenKind possible3) {
    Token t = peekToken();
    if (t == null) {
      return false;
    }
    return (t.kind == possible1 || t.kind == possible2 || t.kind == possible3);
  }

  private boolean peekIdentifierToken(String identifierString) {
    Token t = peekToken();
    if (t == null) {
      return false;
    }
    return t.isIdentifier(identifierString);
  }

  private Token takeToken() {
    return takeToken("Unexpectedly ran out of input");
  }

  private Token takeToken(String errorMessage) {
    if (this.tokenStreamPointer >= this.tokenStreamLength) {
      throw parsingException(selectSQL.length(), errorMessage);
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
      return new ParsingException("Statement [%s] @%s: %s".formatted(selectSQL, startPos, message));
    }
    return new ParsingException("Statement [%s]: %s".formatted(selectSQL, message));
  }

  // Static factory methods

  public static SelectExpression parse(String selectSQL) {
    return new SelectParser(selectSQL).parse();
  }

}
