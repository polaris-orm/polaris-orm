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

package cn.taketoday.polaris.transaction;

/**
 * 事物隔离级别
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public enum Isolation {

  /**
   * 使用底层数据存储的默认隔离级别。
   * <p>
   * 所有其他级别均对应于 JDBC 隔离级别。
   *
   * @see java.sql.Connection
   */
  DEFAULT(TransactionConfig.ISOLATION_DEFAULT),

  /**
   * 表示可能发生脏读、不可重复读和幻读的常量。
   * <p>
   * 此级别允许一个事务更改的行在提交该行的任何更改之前被另一个事务读取（“脏读”）。
   * 如果任何更改被回滚，则第二个事务将检索到无效行。
   *
   * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
   */
  READ_UNCOMMITTED(TransactionConfig.ISOLATION_READ_UNCOMMITTED),

  /**
   * 表示阻止脏读；可能发生不可重复读和幻读。
   * <p>
   * 此级别仅禁止事务读取其中有未提交更改的行。
   *
   * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
   */
  READ_COMMITTED(TransactionConfig.ISOLATION_READ_COMMITTED),

  /**
   * 表示防止脏读和不可重复读；可能会发生幻读。
   * <p>
   * 此级别禁止事务读取包含未提交更改的行，还禁止出现一个事务读取一行，第二个事务更改该行，
   * 第一个事务重新读取该行，第二次获得不同值的情况（“不可重复读”）。
   *
   * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
   */
  REPEATABLE_READ(TransactionConfig.ISOLATION_REPEATABLE_READ),

  /**
   * 表示阻止脏读、不可重复读和幻读。
   * <p>
   * 此级别包括 REPEATABLE_READ 中的禁止，并进一步禁止以下情况：
   * 一个事务读取满足 WHERE 条件的所有行，第二个事务插入满足该 WHERE 条件的行，
   * 第一个事务针对相同条件重新读取，在第二次读取中检索额外的 “phantom” 行。
   *
   * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
   */
  SERIALIZABLE(TransactionConfig.ISOLATION_SERIALIZABLE);

  private final int value;

  Isolation(int value) {
    this.value = value;
  }

  public int value() {
    return this.value;
  }

}
