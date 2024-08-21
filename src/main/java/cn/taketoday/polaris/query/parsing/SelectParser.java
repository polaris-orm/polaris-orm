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
import cn.taketoday.polaris.query.parsing.ast.SqlNode;
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
    SqlNode ast = eatExpression();
    Token t = peekToken();
    if (t != null) {
      throw new ParsingException("After parsing a valid expression, there is still more data in the expression: ''%s''"
              .formatted(toString(t)));
    }
    return new SelectExpression(ast);
  }

  private SqlNode eatExpression() {
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
        WhereNode whereNode = eatWhereExpression(whereToken);
        return new SelectNode(selectSQL.substring(0, whereToken.startPos), whereNode);
      }
      else {
        return new SelectNode(selectSQL, null);
      }
    }
    throw new ParsingException("Not a select SQL");
  }

  private WhereNode eatWhereExpression(Token whereToken) {
    Expression expression = eatLogicalOrExpression();

    Token next = nextToken();
    while (next != null) {
      next = nextToken();
    }

    return new WhereNode(expression);
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
    Token columnToken = eatToken(TokenKind.IDENTIFIER);
    String columnName = columnToken.stringValue();
    if (peekToken(TokenKind.DOT)) {
      nextToken();
      Token token = takeToken();
      columnName = columnName + "." + token.stringValue();
    }

    ColumnNameExpression left = new ColumnNameExpression(columnName);

    // operator
    Token operator = takeToken();
    return switch (operator.kind) {
      case LE, LT, GE, GT, EQ, NE -> {
        Expression right = eatValueExpression();
        yield new ComparisonOperator(new String(operator.kind.tokenChars), left, right);
      }
      case IDENTIFIER -> {
        boolean not = false;
        Expression expr;
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
          expr = new Between(left, not, start, end);
        }
        else if (stringValue.equalsIgnoreCase("in")) {
          // maybe a subquery
          // todo
          expr = new InExpression(left, not);
        }
        else if (stringValue.equalsIgnoreCase("like")) {
          // like '' | not like ''
          Expression right = eatValueExpression();
          expr = new LikeExpression(left, not, right);
        }
        else if (stringValue.equalsIgnoreCase("is")) {
          expr = eatNullExpression(left);
        }
        else {
          throw parsingException(operator.startPos, "Not a valid operator token: '%s'".formatted(toString(operator)));
        }
        yield expr;
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
      default -> throw new ParsingException("Unsupported operator '%s'".formatted(toString(value)));
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

  //parenExpr : LPAREN! expression RPAREN!;

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
    if (this.tokenStreamPointer >= this.tokenStreamLength) {
      throw new IllegalStateException("No token");
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
    String happenPos = selectSQL.substring(startPos);
    if (happenPos.length() > 32) {
      happenPos = happenPos.substring(0, 32);
    }
    return new ParsingException(message + ", near : '" + happenPos + "'");
  }

  // Static factory methods

  public static SelectExpression parse(String selectSQL) {
    return new SelectParser(selectSQL).parse();
  }

}
