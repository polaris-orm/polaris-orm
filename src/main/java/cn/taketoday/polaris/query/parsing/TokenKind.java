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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/16 17:08
 */
enum TokenKind {

  LITERAL_INT,

  LITERAL_LONG,

  LITERAL_HEXINT,

  LITERAL_HEXLONG,

  LITERAL_STRING,

  LITERAL_REAL,

  LITERAL_REAL_FLOAT,

  LPAREN("("),

  RPAREN(")"),

  COMMA(","),

  IDENTIFIER,

  COLON(":"),

  HASH("#"),

  RSQUARE("]"),

  LSQUARE("["),

  DOT("."),

  PLUS("+"),

  STAR("*"),

  MINUS("-"),

  QMARK("?"),

  GE(">="),

  GT(">"),

  LE("<="),

  LT("<"),

  EQ("="),

  NE("<>"),

  LIKE_PREFIX("%"),

  VARIABLE_REF("@");

  final char[] tokenChars;

  private final boolean hasPayload;  // is there more to this token than simply the kind

  TokenKind(String tokenString) {
    this.tokenChars = tokenString.toCharArray();
    this.hasPayload = (this.tokenChars.length == 0);
  }

  TokenKind() {
    this("");
  }

  @Override
  public String toString() {
    return (name() + (this.tokenChars.length != 0 ? "(" + new String(this.tokenChars) + ")" : ""));
  }

  public boolean hasPayload() {
    return this.hasPayload;
  }

  public int getLength() {
    return this.tokenChars.length;
  }

}
