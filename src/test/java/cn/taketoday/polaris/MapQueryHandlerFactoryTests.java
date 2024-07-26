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

import java.util.ArrayList;
import java.util.Map;

import cn.taketoday.polaris.platform.Platform;
import cn.taketoday.polaris.model.UserModel;
import cn.taketoday.polaris.sql.Restriction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/4/10 17:39
 */
class MapQueryHandlerFactoryTests {

  final DefaultEntityMetadataFactory metadataFactory = new DefaultEntityMetadataFactory();
  final EntityMetadata entityMetadata = metadataFactory.getEntityMetadata(UserModel.class);

  @Test
  void createCondition() {
    MapQueryHandlerFactory factory = new MapQueryHandlerFactory();
    ConditionStatement condition = factory.createCondition(Map.of("name", "TODAY"));
    assertThat(condition).isNotNull();

    ArrayList<Restriction> restrictions = new ArrayList<>();
    condition.renderWhereClause(entityMetadata, restrictions);
    assertThat(restrictions).hasSize(1).contains(Restriction.equal("name"));

    //
    assertThat(factory.createCondition(null)).isNull();
  }

  @Test
  void createQuery() {
    MapQueryHandlerFactory factory = new MapQueryHandlerFactory();
    QueryStatement queryStatement = factory.createQuery(Map.of("name", "TODAY"));
    assertThat(queryStatement).isNotNull();

    StatementSequence sequence = queryStatement.render(entityMetadata);
    assertThat(sequence.toStatementString(Platform.forClasspath())).endsWith("FROM t_user WHERE `name` = ?");

    assertThat(factory.createQuery(null)).isNull();
  }

}