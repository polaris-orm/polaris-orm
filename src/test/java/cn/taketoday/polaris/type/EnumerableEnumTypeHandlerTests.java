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

package cn.taketoday.polaris.type;

import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/1 23:02
 */
class EnumerableEnumTypeHandlerTests {

  enum Gender implements Enumerable<Integer> {

    MALE(1, "男"),
    FEMALE(0, "女");

    private final int value;
    private final String desc;

    Gender(int value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    @Override
    public Integer getValue() {
      return value;
    }

    @Override
    public String getDescription() {
      return desc;
    }

  }

  enum StringValue implements Enumerable {

    TEST1,
    TEST2;
  }

  @Data
  static class UserModel {

    Gender gender;

    StringValue stringValue;
  }

  @Test
  void test() {
    Class<?> valueType = EnumerableEnumTypeHandler.getValueType(Gender.class);
    assertThat(valueType).isNotNull().isEqualTo(Integer.class);

    assertThat(EnumerableEnumTypeHandler.getValueType(StringValue.class))
            .isEqualTo(String.class);

  }

  @Test
  void setParameter() throws SQLException {
    TypeHandler<Integer> delegate = mock(TypeHandler.class);
    PreparedStatement statement = mock(PreparedStatement.class);

    TypeHandlerManager registry = new TypeHandlerManager();
    registry.register(Integer.class, delegate);

    var handler = new EnumerableEnumTypeHandler<>(Gender.class, registry);

    handler.setParameter(statement, 1, null);
    handler.setParameter(statement, 1, Gender.MALE);

    verify(delegate).setParameter(statement, 1, null);
    verify(delegate).setParameter(statement, 1, Gender.MALE.getValue());
  }

  @Test
  void getResult() throws SQLException {
    TypeHandler<Integer> delegate = mock(TypeHandler.class);
    ResultSet resultSet = mock(ResultSet.class);
    CallableStatement callableStatement = mock(CallableStatement.class);

    given(delegate.getResult(resultSet, 1)).willReturn(Gender.MALE.getValue());
    given(delegate.getResult(resultSet, "gender")).willReturn(Gender.FEMALE.getValue());
    given(delegate.getResult(callableStatement, 1)).willReturn(Gender.FEMALE.getValue());

    TypeHandlerManager registry = new TypeHandlerManager();
    registry.register(Integer.class, delegate);

    var handler = new EnumerableEnumTypeHandler<>(Gender.class, registry);

    assertThat(handler.getResult(resultSet, 1)).isEqualTo(Gender.MALE);
    assertThat(handler.getResult(resultSet, "gender")).isEqualTo(Gender.FEMALE);
    assertThat(handler.getResult(callableStatement, 1)).isEqualTo(Gender.FEMALE);

    verify(delegate).getResult(resultSet, 1);
    verify(delegate).getResult(resultSet, "gender");
    verify(delegate).getResult(callableStatement, 1);

  }
}