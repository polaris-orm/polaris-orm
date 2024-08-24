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

package cn.taketoday.polaris.query.parsing.ast;

import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 22:54
 */
public class SelectNode implements Expression {

  private final String select;

  @Nullable
  private final WhereNode whereNode;

  @Nullable
  private final String other;

  public SelectNode(String select, @Nullable WhereNode whereNode, @Nullable String other) {
    this.select = select;
    this.whereNode = whereNode;
    this.other = other;
  }

  public void render(StringBuilder selectSQL) {
    selectSQL.append(select);
    if (whereNode != null) {
      whereNode.render(selectSQL);
    }
    if (other != null) {
      selectSQL.append(" ").append(other);
    }
  }

}
