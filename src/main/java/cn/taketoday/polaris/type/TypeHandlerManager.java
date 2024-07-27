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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import cn.taketoday.polaris.beans.BeanProperty;
import cn.taketoday.polaris.beans.BeanUtils;
import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.Nullable;

/**
 * {@link TypeHandler} Manager
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class TypeHandlerManager implements TypeHandlerResolver {

  public static final TypeHandlerManager sharedInstance = new TypeHandlerManager();

  private final TypeHandler<Object> unknownTypeHandler;

  private final HashMap<Class<?>, TypeHandler<?>> typeHandlers = new HashMap<>();

  private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumerationValueTypeHandler.class;

  private TypeHandlerResolver typeHandlerResolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();

  public TypeHandlerManager() {
    this.unknownTypeHandler = new UnknownTypeHandler(this);
    registerDefaults(this);
  }

  /**
   * Set a default {@link TypeHandler} class for {@link Enum}.
   * A default {@link TypeHandler} is {@link EnumTypeHandler}.
   *
   * @param typeHandler a type handler class for {@link Enum}
   */
  public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
    this.defaultEnumTypeHandler = typeHandler;
  }

  public void addHandlerResolver(TypeHandlerResolver resolver) {
    Assert.notNull(resolver, "TypeHandlerResolver is required");
    this.typeHandlerResolver = typeHandlerResolver.and(resolver);
  }

  public void setHandlerResolver(@Nullable TypeHandlerResolver resolver) {
    this.typeHandlerResolver = resolver == null ? TypeHandlerResolver.forMappedTypeHandlerAnnotation() : resolver;
  }

  //

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    TypeHandler<?> typeHandler = typeHandlers.get(type);
    if (typeHandler == null) {
      if (Enumerable.class.isAssignableFrom(type)) {
        typeHandler = new EnumerableEnumTypeHandler(type, this);
        register(type, typeHandler);
      }
      else if (Enum.class.isAssignableFrom(type)) {
        typeHandler = getInstance(type, defaultEnumTypeHandler);
        register(type, typeHandler);
      }
      else {
        typeHandler = typeHandlerNotFound(type);
      }
    }
    return (TypeHandler<T>) typeHandler;
  }

  @Nullable
  @Override
  public TypeHandler<?> resolve(BeanProperty beanProperty) {
    return getTypeHandler(beanProperty);
  }

  /**
   * @since 1.0
   */
  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getTypeHandler(BeanProperty property) {
    TypeHandler<?> typeHandler = typeHandlerResolver.resolve(property);
    if (typeHandler == null) {
      // fallback to default
      Class<?> type = property.getType();
      typeHandler = typeHandlers.get(type);
      if (typeHandler == null) {
        if (Enumerable.class.isAssignableFrom(type)) {
          // for Enumerable type
          typeHandler = new EnumerableEnumTypeHandler(type, this);
          register(type, typeHandler);
        }
        else if (Enum.class.isAssignableFrom(type)) {
          // BeanProperty based
          Enumerated enumerated = property.getAnnotation(Enumerated.class);
          if (enumerated == null) {
            enumerated = type.getAnnotation(Enumerated.class);
          }

          if (enumerated != null) {
            EnumType enumType = enumerated.value();
            if (enumType == EnumType.ORDINAL) {
              typeHandler = new EnumOrdinalTypeHandler(type);
            }
            else {
              typeHandler = new EnumTypeHandler(type);
            }
          }
          else {
            typeHandler = getInstance(type, defaultEnumTypeHandler);
          }
        }
        else {
          typeHandler = typeHandlerNotFound(type);
        }
      }
    }

    return (TypeHandler<T>) typeHandler;
  }

  protected TypeHandler<?> typeHandlerNotFound(Type type) {
    return unknownTypeHandler;
  }

  public TypeHandler<Object> getUnknownTypeHandler() {
    return unknownTypeHandler;
  }

  @SuppressWarnings("unchecked")
  public <T> void register(TypeHandler<T> typeHandler) {
    boolean mappedTypeFound = false;
    MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
    if (mappedTypes != null) {
      for (Class<?> handledType : mappedTypes.value()) {
        register((Class<T>) handledType, typeHandler);
        mappedTypeFound = true;
      }
    }
    // try to auto-discover the mapped type
    if (!mappedTypeFound) {
      register((Class<T>) null, typeHandler);
    }
  }

  public <T> void register(Class<T> javaType, TypeHandler<?> typeHandler) {
    typeHandlers.put(javaType, typeHandler);
  }

  //
  // REGISTER CLASS
  //

  // Only handler type

  public void register(Class<?> typeHandlerClass) {
    boolean mappedTypeFound = false;
    MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
    if (mappedTypes != null) {
      for (Class<?> javaTypeClass : mappedTypes.value()) {
        register(javaTypeClass, typeHandlerClass);
        mappedTypeFound = true;
      }
    }
    if (!mappedTypeFound) {
      register(getInstance(null, typeHandlerClass));
    }
  }

  public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
  }

  // Construct a handler (used also from Builders)

  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getInstance(@Nullable Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    if (javaTypeClass != null) {
      Constructor<?> constructor = BeanUtils.getConstructor(typeHandlerClass);
      if (constructor == null) {
        throw new IllegalStateException("No suitable constructor in " + typeHandlerClass);
      }

      try {
        if (constructor.getParameterCount() != 0) {
          Object[] args = new Object[constructor.getParameterCount()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          int i = 0;
          for (Class<?> parameterType : parameterTypes) {
            args[i++] = resolveArg(javaTypeClass, parameterType);
          }
          return (TypeHandler<T>) BeanUtils.newInstance(constructor, args);
        }
        else {
          return (TypeHandler<T>) BeanUtils.newInstance(constructor);
        }
      }
      catch (Exception e) {
        throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
      }
    }

    try {
      Constructor<?> c = typeHandlerClass.getConstructor();
      return (TypeHandler<T>) c.newInstance();
    }
    catch (Exception e) {
      throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
    }
  }

  private Object resolveArg(Class<?> propertyType, Class<?> parameterType) {
    if (parameterType == Class.class) {
      return propertyType;
    }
    if (parameterType == TypeHandlerManager.class) {
      return this;
    }
    throw new IllegalArgumentException(
            "TypeHandler Constructor parameterType '" + parameterType.getName() + "' currently not supported");
  }

  public void clear() {
    typeHandlers.clear();
  }

  // static

  public static void registerDefaults(TypeHandlerManager registry) {
    registry.register(Boolean.class, new BooleanTypeHandler());
    registry.register(boolean.class, new BooleanTypeHandler());

    registry.register(Byte.class, new ByteTypeHandler());
    registry.register(byte.class, new ByteTypeHandler());

    registry.register(Short.class, new ShortTypeHandler());
    registry.register(short.class, new ShortTypeHandler());

    registry.register(int.class, new IntegerTypeHandler());
    registry.register(Integer.class, new IntegerTypeHandler());

    registry.register(Long.class, new LongTypeHandler());
    registry.register(long.class, new LongTypeHandler());

    registry.register(Float.class, new FloatTypeHandler());
    registry.register(float.class, new FloatTypeHandler());

    registry.register(Double.class, new DoubleTypeHandler());
    registry.register(double.class, new DoubleTypeHandler());

    registry.register(String.class, new StringTypeHandler());

    registry.register(BigInteger.class, new BigIntegerTypeHandler());
    registry.register(BigDecimal.class, new BigDecimalTypeHandler());

    registry.register(byte[].class, new ByteArrayTypeHandler());

    registry.register(Object.class, registry.getUnknownTypeHandler());

    registry.register(Date.class, new DateTypeHandler());

    registry.register(java.sql.Date.class, new SqlDateTypeHandler());
    registry.register(Time.class, new SqlTimeTypeHandler());
    registry.register(Timestamp.class, new SqlTimestampTypeHandler());

    registry.register(Instant.class, new InstantTypeHandler());
    registry.register(Year.class, new YearTypeHandler());
    registry.register(Month.class, new MonthTypeHandler());
    registry.register(YearMonth.class, new YearMonthTypeHandler());
    registry.register(char.class, new CharacterTypeHandler());
    registry.register(Character.class, new CharacterTypeHandler());

    registry.register(UUID.class, new UUIDTypeHandler());
    registry.register(Duration.class, new DurationTypeHandler());

    registry.register(LocalDate.class, new AnyTypeHandler<>(LocalDate.class));
    registry.register(LocalTime.class, new AnyTypeHandler<>(LocalTime.class));
    registry.register(LocalDateTime.class, new AnyTypeHandler<>(LocalDateTime.class));
    registry.register(OffsetTime.class, new AnyTypeHandler<>(OffsetTime.class));
    registry.register(ZonedDateTime.class, new AnyTypeHandler<>(ZonedDateTime.class));
    registry.register(OffsetDateTime.class, new AnyTypeHandler<>(OffsetDateTime.class));
  }

}
