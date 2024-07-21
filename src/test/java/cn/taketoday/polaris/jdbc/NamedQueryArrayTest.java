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

import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.db.EmbeddedDatabaseRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zapodot
 */
public class NamedQueryArrayTest {

  private static class Foo {
    private int bar;
  }

  @Rule
  public EmbeddedDatabaseRule databaseRule = EmbeddedDatabaseRule.builder()
          .withMode(EmbeddedDatabaseRule.CompatibilityMode.Oracle)
          .withInitialSql("CREATE TABLE FOO(BAR int PRIMARY KEY); INSERT INTO FOO VALUES(1); INSERT INTO FOO VALUES(2)")
          .build();

  @Test
  public void arrayTest() throws Exception {
    final RepositoryManager database = new RepositoryManager(databaseRule.getDataSource());
    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> foos = query.addParameters("bars", 1, 2)
              .fetch(Foo.class);

      assertThat(foos.size()).isEqualTo(2);
    }
  }

  @Test
  public void emptyArrayTest() throws Exception {
    final RepositoryManager database = new RepositoryManager(databaseRule.getDataSource());

    try (final JdbcConnection connection = database.open();
            final NamedQuery query = connection.createNamedQuery("SELECT * FROM FOO WHERE BAR IN (:bars)")) {

      final List<Foo> noFoos = query.addParameters("bars")
              .fetch(Foo.class);
      assertThat(noFoos.size()).isZero();
    }
  }
}
