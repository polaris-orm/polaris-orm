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

import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public final class LazyTable implements AutoCloseable {

  private String name;

  private ResultSetIterable<Row> rows;

  private List<Column> columns;

  public String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  public Iterable<Row> rows() {
    return rows;
  }

  void setRows(ResultSetIterable<Row> rows) {
    this.rows = rows;
  }

  public List<Column> columns() {
    return columns;
  }

  void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  @Override
  public void close() {
    this.rows.close();
  }
}
