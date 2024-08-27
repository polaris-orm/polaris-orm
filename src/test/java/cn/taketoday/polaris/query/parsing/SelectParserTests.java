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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/8/20 23:06
 */
class SelectParserTests {

  @Test
  void test() {
    String sql = """
            SELECT `category`, `content`, `copyright`, `cover`, `create_at`, `id`, `markdown`, `password`, `pv`, `status`, `summary`, `title`, `update_at`, `uri`
            FROM article WHERE article.`category` = #category and  (`title` like @q OR `content` like '%#q%' ) and (status = :status) and 1=1
            and create_at between :create_at[0] and :create_at[1] or status is not null and status not like 's' and TRIM(status) = 'YHJ'
            or status not in (?, :status, 3, 4, '5', :`d`)  or find_in_set(status, 'd') and status in(1, true, false) and status in(func(status, 'd'))
            and binary status = 1
            and status in ((select 1 where 1 = 1 order by col desc), 2)
            and status in (select 1, 2)
            and status in (select 1 where col = 1 and 1 = 1)
            and status like binary '/%/_%_' ESCAPE '/'
            and status rlike binary '/%/_%_' ESCAPE '/'
            and status REGEXP binary '/%/_%_' ESCAPE '/'
            
            order by update_at DESC, create_at DESC LIMIT 20""";

    //  and status in ((select 1), 2)

    SelectExpression expression = SelectParser.parse(sql);

    String render = expression.render();

    System.out.println(render);
  }

  @Test
  void syntaxError() {
    assertThatThrownBy(() -> SelectParser.parse("Update article set status = 1 where status = 2"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [Update article set status = 1 where status = 2]: Not a select statement");

    assertThatThrownBy(() -> SelectParser.parse("SELECT * FROM article WHERE"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [SELECT * FROM article WHERE] @27: Where clause not found");

    assertThatThrownBy(() -> SelectParser.parse("SELECT * FROM article WHERE a"))
            .isInstanceOf(ParsingException.class)
            .hasMessage("Statement [SELECT * FROM article WHERE a] @29: Syntax error, operator token expected");




  }

}