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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 23:06
 */
class SelectParserTests {

  @Test
  void test() {
    String sql = """
            SELECT `category`, `content`, `copyright`, `cover`, `create_at`, `id`, `markdown`, `password`, `pv`, `status`, `summary`, `title`, `update_at`, `uri`
            FROM article WHERE `category` = #category and  (`title` like @q OR `content` like '%#q%' ) and status = :status
               and create_at between :create_at[0] and :create_at[1]
            order by update_at DESC, create_at DESC LIMIT 20""";

    SelectExpression expression = SelectParser.parse(sql);

    String render = expression.render();

    System.out.println(render);
  }

}