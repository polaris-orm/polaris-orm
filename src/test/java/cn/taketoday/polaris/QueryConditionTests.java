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

package cn.taketoday.polaris;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import cn.taketoday.polaris.model.Gender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/11 00:01
 */
class QueryConditionTests {

  @Test
  void and() {
    cn.taketoday.polaris.DefaultQueryCondition condition = cn.taketoday.polaris.DefaultQueryCondition.isEqualsTo("name", "T");

    StringBuilder sql = new StringBuilder();
    condition.render(sql);
    assertThat(sql.toString()).isEqualTo(" `name` = ?");

    condition.and(cn.taketoday.polaris.DefaultQueryCondition.isNull("age"));

    sql = new StringBuilder();
    condition.render(sql);
    assertThat(sql.toString()).isEqualTo(" `name` = ? AND `age` is null");

    condition.and(
            DefaultQueryCondition.between("age", new Date(), new Date())
    );

    sql = new StringBuilder();
    condition.render(sql);

    System.out.println(sql);

    assertThat(sql.toString()).isEqualTo(" `name` = ? AND `age` BETWEEN ? AND ?");
  }

  @Test
  void composite() {
    NestedQueryCondition condition = cn.taketoday.polaris.QueryCondition.nested(
            cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY")
                    .or(cn.taketoday.polaris.QueryCondition.isEqualsTo("age", 10))
    );

    StringBuilder sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" ( `name` = ? OR `age` = ? )");
    //

    condition.and(
            cn.taketoday.polaris.QueryCondition.nested(
                    cn.taketoday.polaris.QueryCondition.isEqualsTo("gender", Gender.MALE)
                            .and(cn.taketoday.polaris.QueryCondition.of("email", cn.taketoday.polaris.Operator.PREFIX_LIKE, "taketoday"))
            )
    );

    sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" ( `name` = ? OR `age` = ? ) AND ( `gender` = ? AND `email` like concat(?, '%') )");

    //

    condition.and(
            cn.taketoday.polaris.QueryCondition.nested(
                    cn.taketoday.polaris.QueryCondition.isEqualsTo("gender", Gender.MALE)
                            .and(cn.taketoday.polaris.QueryCondition.of("email", cn.taketoday.polaris.Operator.PREFIX_LIKE, "taketoday")
                                    .and(cn.taketoday.polaris.QueryCondition.nested(
                                                    cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY")
                                                            .or(cn.taketoday.polaris.QueryCondition.isEqualsTo("age", 10))
                                            )
                                    )
                            )

            )

    );

    sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" ( `name` = ? OR `age` = ? ) AND ( `gender` = ? AND `email` like concat(?, '%') AND ( `name` = ? OR `age` = ? ) )");

  }

  @Test
  void andExprShouldAssignOnce() {
    var condition = cn.taketoday.polaris.QueryCondition.isEqualsTo("gender", Gender.MALE)
            .and(cn.taketoday.polaris.QueryCondition.of("email", cn.taketoday.polaris.Operator.PREFIX_LIKE, "taketoday"))
            .and(cn.taketoday.polaris.QueryCondition.nested(
                            cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY")
                                    .or(cn.taketoday.polaris.QueryCondition.isEqualsTo("age", 10))
                    )
            );

    StringBuilder sql = new StringBuilder();
    assertThat(condition.render(sql)).isTrue();
    assertThat(sql.toString()).isEqualTo(" `gender` = ? AND ( `name` = ? OR `age` = ? )");
  }

  @Test
  void setParameter() throws SQLException {
    PreparedStatement statement = Mockito.mock(PreparedStatement.class);

    cn.taketoday.polaris.QueryCondition condition = cn.taketoday.polaris.QueryCondition.nested(
            cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY").or(cn.taketoday.polaris.QueryCondition.isEqualsTo("age", 10))
    ).and(cn.taketoday.polaris.QueryCondition.nested(
                    cn.taketoday.polaris.QueryCondition.isEqualsTo("gender", Gender.MALE)
                            .and(
                                    cn.taketoday.polaris.QueryCondition.of("email", Operator.PREFIX_LIKE, "taketoday")
                                            .and(
                                                    cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY")
                                                            .or(cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY"))
                                            )
                            )
            ).and(
                    cn.taketoday.polaris.QueryCondition.isEqualsTo("name", "TODAY7")
                            .or(QueryCondition.isEqualsTo("name", "TODAY8"))
            )
    );

    StringBuilder sql = new StringBuilder();
    condition.render(sql);
    System.out.println(sql);

    condition.setParameter(statement);

    InOrder inOrder = Mockito.inOrder(statement);
    inOrder.verify(statement).setObject(1, "TODAY");
    inOrder.verify(statement).setObject(2, 10);
    inOrder.verify(statement).setObject(3, Gender.MALE);
    inOrder.verify(statement).setObject(4, "taketoday");
    inOrder.verify(statement).setObject(5, "TODAY");
    inOrder.verify(statement).setObject(6, "TODAY");

    inOrder.verify(statement).setObject(7, "TODAY7");
    inOrder.verify(statement).setObject(8, "TODAY8");

  }

}