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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.Nullable;

/**
 * TypeHandler for Enumerable
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2022/8/1 22:15
 */
public class EnumerableEnumTypeHandler<V> implements TypeHandler<Enumerable<V>> {

  private final Class<? extends Enumerable<V>> type;

  private final TypeHandler<V> delegate;

  public EnumerableEnumTypeHandler(Class<? extends Enumerable<V>> type, TypeHandlerManager registry) {
    Assert.notNull(type, "Type argument is required");
    this.type = type;
    Class<V> valueType = getValueType(type);
    this.delegate = registry.getTypeHandler(valueType);
  }

  @SuppressWarnings("unchecked")
  static <V> Class<V> getValueType(Class<?> type) {
    Class<?> valueType = resolveGeneric(type);
    if (valueType == null || valueType == Object.class) {
      Method getValue = findValueMethod(type);
      if (getValue != null) {
        valueType = getValue.getReturnType();
      }
      if (valueType == null || valueType == Object.class) {
        valueType = String.class;// fallback to Enum#name()
      }
    }
    return (Class<V>) valueType;
  }

  @Nullable
  static Method findValueMethod(Class<?> type) {
    for (Method declaredMethod : type.getDeclaredMethods()) {
      if (declaredMethod.getName().equals("getValue")
              && declaredMethod.getParameterCount() == 0
              && declaredMethod.getReturnType() != Object.class) {
        return declaredMethod;
      }
    }
    return null;
  }

  @Nullable
  static Class<?> resolveGeneric(Class<?> type) {
    Type[] genericInterfaces = type.getGenericInterfaces();
    for (Type genericInterface : genericInterfaces) {
      if (genericInterface instanceof ParameterizedType pType) {
        Type[] typeArguments = pType.getActualTypeArguments();
        if (typeArguments.length == 1 && typeArguments[0] instanceof Class<?> valueType) {
          return valueType;
        }
      }
    }
    return null;
  }

  @Override
  public void setParameter(PreparedStatement ps,
          int parameterIndex, @Nullable Enumerable<V> parameter) throws SQLException {
    if (parameter == null) {
      delegate.setParameter(ps, parameterIndex, null);
    }
    else {
      delegate.setParameter(ps, parameterIndex, parameter.getValue());
    }
  }

  @Nullable
  @Override
  public Enumerable<V> getResult(ResultSet rs, String columnName) throws SQLException {
    V result = delegate.getResult(rs, columnName);
    return Enumerable.of(type, result);
  }

  @Nullable
  @Override
  public Enumerable<V> getResult(ResultSet rs, int columnIndex) throws SQLException {
    V result = delegate.getResult(rs, columnIndex);
    return Enumerable.of(type, result);
  }

  @Nullable
  @Override
  public Enumerable<V> getResult(CallableStatement cs, int columnIndex) throws SQLException {
    V result = delegate.getResult(cs, columnIndex);
    return Enumerable.of(type, result);
  }

}
