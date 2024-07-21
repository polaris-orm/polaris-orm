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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.sql.Connection;

import javax.sql.DataSource;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test to check if the autoCommit state has been reset upon close
 */
@Disabled
public class ConnectionTransactionTest {

  @Test
  public void beginTransaction() throws Exception {
    final DataSource dataSource = mock(DataSource.class);
    final Connection connectionMock = mock(Connection.class);

    // mocked behaviour
    when(dataSource.getConnection()).thenReturn(connectionMock);
    when(connectionMock.getAutoCommit()).thenReturn(true);
    when(connectionMock.isClosed()).thenReturn(true);

    RepositoryManager manager = new RepositoryManager(dataSource);

    try (JdbcConnection connection = manager.beginTransaction()) {
      connection.commit();
    }

    // Verifications
    verify(dataSource).getConnection();
    verify(connectionMock, atLeastOnce()).getAutoCommit();
    verify(connectionMock, atLeastOnce()).setTransactionIsolation(ArgumentMatchers.anyInt());
    verify(connectionMock, times(1)).isClosed();
    verify(connectionMock, times(1)).close();
    verifyNoMoreInteractions(connectionMock, dataSource);
  }
}
