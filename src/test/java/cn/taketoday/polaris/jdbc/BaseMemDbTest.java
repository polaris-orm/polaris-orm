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

import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import cn.taketoday.polaris.jdbc.JdbcConnection;
import cn.taketoday.polaris.jdbc.RepositoryManager;

/**
 * Created by lars on 01.11.14.
 */
public abstract class BaseMemDbTest {

  public enum DbType {
    H2("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", ""),
    HyperSQL("jdbc:hsqldb:mem:testmemdb", "SA", "");

    public final String url;
    public final String user;
    public final String pass;

    DbType(String url, String user, String pass) {
      this.url = url;
      this.user = user;
      this.pass = pass;
    }
  }

  @Parameterized.Parameters(name = "{index} - {2}")
  public static Collection<Object[]> getData() {
    return Arrays.asList(new Object[][] {
            { DbType.H2, "H2 test" },
            { DbType.HyperSQL, "HyperSQL Test" }
    });
  }

  protected final DbType dbType;
  protected final RepositoryManager repositoryManager;

  public BaseMemDbTest(DbType dbType, String testName) {
    this.dbType = dbType;
    this.repositoryManager = new RepositoryManager(dbType.url, dbType.user, dbType.pass);

    if (dbType == DbType.HyperSQL) {
      try (JdbcConnection con = repositoryManager.open()) {
        con.createNamedQuery("set database sql syntax MYS true").executeUpdate();
      }
    }
  }
}
