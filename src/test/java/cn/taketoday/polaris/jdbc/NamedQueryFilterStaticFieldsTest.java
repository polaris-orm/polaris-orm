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

package cn.taketoday.polaris.jdbc;

import com.google.common.primitives.Longs;

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.Comparator;

import cn.taketoday.polaris.jdbc.JdbcConnection;
import cn.taketoday.polaris.jdbc.NamedQuery;
import cn.taketoday.polaris.jdbc.RepositoryManager;

import static org.assertj.core.api.Assertions.assertThat;

public class NamedQueryFilterStaticFieldsTest {

  @Rule
  public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
          .withInitialSql(
                  "CREATE TABLE TEST(ver int primary key); INSERT INTO TEST VALUES(1);")
          .build();

  static class Entity {
    public long ver;
    public static final Comparator<Entity> VER = new Comparator<Entity>() {
      @Override
      public int compare(final Entity o1, final Entity o2) {
        return Longs.compare(o1.ver, o2.ver);
      }
    };
  }

  @Test
  public void dontTouchTheStaticFieldTest() throws Exception {
    final cn.taketoday.polaris.jdbc.RepositoryManager dataBase = new RepositoryManager(databaseRule.getDataSource());
    try (final JdbcConnection connection = dataBase.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM TEST WHERE ver=1")) {
      final Entity entity = query.fetchFirst(Entity.class);
      assertThat(entity.ver).isEqualTo(1L);
    }
  }
}
