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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import cn.taketoday.polaris.jdbc.JdbcConnection;
import cn.taketoday.polaris.jdbc.RepositoryManager;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: dimzon Date: 4/29/14 Time: 10:05 PM
 */
public class ConnectionTest {
  @Test
  public void test_createQueryWithParams() throws Throwable {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    when(jdbcConnection.isClosed()).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(jdbcConnection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(ps);

    cn.taketoday.polaris.jdbc.RepositoryManager operations = new cn.taketoday.polaris.jdbc.RepositoryManager(dataSource);

    operations.setGeneratedKeys(false);
    cn.taketoday.polaris.jdbc.JdbcConnection cn = new cn.taketoday.polaris.jdbc.JdbcConnection(operations, operations.getDataSource(), false);
    cn.createNamedQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildStatement();

    verify(dataSource, times(1)).getConnection();
    verify(jdbcConnection).isClosed();
    verify(jdbcConnection, times(1)).prepareStatement("select ? name, ? age");
    verify(ps, times(1)).setString(1, "Dmitry Alexandrov");
    verify(ps, times(1)).setInt(2, 35);
    // check statement still alive
    verify(ps, never()).close();

  }

  @SuppressWarnings("serial")
  public class MyException extends RuntimeException { }

  @Test
  public void test_createQueryWithParamsThrowingException() throws Throwable {
    DataSource dataSource = mock(DataSource.class);
    Connection jdbcConnection = mock(Connection.class);
    when(jdbcConnection.isClosed()).thenReturn(false);
    when(dataSource.getConnection()).thenReturn(jdbcConnection);
    PreparedStatement ps = mock(PreparedStatement.class);
    doThrow(MyException.class).when(ps).setInt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    when(jdbcConnection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(ps);

    cn.taketoday.polaris.jdbc.RepositoryManager manager = new RepositoryManager(dataSource);
    manager.setGeneratedKeys(false);
    try (JdbcConnection cn = manager.open()) {
      cn.createNamedQueryWithParams("select :p1 name, :p2 age", "Dmitry Alexandrov", 35).buildStatement();
      fail("exception not thrown");
    }
    catch (MyException ex) {
      // as designed
    }
    verify(dataSource, times(1)).getConnection();
    verify(jdbcConnection, atLeastOnce()).isClosed();
    verify(jdbcConnection, times(1)).prepareStatement("select ? name, ? age");
    verify(ps, times(1)).setInt(2, 35);
    // check statement was closed
    verify(ps, times(1)).close();
  }
}
