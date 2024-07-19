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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.polaris.jdbc.parsing.ParameterIndexHolder;
import cn.taketoday.polaris.jdbc.parsing.QueryParameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayParametersTest {

  @Test
  public void testUpdateParameterNamesToIndexes() {
    final ImmutableList<Integer> of = ImmutableList.of(3, 5);
    ArrayList<ArrayParameters.ArrayParameter> arrayParametersSortedAsc =
            listOf(new ArrayParameters.ArrayParameter(6, 3));

    QueryParameter parameter = new QueryParameter("paramName", ParameterIndexHolder.valueOf(of));

    ImmutableMap<String, QueryParameter> paramName2 = ImmutableMap.of("paramName", parameter);
    Map<String, QueryParameter> paramName =
            ArrayParameters.updateMap(Maps.newHashMap(paramName2), arrayParametersSortedAsc);

    assertEquals(ImmutableMap.of("paramName", new QueryParameter("paramName", ParameterIndexHolder.valueOf(ImmutableList.of(3, 5)))),
            paramName);

    parameter = new QueryParameter("paramName", ParameterIndexHolder.valueOf(ImmutableList.of(3, 7)));

    ImmutableMap<String, QueryParameter> paramName1 = ImmutableMap.of("paramName", parameter);

    assertEquals(
            ImmutableMap.of("paramName", new QueryParameter("paramName", ParameterIndexHolder.valueOf(ImmutableList.of(3, 9)))),
            ArrayParameters.updateMap(
                    Maps.newHashMap(paramName1),
                    listOf(new ArrayParameters.ArrayParameter(6, 3))));
  }

  static ArrayList<ArrayParameters.ArrayParameter> listOf(
          ArrayParameters.ArrayParameter... parameter) {
    ArrayList<ArrayParameters.ArrayParameter> parameters = new ArrayList<>();
    Collections.addAll(parameters, parameter);
    return parameters;
  }

  @Test
  public void testComputeNewIndex() {

    assertEquals(
            2,
            ArrayParameters.computeNewIndex(
                    2,
                    listOf(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            3,
            ArrayParameters.computeNewIndex(
                    3,
                    listOf(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            8,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5),
                            new ArrayParameters.ArrayParameter(4, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5),
                            new ArrayParameters.ArrayParameter(5, 5))));
  }

  @Test
  public void testUpdateQueryWithArrayParameters() {
    assertEquals(
            "SELECT * FROM user WHERE id IN(?,?,?,?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(1, 5))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    new ArrayList<ArrayParameters.ArrayParameter>()));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(1, 0))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(1, 1))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(2, 2))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?) AND name = ?",
                    listOf(new ArrayParameters.ArrayParameter(2, 2))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 3),
                            new ArrayParameters.ArrayParameter(3, 3))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 5),
                            new ArrayParameters.ArrayParameter(3, 3))));
  }

}
