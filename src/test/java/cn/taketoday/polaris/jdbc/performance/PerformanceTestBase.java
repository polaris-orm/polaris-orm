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

package cn.taketoday.polaris.jdbc.performance;

import com.google.common.base.Stopwatch;

import java.sql.SQLException;
import java.util.function.Function;

/**
 * Basically a {@link Runnable} with an Integer input.
 */
public abstract class PerformanceTestBase implements Function<Integer, Void>, AutoCloseable {
  private Stopwatch watch = Stopwatch.createUnstarted();

  public Void apply(Integer input) {
    try {
      run(input);
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void initialize() {
    watch.reset();
    init();
  }

  public abstract void init();

  public abstract void run(int input) throws SQLException;

  public abstract void close();

  String getName() {
    return getClass().getSimpleName();
  }

  Stopwatch getWatch() {
    return watch;
  }
}
