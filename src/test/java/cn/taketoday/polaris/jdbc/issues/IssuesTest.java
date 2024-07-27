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

package cn.taketoday.polaris.jdbc.issues;

import org.hsqldb.jdbcDriver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import cn.taketoday.polaris.jdbc.JdbcConnection;
import cn.taketoday.polaris.jdbc.PersistenceException;
import cn.taketoday.polaris.jdbc.RepositoryManager;
import cn.taketoday.polaris.jdbc.issues.pojos.Issue1Pojo;
import cn.taketoday.polaris.jdbc.issues.pojos.KeyValueEntity;
import lombok.Setter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA. User: lars Date: 10/17/11 Time: 9:02 PM This class
 * is to test for reported issues.
 */
@RunWith(Parameterized.class)
public class IssuesTest {

  @Parameterized.Parameters(name = "{index} - {4}")
  public static Collection<Object[]> getData() {
    return Arrays.asList(
            new Object[][] {
                    { null, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "", "H2 test" },
                    { new jdbcDriver(), "jdbc:hsqldb:mem:testmemdb", "SA", "", "HyperSQL DB test" }
            });
  }

  private RepositoryManager manager;

  private String url;

  private String user;

  private String pass;

  public IssuesTest(Driver driverToRegister, String url, String user, String pass, String testName) {
    if (driverToRegister != null) {
      try {
        DriverManager.registerDriver(driverToRegister);
      }
      catch (SQLException e) {
        throw new RuntimeException("could not register driver '" + driverToRegister.getClass().getName() + "'", e);
      }
    }

    this.manager = new RepositoryManager(url, user, pass);

    this.url = url;
    this.user = user;
    this.pass = pass;

    if ("HyperSQL DB test".equals(testName)) {
      manager.createNamedQuery("set database sql syntax MSS true").executeUpdate();
    }
  }

  @Test
  public void testSetterPriority() {
    RepositoryManager sql2o = new RepositoryManager(url, user, pass);
    Issue1Pojo pojo = sql2o.createNamedQuery("select 1 val from (values(0))")
            .fetchFirst(Issue1Pojo.class);

    assertEquals(2, pojo.val);
  }

  @Test
  public void testForFieldDoesNotExistException() {
    RepositoryManager sql2o = new RepositoryManager(url, user, pass);

    try {
      KeyValueEntity pojo = sql2o.createNamedQuery("select 1 id, 'something' foo from (values(0))").fetchFirst(
              KeyValueEntity.class);
    }
    catch (PersistenceException ex) {
      assertTrue(ex.getMessage().contains("Could not map"));
    }
  }

  @Setter
  public static class Issue5POJO {
    public int id;
    public int val;
  }

  public static class Issue5POJO2 {
    public int id;
    public int val;

    public int getVal() {
      return val;
    }

    public void setVal(int val) {
      this.val = val;
    }
  }

  @Test
  public void testForNullToSimpeType() {
    manager.createNamedQuery("create table issue5table(id int identity primary key, val integer)").executeUpdate();

    manager.createNamedQuery("insert into issue5table(val) values (:val)")
            .addParameter("val", (Object) null).executeUpdate();

    List<Issue5POJO> list1 = manager.createNamedQuery("select * from issue5table")
            .fetch(Issue5POJO.class);

    List<Issue5POJO2> list2 = manager.createNamedQuery("select * from issue5table")
            .fetch(Issue5POJO2.class);

    assertEquals(1, list1.size());
    assertEquals(1, list2.size());
    assertEquals(0, list1.get(0).val);
    assertEquals(0, list2.get(0).getVal());
  }

  @Test
  public void testForLabelErrorInHsqlDb() {
    manager.createNamedQuery("create table issue9test (id integer identity primary key, val varchar(50))").executeUpdate();

    String insertSql = "insert into issue9test(val) values (:val)";
    manager.createNamedQuery(insertSql).addParameter("val", "something").executeUpdate();
    manager.createNamedQuery(insertSql).addParameter("val", "something else").executeUpdate();
    manager.createNamedQuery(insertSql).addParameter("val", "something third").executeUpdate();

    List<Issue9Pojo> pojos = manager.createNamedQuery("select id, val theVal from issue9Test").fetch(Issue9Pojo.class);

    assertEquals(3, pojos.size());
    assertEquals("something", pojos.get(0).theVal);

  }

  public static enum WhatEverEnum {
    VAL, ANOTHER_VAL;
  }

  @Test
  public void testForNullPointerExceptionInAddParameterMethod() {
    manager.createNamedQuery("create table issue11test (id integer identity primary key, val varchar(50), adate datetime)")
            .executeUpdate();

    String insertSql = "insert into issue11test (val, adate) values (:val, :date)";
    manager.createNamedQuery(insertSql)
            .addParameter("val", WhatEverEnum.VAL)
            .addParameter("date", new Date())
            .executeUpdate();
    Date dtNull = null;
    WhatEverEnum enumNull = null;

    manager.createNamedQuery(insertSql).addParameter("val", enumNull)
            .addParameter("date", dtNull).executeUpdate();
  }

  @Test
  public void testErrorWhenFieldDoesntExist() {

    class LocalPojo {
      private long id;
      private String strVal;

      public long getId() {
        return id;
      }

      public String getStrVal() {
        return strVal;
      }
    }

    String createQuery = "create table testErrorWhenFieldDoesntExist(id_val integer primary key, str_val varchar(100))";

    try (JdbcConnection connection = manager.open()) {
      connection.createNamedQuery(createQuery).executeUpdate();

      String insertSql = "insert into testErrorWhenFieldDoesntExist(id_val, str_val) values (:val1, :val2)";
      connection.createNamedQuery(insertSql)
              .addParameter("val1", 1)
              .addParameter("val2", "test")
              .executeUpdate();

      Exception ex = null;
      try {
        // This is expected to fail to map columns and throw an exception.
        LocalPojo p = connection.createNamedQuery("select * from testErrorWhenFieldDoesntExist")
                .fetchFirst(LocalPojo.class);
      }
      catch (Exception e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

    }
  }

  public static class Issue9Pojo {
    public int id;
    public String theVal;
  }

  static class ThePojo {
    public int id;
    public String name;
  }

  @Test
  public void testIndexOutOfRangeExceptionWithMultipleColumnsWithSameName() {
    String sql = "select 11 id, 'something' name, 'something else' name from (values(0))";

    try (JdbcConnection connection = manager.open()) {
      ThePojo p = connection.createNamedQuery(sql).fetchFirst(ThePojo.class);

      assertEquals(11, p.id);
      assertEquals("something else", p.name);
    }
  }

  static class TheIgnoreSqlCommentPojo {
    public int id;
    public int intval;
    public String strval;
  }

  @Test
  public void testIgnoreSqlComments() {

    String createSql = "create table testIgnoreSqlComments(id integer primary key, intval integer, strval varchar(100))";

    String insertQuery = "insert into testIgnoreSqlComments (id, intval, strval)\n " +
            "-- It's a comment!\n" +
            "values (:id, :intval, :strval);";

    String fetchQuery = "select id, intval, strval\n" +
            "-- a 'comment'\n" +
            "from testIgnoreSqlComments\n" +
            "/* and, it's another type of comment!*/" +
            "where intval = :param";

    try (JdbcConnection connection = manager.open()) {
      connection.createNamedQuery(createSql).executeUpdate();

      for (int idx = 0; idx < 100; idx++) {
        int intval = idx % 10;
        connection.createNamedQuery(insertQuery)
                .addParameter("id", idx)
                .addParameter("intval", intval)
                .addParameter("strval", "teststring" + idx)
                .executeUpdate();
      }

      List<TheIgnoreSqlCommentPojo> resultList = connection.createNamedQuery(fetchQuery)
              .addParameter("param", 5)
              .fetch(TheIgnoreSqlCommentPojo.class);

      assertEquals(10, resultList.size());
    }
  }

  static class Pojo {
    public int id;
    public String val1;
  }

  @Test
  public void testIssue166OneCharacterParameterFail() {
    try (JdbcConnection connection = manager.open()) {
      connection.createNamedQuery("create table testIssue166OneCharacterParameterFail(id integer, val varchar(10))")
              .executeUpdate();

      // This because of the :v parameter.
      connection.createNamedQuery("insert into testIssue166OneCharacterParameterFail(id, val) values(:id, :v)")
              .addParameter("id", 1)
              .addParameter("v", "foobar")
              .executeUpdate();

      int cnt = connection.createNamedQuery("select count(*) from testIssue166OneCharacterParameterFail where id = :p")
              .addParameter("p", 1)
              .fetchScalar(Integer.class);

      assertEquals(1, cnt);
    }
  }

  @Test
  public void testIssue149NullPointerWhenUsingWrongParameterName() {

    try (JdbcConnection connection = manager.open()) {
      connection.createNamedQuery("create table issue149 (id integer primary key, val varchar(20))").executeUpdate();
      connection.createNamedQuery("insert into issue149(id, val) values (:id, :val)")
              .addParameter("id", 1)
              .addParameter("asdsa", "something") // spell-error in parameter name
              .executeUpdate();

      Assert.fail("Expected exception!!");
    }
    catch (PersistenceException ex) {
      // awesome!
    }
    catch (Throwable t) {
      Assert.fail("A " + t.getClass().getName() + " was thrown, but An " + PersistenceException.class.getName() + " was expected");
    }
  }
}
