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

import java.io.InputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import cn.taketoday.polaris.jdbc.type.TypeHandler;

/**
 * Parameter Setter
 *
 * @author TODAY 2021/1/7 15:09
 */
public abstract class ParameterBinder {

  /**
   * Bind a value to statement
   *
   * @param statement statement
   * @param paramIdx parameter index
   * @throws SQLException parameter set error
   */
  public abstract void bind(PreparedStatement statement, int paramIdx)
          throws SQLException;

  /**
   * null setter
   */
  public static final ParameterBinder null_binder = new ParameterBinder() {
    @Override
    public void bind(final PreparedStatement statement, final int paramIdx) throws SQLException {
      statement.setObject(paramIdx, null);
    }
  };

  /**
   * Bind int to {@link PreparedStatement}
   *
   * @param value int value
   * @return Int ParameterBinder
   * @see PreparedStatement#setInt(int, int)
   */
  public static ParameterBinder forInt(int value) {
    final class IntegerParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setInt(paramIdx, value);
      }

    }
    return new IntegerParameterBinder();
  }

  /**
   * Bind long to {@link PreparedStatement}
   *
   * @param value long value
   * @return Long ParameterBinder
   * @see PreparedStatement#setLong(int, long)
   */
  public static ParameterBinder forLong(long value) {
    final class LongParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setLong(paramIdx, value);
      }

    }
    return new LongParameterBinder();
  }

  /**
   * Bind String to {@link PreparedStatement}
   *
   * @param value String value
   * @return String ParameterBinder
   * @see PreparedStatement#setString(int, String)
   */
  public static ParameterBinder forString(String value) {
    final class StringParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setString(paramIdx, value);
      }

    }
    return new StringParameterBinder();
  }

  /**
   * Bind Timestamp to {@link PreparedStatement}
   *
   * @param value Timestamp value
   * @return Timestamp ParameterBinder
   * @see PreparedStatement#setTimestamp(int, Timestamp)
   */
  public static ParameterBinder forTimestamp(Timestamp value) {
    final class TimestampParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTimestamp(paramIdx, value);
      }

    }
    return new TimestampParameterBinder();
  }

  /**
   * Bind Time to {@link PreparedStatement}
   *
   * @param value Time value
   * @return Time ParameterBinder
   * @see PreparedStatement#setTime(int, Time)
   */
  public static ParameterBinder forTime(Time value) {
    final class TimeParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTime(paramIdx, value);
      }

    }
    return new TimeParameterBinder();
  }

  public static ParameterBinder forDate(Date value) {
    final class DateParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setDate(paramIdx, value);
      }
    }
    return new DateParameterBinder();
  }

  /**
   * Bind Boolean to {@link PreparedStatement}
   *
   * @param value Boolean value
   * @return Boolean ParameterBinder
   * @see PreparedStatement#setBoolean(int, boolean)
   */
  public static ParameterBinder forBoolean(boolean value) {
    final class BooleanParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBoolean(paramIdx, value);
      }

    }
    return new BooleanParameterBinder();
  }

  /**
   * Bind InputStream to {@link PreparedStatement}
   *
   * @param value InputStream value
   * @return InputStream ParameterBinder
   * @see PreparedStatement#setBinaryStream(int, InputStream)
   */
  public static ParameterBinder forBinaryStream(InputStream value) {
    final class BinaryStreamParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBinaryStream(paramIdx, value);
      }
    }
    return new BinaryStreamParameterBinder();
  }

  /**
   * Bind Object to {@link PreparedStatement} using TypeHandler
   *
   * @param value Object value
   * @return InputStream ParameterBinder
   * @see PreparedStatement#setBinaryStream(int, InputStream)
   */
  public static <T> ParameterBinder forTypeHandler(TypeHandler<T> typeHandler, T value) {
    final class TypeHandlerParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        typeHandler.setParameter(statement, paramIdx, value);
      }
    }
    return new TypeHandlerParameterBinder();
  }
}
