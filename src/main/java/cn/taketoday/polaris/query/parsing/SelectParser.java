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

import cn.taketoday.polaris.query.parsing.ast.SelectNode;
import cn.taketoday.polaris.query.parsing.ast.SqlNode;
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
        WhereNode whereNode = eatWhereExpression();
        return new SelectNode(selectSQL.substring(0, whereToken.startPos), whereNode);
      }
      else {
        return new SelectNode(selectSQL, null);
      }
    }
    throw new ParsingException("Not a select SQL");
  }

  private WhereNode eatWhereExpression() {

    Token next = nextToken();
    while (next != null) {
      next = nextToken();
    }

    return null;
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

  // Static factory methods

  public static SelectExpression parse(String selectSQL) {
    return new SelectParser(selectSQL).parse();
  }

}
