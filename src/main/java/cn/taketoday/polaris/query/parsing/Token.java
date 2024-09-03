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

import cn.taketoday.polaris.util.Nullable;

class Token {

  public TokenKind kind;

  @Nullable
  public final String data;

  public int startPos;  // index of first character

  public int endPos;  // index of char after the last character

  /**
   * Constructor for use when there is no particular data for the token
   * (e.g. TRUE or '+')
   *
   * @param startPos the exact start
   * @param endPos the index to the last character
   */
  public Token(TokenKind tokenKind, int startPos, int endPos) {
    this.kind = tokenKind;
    this.startPos = startPos;
    this.endPos = endPos;
    this.data = null;
  }

  public Token(TokenKind tokenKind, char[] tokenData, int startPos, int endPos) {
    this.kind = tokenKind;
    this.startPos = startPos;
    this.endPos = endPos;
    this.data = new String(tokenData);
  }

  public boolean isIdentifier() {
    return kind == TokenKind.IDENTIFIER;
  }

  public boolean isLiteral() {
    return kind == TokenKind.LITERAL_INT
            || kind == TokenKind.LITERAL_HEXINT
            || kind == TokenKind.LITERAL_LONG
            || kind == TokenKind.LITERAL_HEXLONG
            || kind == TokenKind.LITERAL_REAL
            || kind == TokenKind.LITERAL_REAL_FLOAT
            || kind == TokenKind.LITERAL_STRING;
  }

  public boolean isIdentifier(String identifier) {
    return kind == TokenKind.IDENTIFIER && identifier.equalsIgnoreCase(data);
  }

  public String stringValue() {
    return data != null ? data : "";
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[').append(this.kind);
    if (this.kind.hasPayload()) {
      sb.append(':').append(this.data);
    }
    sb.append(']');
    sb.append('(').append(this.startPos).append(',').append(this.endPos).append(')');
    return sb.toString();
  }

}
