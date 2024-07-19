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

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author aldenquimby@gmail.com
 */
@SuppressWarnings("serial")
public class PerformanceTestList extends ArrayList<PerformanceTestBase> {
  public void run(int iterations) throws SQLException {
    // initialize
    for (PerformanceTestBase test : this) {
      test.initialize();
    }

    final Random rand = new Random();

    for (int i = 1; i <= iterations; i++) {
      Iterable<PerformanceTestBase> sortedByRandom = orderBy(this, input -> rand.nextInt());

      for (PerformanceTestBase test : sortedByRandom) {
        final Stopwatch watch = test.getWatch();
        watch.start();
        test.run(i);
        watch.stop();
      }
    }

    // close up
    for (PerformanceTestBase test : this) {
      test.close();
    }
  }

  public void printResults(String heading) {
    Iterable<PerformanceTestBase> sortedByTime = orderBy(this, input -> input.getWatch().elapsed(TimeUnit.MILLISECONDS));

    System.out.println(heading + " Results");
    System.out.println("-------------------------");

    PerformanceTestBase fastest = null;

    for (PerformanceTestBase test : sortedByTime) {
      long millis = test.getWatch().elapsed(TimeUnit.MILLISECONDS);
      String testName = test.getName().replaceAll(heading + "$", "");
      if (fastest == null) {
        fastest = test;
        System.out.println(String.format("%s took %dms", testName, millis));
      }
      else {
        long fastestMillis = fastest.getWatch().elapsed(TimeUnit.MILLISECONDS);
        double percentSlower = (double) (millis - fastestMillis) / fastestMillis * 100;
        System.out.println(String.format("%s took %dms (%.2f%% slower)", testName, millis, percentSlower));
      }
    }
  }

  private static <T> Iterable<T> orderBy(Iterable<T> iterable, Function<T, ? extends Comparable> selector) {
    return Ordering.natural().onResultOf(selector).sortedCopy(iterable);
  }
}
