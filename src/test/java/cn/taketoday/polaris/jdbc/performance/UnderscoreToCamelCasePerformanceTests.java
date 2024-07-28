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

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import cn.taketoday.polaris.util.StringUtils;

/**
 * @author aldenquimby@gmail.com
 */
public class UnderscoreToCamelCasePerformanceTests {
  private static final int ITERATIONS = 1000;

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Test
  public void run() throws SQLException {
    PerformanceTestList tests = new PerformanceTestList();
    tests.add(new Sql2oUnderscoreToCamelCase());
    tests.add(new GuavaUnderscoreToCamelCase());

    tests.run(ITERATIONS);
    tests.printResults("");
  }

  private List<String> toConvert = List.of("my_string_variable", "string", "my_really_long_string_variable_name",
          "my_string2_with_numbers_4", "my_string_with_MixED_CaSe",
          "", " ", "\t", "\n\n");

  //----------------------------------------
  //          performance tests
  // ---------------------------------------

  class Sql2oUnderscoreToCamelCase extends PerformanceTestBase {
    @Override
    public void init() { }

    @Override
    public void close() { }

    @Override
    public void run(int input) {
      for (String s : toConvert) {
        StringUtils.underscoreToCamelCase(s);
      }
    }
  }

  class GuavaUnderscoreToCamelCase extends PerformanceTestBase {
    @Override
    public void init() { }

    @Override
    public void close() { }

    @Override
    public void run(int input) {
      final CaseFormat lowerCamel = CaseFormat.LOWER_CAMEL;
      final CaseFormat lowerUnderscore = CaseFormat.LOWER_UNDERSCORE;
      for (String s : toConvert) {
        lowerUnderscore.to(lowerCamel, s);
      }
    }
  }
}
