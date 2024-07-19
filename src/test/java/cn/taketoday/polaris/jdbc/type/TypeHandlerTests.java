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

package cn.taketoday.polaris.jdbc.type;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.jdbc.type.AnyTypeHandler;
import cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler;
import cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler;
import cn.taketoday.polaris.jdbc.type.BooleanTypeHandler;
import cn.taketoday.polaris.jdbc.type.ByteArrayTypeHandler;
import cn.taketoday.polaris.jdbc.type.ByteTypeHandler;
import cn.taketoday.polaris.jdbc.type.BytesInputStreamTypeHandler;
import cn.taketoday.polaris.jdbc.type.CharacterTypeHandler;
import cn.taketoday.polaris.jdbc.type.DateTypeHandler;
import cn.taketoday.polaris.jdbc.type.DoubleTypeHandler;
import cn.taketoday.polaris.jdbc.type.DurationTypeHandler;
import cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler;
import cn.taketoday.polaris.jdbc.type.EnumType;
import cn.taketoday.polaris.jdbc.type.FloatTypeHandler;
import cn.taketoday.polaris.jdbc.type.InstantTypeHandler;
import cn.taketoday.polaris.jdbc.type.IntegerTypeHandler;
import cn.taketoday.polaris.jdbc.type.LongTypeHandler;
import cn.taketoday.polaris.jdbc.type.MonthTypeHandler;
import cn.taketoday.polaris.jdbc.type.ObjectTypeHandler;
import cn.taketoday.polaris.jdbc.type.ShortTypeHandler;
import cn.taketoday.polaris.jdbc.type.SqlDateTypeHandler;
import cn.taketoday.polaris.jdbc.type.SqlTimeTypeHandler;
import cn.taketoday.polaris.jdbc.type.SqlTimestampTypeHandler;
import cn.taketoday.polaris.jdbc.type.StringTypeHandler;
import cn.taketoday.polaris.jdbc.type.TypeHandler;
import cn.taketoday.polaris.jdbc.type.UUIDTypeHandler;
import cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler;
import cn.taketoday.polaris.jdbc.type.YearTypeHandler;
import cn.taketoday.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/6/15 11:15
 */
class TypeHandlerTests {

  protected final ResultSet resultSet = mock(ResultSet.class);

  protected final CallableStatement callableStatement = mock(CallableStatement.class);

  protected final PreparedStatement preparedStatement = mock(PreparedStatement.class);

  interface PreparedStatementFunc<T> {

    void accept(PreparedStatement statement, int parameterIndex, T value) throws SQLException;

  }

  @ParameterizedTest
  @MethodSource("setParameterArgumentSource")
  <T> void setParameter(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler, PreparedStatementFunc<T> consumer, T value, T verifyVal) throws SQLException {
    typeHandler.setParameter(preparedStatement, 0, value);
    consumer.accept(verify(preparedStatement), 0, verifyVal);

    typeHandler.setParameter(preparedStatement, 0, null);
    verify(preparedStatement).setObject(0, null);
  }

  @ParameterizedTest
  @MethodSource("getResultColumnIndexArgumentSource")
  <T> void getResultColumnIndex(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler, BiFunction<ResultSet, Integer, T> consumer,
          T value, T verifyVal, boolean wasNull) throws SQLException {
    given(consumer.apply(resultSet, 1)).willReturn(value);
    given(resultSet.wasNull()).willReturn(wasNull);

    T result = typeHandler.getResult(resultSet, 1);
    assertThat(result).isEqualTo(verifyVal);
  }

  @ParameterizedTest
  @MethodSource("getResultColumnNameArgumentSource")
  <T> void getResultColumnName(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler, BiFunction<ResultSet, String, T> consumer,
          T value, T verifyVal, boolean wasNull) throws SQLException {
    given(consumer.apply(resultSet, "columnName")).willReturn(value);
    given(resultSet.wasNull()).willReturn(wasNull);

    T result = typeHandler.getResult(resultSet, "columnName");
    assertThat(result).isEqualTo(verifyVal);
  }

  @ParameterizedTest
  @MethodSource("getResultColumnIndexFromCallableStatementArgumentSource")
  <T> void getResultColumnIndexFromCallableStatement(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler,
          BiFunction<CallableStatement, Integer, T> consumer, T value, T verifyVal, boolean wasNull) throws SQLException {
    given(consumer.apply(callableStatement, 1)).willReturn(value);
    given(callableStatement.wasNull()).willReturn(wasNull);

    T result = typeHandler.getResult(callableStatement, 1);
    assertThat(result).isEqualTo(verifyVal);
  }

  public static Stream<Arguments> getResultColumnIndexFromCallableStatementArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());

    OffsetDateTime offsetDateTime = OffsetDateTime.now();
    return Stream.of(
            call(new cn.taketoday.polaris.jdbc.type.LongTypeHandler(), CallableStatement::getLong, 1L),
            call(new cn.taketoday.polaris.jdbc.type.IntegerTypeHandler(), CallableStatement::getInt, 1),
            call(new cn.taketoday.polaris.jdbc.type.DoubleTypeHandler(), CallableStatement::getDouble, 1D),
            call(new cn.taketoday.polaris.jdbc.type.FloatTypeHandler(), CallableStatement::getFloat, 1f),
            call(new cn.taketoday.polaris.jdbc.type.BooleanTypeHandler(), CallableStatement::getBoolean, true),
            call(new cn.taketoday.polaris.jdbc.type.ByteArrayTypeHandler(), CallableStatement::getBytes, new byte[] { 1 }),
            call(new cn.taketoday.polaris.jdbc.type.ByteTypeHandler(), CallableStatement::getByte, (byte) 1),
            call(new cn.taketoday.polaris.jdbc.type.ObjectTypeHandler(), CallableStatement::getObject, 1),
            call(new cn.taketoday.polaris.jdbc.type.ShortTypeHandler(), CallableStatement::getShort, (short) 1),
            call(new cn.taketoday.polaris.jdbc.type.StringTypeHandler(), CallableStatement::getString, "0001"),
            call(new cn.taketoday.polaris.jdbc.type.SqlTimeTypeHandler(), CallableStatement::getTime, Time.valueOf(LocalTime.now())),
            call(new cn.taketoday.polaris.jdbc.type.SqlDateTypeHandler(), CallableStatement::getDate, Date.valueOf(LocalDate.now())),
            call(new cn.taketoday.polaris.jdbc.type.SqlTimestampTypeHandler(), CallableStatement::getTimestamp, Timestamp.from(Instant.now())),

            call(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), CallableStatement::getLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            call(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), CallableStatement::getLong, 0L, Duration.ZERO),
            call(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), CallableStatement::getLong, 0L, null, true),

            call(new cn.taketoday.polaris.jdbc.type.InstantTypeHandler(), CallableStatement::getTimestamp, null, null),
            call(new cn.taketoday.polaris.jdbc.type.InstantTypeHandler(), CallableStatement::getTimestamp, Timestamp.from(Instant.EPOCH), Instant.EPOCH),

            call(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), CallableStatement::getTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            call(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), CallableStatement::getTimestamp, null),

            call(new cn.taketoday.polaris.jdbc.type.CharacterTypeHandler(), CallableStatement::getString, "1", '1'),
            call(new cn.taketoday.polaris.jdbc.type.CharacterTypeHandler(), CallableStatement::getString, null, null),

            call(new cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler(), CallableStatement::getBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            call(new cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler(), CallableStatement::getBigDecimal, null, null),

            call(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), CallableStatement::getString, uuid.toString(), uuid),
            call(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), CallableStatement::getString, null, null),
            call(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), CallableStatement::getString, "", null),

            call(new cn.taketoday.polaris.jdbc.type.YearTypeHandler(), CallableStatement::getInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            call(new cn.taketoday.polaris.jdbc.type.YearTypeHandler(), CallableStatement::getInt, 0, null, true),

            call(new cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler(), CallableStatement::getString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            call(new cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler(), CallableStatement::getString, null, null),

            call(new cn.taketoday.polaris.jdbc.type.MonthTypeHandler(), CallableStatement::getInt, Month.JANUARY.getValue(), Month.JANUARY),
            call(new cn.taketoday.polaris.jdbc.type.MonthTypeHandler(), CallableStatement::getInt, 0, null, true),

            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), OffsetTime.now(ZoneOffset.UTC)),
            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), null),

            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), offsetDateTime),
            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), null),

            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), null),
            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), LocalTime.now()),

            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), LocalDate.now()),
            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), null),

            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), LocalDateTime.now()),
            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), null),

            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), null),
            call(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), ZonedDateTime.now()),

            call(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), CallableStatement::getTimestamp, new Timestamp(date.getTime()), date),
            call(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), CallableStatement::getTimestamp, null, null),

            //call(new BytesInputStreamTypeHandler(), CallableStatement::getBytes, new byte[] { 1 }, new ByteArrayInputStream(new byte[] { 1 })),
            call(new cn.taketoday.polaris.jdbc.type.BytesInputStreamTypeHandler(), CallableStatement::getBytes, null, null),

            call(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), CallableStatement::getInt, 0, null, true),
            call(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), CallableStatement::getInt, 0, cn.taketoday.polaris.jdbc.type.EnumType.ORDINAL),
            call(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), CallableStatement::getInt, 1, cn.taketoday.polaris.jdbc.type.EnumType.NAME),

            call(new cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler(), CallableStatement::getBigDecimal, BigDecimal.valueOf(1)),
            call(new cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler(), CallableStatement::getBigDecimal, null, null)
    );
  }

  public static Stream<Arguments> getResultColumnNameArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());

    OffsetDateTime offsetDateTime = OffsetDateTime.now();
    return Stream.of(
            stringArgs(new cn.taketoday.polaris.jdbc.type.LongTypeHandler(), ResultSet::getLong, 1L),
            stringArgs(new cn.taketoday.polaris.jdbc.type.IntegerTypeHandler(), ResultSet::getInt, 1),
            stringArgs(new cn.taketoday.polaris.jdbc.type.DoubleTypeHandler(), ResultSet::getDouble, 1D),
            stringArgs(new cn.taketoday.polaris.jdbc.type.FloatTypeHandler(), ResultSet::getFloat, 1f),
            stringArgs(new cn.taketoday.polaris.jdbc.type.BooleanTypeHandler(), ResultSet::getBoolean, true),
            stringArgs(new cn.taketoday.polaris.jdbc.type.ByteArrayTypeHandler(), ResultSet::getBytes, new byte[] { 1 }),
            stringArgs(new cn.taketoday.polaris.jdbc.type.ByteTypeHandler(), ResultSet::getByte, (byte) 1),
            stringArgs(new cn.taketoday.polaris.jdbc.type.ObjectTypeHandler(), ResultSet::getObject, 1),
            stringArgs(new cn.taketoday.polaris.jdbc.type.ShortTypeHandler(), ResultSet::getShort, (short) 1),
            stringArgs(new cn.taketoday.polaris.jdbc.type.StringTypeHandler(), ResultSet::getString, "0001"),
            stringArgs(new cn.taketoday.polaris.jdbc.type.SqlTimeTypeHandler(), ResultSet::getTime, Time.valueOf(LocalTime.now())),
            stringArgs(new cn.taketoday.polaris.jdbc.type.SqlDateTypeHandler(), ResultSet::getDate, Date.valueOf(LocalDate.now())),
            stringArgs(new cn.taketoday.polaris.jdbc.type.SqlTimestampTypeHandler(), ResultSet::getTimestamp, Timestamp.from(Instant.now())),

            stringArgs(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), ResultSet::getLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), ResultSet::getLong, 0L, Duration.ZERO),
            stringArgs(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), ResultSet::getLong, 0L, null, true),

            stringArgs(new cn.taketoday.polaris.jdbc.type.InstantTypeHandler(), ResultSet::getTimestamp, null, null),
            stringArgs(new cn.taketoday.polaris.jdbc.type.InstantTypeHandler(), ResultSet::getTimestamp, Timestamp.from(Instant.EPOCH), Instant.EPOCH),

            stringArgs(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), ResultSet::getTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), ResultSet::getTimestamp, null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.CharacterTypeHandler(), ResultSet::getString, "1", '1'),
            stringArgs(new cn.taketoday.polaris.jdbc.type.CharacterTypeHandler(), ResultSet::getString, null, null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler(), ResultSet::getBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler(), ResultSet::getBigDecimal, null, null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), ResultSet::getString, uuid.toString(), uuid),
            stringArgs(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), ResultSet::getString, null, null),
            stringArgs(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), ResultSet::getString, "", null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.YearTypeHandler(), ResultSet::getInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.YearTypeHandler(), ResultSet::getInt, 0, null, true),

            stringArgs(new cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler(), ResultSet::getString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler(), ResultSet::getString, null, null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.MonthTypeHandler(), ResultSet::getInt, Month.JANUARY.getValue(), Month.JANUARY),
            stringArgs(new cn.taketoday.polaris.jdbc.type.MonthTypeHandler(), ResultSet::getInt, 0, null, true),

            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), OffsetTime.now(ZoneOffset.UTC)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), offsetDateTime),
            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), null),
            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), LocalTime.now()),

            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), LocalDate.now()),
            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), LocalDateTime.now()),
            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), null),
            stringArgs(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), ZonedDateTime.now()),

            stringArgs(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), ResultSet::getTimestamp, new Timestamp(date.getTime()), date),
            stringArgs(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), ResultSet::getTimestamp, null, null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.BytesInputStreamTypeHandler(), ResultSet::getBytes, null, null),

            stringArgs(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), ResultSet::getInt, 0, null, true),
            stringArgs(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), ResultSet::getInt, 0, cn.taketoday.polaris.jdbc.type.EnumType.ORDINAL),
            stringArgs(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), ResultSet::getInt, 1, cn.taketoday.polaris.jdbc.type.EnumType.NAME),

            stringArgs(new cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler(), ResultSet::getBigDecimal, BigDecimal.valueOf(1)),
            stringArgs(new cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler(), ResultSet::getBigDecimal, null, null)
    );
  }

  public static Stream<Arguments> getResultColumnIndexArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());

    OffsetDateTime offsetDateTime = OffsetDateTime.now();
    return Stream.of(
            args(new cn.taketoday.polaris.jdbc.type.LongTypeHandler(), ResultSet::getLong, 1L),
            args(new cn.taketoday.polaris.jdbc.type.IntegerTypeHandler(), ResultSet::getInt, 1),
            args(new cn.taketoday.polaris.jdbc.type.DoubleTypeHandler(), ResultSet::getDouble, 1D),
            args(new cn.taketoday.polaris.jdbc.type.FloatTypeHandler(), ResultSet::getFloat, 1f),
            args(new cn.taketoday.polaris.jdbc.type.BooleanTypeHandler(), ResultSet::getBoolean, true),
            args(new cn.taketoday.polaris.jdbc.type.ByteArrayTypeHandler(), ResultSet::getBytes, new byte[] { 1 }),
            args(new cn.taketoday.polaris.jdbc.type.ByteTypeHandler(), ResultSet::getByte, (byte) 1),
            args(new cn.taketoday.polaris.jdbc.type.ObjectTypeHandler(), (resultSet1, integer) -> resultSet1.getObject(integer), 1),
            args(new cn.taketoday.polaris.jdbc.type.ShortTypeHandler(), ResultSet::getShort, (short) 1),
            args(new cn.taketoday.polaris.jdbc.type.StringTypeHandler(), ResultSet::getString, "0001"),
            args(new cn.taketoday.polaris.jdbc.type.SqlTimeTypeHandler(), (rs, idx) -> rs.getTime(idx), Time.valueOf(LocalTime.now())),
            args(new cn.taketoday.polaris.jdbc.type.SqlDateTypeHandler(), (rs, idx) -> rs.getDate(idx), Date.valueOf(LocalDate.now())),
            args(new cn.taketoday.polaris.jdbc.type.SqlTimestampTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), Timestamp.from(Instant.now())),

            args(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), ResultSet::getLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            args(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), ResultSet::getLong, 0L, Duration.ZERO),
            args(new cn.taketoday.polaris.jdbc.type.DurationTypeHandler(), ResultSet::getLong, 0L, null, true),

            args(new cn.taketoday.polaris.jdbc.type.InstantTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), null, null),
            args(new cn.taketoday.polaris.jdbc.type.InstantTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), Timestamp.from(Instant.EPOCH), Instant.EPOCH),

            args(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), new Timestamp(new Date(1).getTime()), new Date(1)),
            args(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), null),

            args(new cn.taketoday.polaris.jdbc.type.CharacterTypeHandler(), ResultSet::getString, "1", '1'),
            args(new cn.taketoday.polaris.jdbc.type.CharacterTypeHandler(), ResultSet::getString, null, null),

            args(new cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            args(new cn.taketoday.polaris.jdbc.type.BigIntegerTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), null, null),

            args(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), ResultSet::getString, uuid.toString(), uuid),
            args(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), ResultSet::getString, null, null),
            args(new cn.taketoday.polaris.jdbc.type.UUIDTypeHandler(), ResultSet::getString, "", null),

            args(new cn.taketoday.polaris.jdbc.type.YearTypeHandler(), ResultSet::getInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            args(new cn.taketoday.polaris.jdbc.type.YearTypeHandler(), ResultSet::getInt, 0, null, true),

            args(new cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler(), ResultSet::getString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),
            args(new cn.taketoday.polaris.jdbc.type.YearMonthTypeHandler(), ResultSet::getString, null, null),

            args(new cn.taketoday.polaris.jdbc.type.MonthTypeHandler(), ResultSet::getInt, Month.JANUARY.getValue(), Month.JANUARY),
            args(new cn.taketoday.polaris.jdbc.type.MonthTypeHandler(), ResultSet::getInt, 0, null, true),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), OffsetTime.now(ZoneOffset.UTC)),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), (rs, idx) -> rs.getObject(idx, OffsetTime.class), null),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), offsetDateTime),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetDateTime.class), (rs, idx) -> rs.getObject(idx, OffsetDateTime.class), null),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), null),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), (rs, idx) -> rs.getObject(idx, LocalTime.class), LocalTime.now()),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), LocalDate.now()),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), (rs, idx) -> rs.getObject(idx, LocalDate.class), null),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), LocalDateTime.now()),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), (rs, idx) -> rs.getObject(idx, LocalDateTime.class), null),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), null),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), (rs, idx) -> rs.getObject(idx, ZonedDateTime.class), ZonedDateTime.now()),

            args(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), new Timestamp(date.getTime()), date),
            args(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), (rs, idx) -> rs.getTimestamp(idx), null, null),

            args(new cn.taketoday.polaris.jdbc.type.BytesInputStreamTypeHandler(), ResultSet::getBytes, null, null),

            args(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), ResultSet::getInt, 0, null, true),
            args(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), ResultSet::getInt, 0, cn.taketoday.polaris.jdbc.type.EnumType.ORDINAL),
            args(new cn.taketoday.polaris.jdbc.type.EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), ResultSet::getInt, 1, cn.taketoday.polaris.jdbc.type.EnumType.NAME),

            args(new cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), BigDecimal.valueOf(1)),
            args(new cn.taketoday.polaris.jdbc.type.BigDecimalTypeHandler(), (rs, idx) -> rs.getBigDecimal(idx), null, null)
    );
  }

  public static Stream<Arguments> setParameterArgumentSource() {
    UUID uuid = UUID.randomUUID();
    java.util.Date date = java.util.Date.from(Instant.now());
    return Stream.of(
            args(new LongTypeHandler(), PreparedStatement::setLong, 1L),
            args(new IntegerTypeHandler(), PreparedStatement::setInt, 1),
            args(new DoubleTypeHandler(), PreparedStatement::setDouble, 1D),
            args(new FloatTypeHandler(), PreparedStatement::setFloat, 1f),
            args(new BooleanTypeHandler(), PreparedStatement::setBoolean, true),
            args(new ByteArrayTypeHandler(), PreparedStatement::setBytes, new byte[] { 1 }),
            args(new ByteTypeHandler(), PreparedStatement::setByte, (byte) 1),
            args(new ObjectTypeHandler(), PreparedStatement::setObject, 1),
            args(new ShortTypeHandler(), PreparedStatement::setShort, (short) 1),
            args(new StringTypeHandler(), PreparedStatement::setString, "0001"),
            args(new SqlTimeTypeHandler(), PreparedStatement::setTime, Time.valueOf(LocalTime.now())),
            args(new SqlDateTypeHandler(), PreparedStatement::setDate, Date.valueOf(LocalDate.now())),
            args(new SqlTimestampTypeHandler(), PreparedStatement::setTimestamp, Timestamp.from(Instant.now())),

            args(new DurationTypeHandler(), PreparedStatement::setLong, Duration.ofDays(1).toNanos(), Duration.ofDays(1)),
            args(new InstantTypeHandler(), PreparedStatement::setTimestamp, Timestamp.from(Instant.MIN), Instant.MIN),
            args(new cn.taketoday.polaris.jdbc.type.DateTypeHandler(), PreparedStatement::setTimestamp, new Timestamp(new Date(1).getTime()), new Date(1)),
            args(new CharacterTypeHandler(), PreparedStatement::setString, "1", '1'),

            args(new BigIntegerTypeHandler(), PreparedStatement::setBigDecimal, new BigDecimal(BigInteger.valueOf(1)), BigInteger.valueOf(1)),
            args(new UUIDTypeHandler(), PreparedStatement::setString, uuid.toString(), uuid),
            args(new YearTypeHandler(), PreparedStatement::setInt, Year.MIN_VALUE, Year.of(Year.MIN_VALUE)),
            args(new YearMonthTypeHandler(), PreparedStatement::setString, YearMonth.of(2000, Month.JANUARY).toString(),
                    YearMonth.of(2000, Month.JANUARY)),

            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDate.class), PreparedStatement::setObject, LocalDate.now()),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalTime.class), PreparedStatement::setObject, LocalTime.now()),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(LocalDateTime.class), PreparedStatement::setObject, LocalDateTime.now()),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(OffsetTime.class), PreparedStatement::setObject, OffsetTime.now()),
            args(new cn.taketoday.polaris.jdbc.type.AnyTypeHandler<>(ZonedDateTime.class), PreparedStatement::setObject, ZonedDateTime.now()),
            args(new AnyTypeHandler<>(OffsetDateTime.class), PreparedStatement::setObject, OffsetDateTime.now()),

            args(new MonthTypeHandler(), PreparedStatement::setInt, Month.JANUARY.getValue(), Month.JANUARY),
            args(new DateTypeHandler(), PreparedStatement::setTimestamp, new Timestamp(date.getTime()), date),
            args(new EnumOrdinalTypeHandler<>(cn.taketoday.polaris.jdbc.type.EnumType.class), PreparedStatement::setInt, cn.taketoday.polaris.jdbc.type.EnumType.ORDINAL.ordinal(), EnumType.ORDINAL),

            args(new BytesInputStreamTypeHandler(), PreparedStatement::setBinaryStream, new ByteArrayInputStream(new byte[] { 1 })),

            args(new BigDecimalTypeHandler(), PreparedStatement::setBigDecimal, BigDecimal.valueOf(1))
    );
  }

  static <T> Arguments call(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler, ThrowingBiFunction<CallableStatement, Integer, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments call(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler, ThrowingBiFunction<CallableStatement, Integer, T> consumer,
          @Nullable T value, @Nullable E verifyVal) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, false);
  }

  static <T, E> Arguments call(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler,
          ThrowingBiFunction<CallableStatement, Integer, T> consumer, T value, @Nullable E verifyVal, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, wasNull);
  }

  static <T> Arguments stringArgs(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler, ThrowingBiFunction<ResultSet, String, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments stringArgs(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler, ThrowingBiFunction<ResultSet, String, T> consumer,
          @Nullable T value, @Nullable E verifyVal) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, false);
  }

  static <T, E> Arguments stringArgs(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler,
          ThrowingBiFunction<ResultSet, String, T> consumer, T value, @Nullable E verifyVal, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, wasNull);
  }

  static <T> Arguments args(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler, ThrowingBiFunction<ResultSet, Integer, T> consumer, @Nullable T value) {
    return Arguments.arguments(typeHandler, consumer, value, value, false);
  }

  static <T, E> Arguments args(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler, ThrowingBiFunction<ResultSet, Integer, T> consumer,
          @Nullable T value, @Nullable E verifyVal) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, false);
  }

  static <T, E> Arguments args(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler,
          ThrowingBiFunction<ResultSet, Integer, T> consumer, T value, @Nullable E verifyVal, boolean wasNull) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal, wasNull);
  }

  static <T, E> Arguments args(cn.taketoday.polaris.jdbc.type.TypeHandler<E> typeHandler, PreparedStatementFunc<T> consumer, T value) {
    return Arguments.arguments(typeHandler, consumer, value, value);
  }

  static <T, E> Arguments args(TypeHandler<T> typeHandler, PreparedStatementFunc<E> consumer, E verifyVal, T value) {
    return Arguments.arguments(typeHandler, consumer, value, verifyVal);
  }

}