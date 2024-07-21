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

package cn.taketoday.polaris.jdbc.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.jdbc.datasource.ConnectionSource;
import cn.taketoday.polaris.transaction.DefaultTransaction;
import cn.taketoday.polaris.transaction.Transaction;
import cn.taketoday.polaris.transaction.TransactionConfig;
import cn.taketoday.polaris.transaction.TransactionManager;
import cn.taketoday.transaction.TransactionException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0
 */
public class JdbcTransactionManager implements TransactionManager {

  private final ConnectionSource connectionSource;

  public JdbcTransactionManager(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public Transaction getTransaction(@Nullable TransactionConfig config) throws TransactionException {
    if (config == null) {
      config = TransactionConfig.forDefaults();
    }
    return new DefaultTransaction(config, connectionSource);
  }

}
