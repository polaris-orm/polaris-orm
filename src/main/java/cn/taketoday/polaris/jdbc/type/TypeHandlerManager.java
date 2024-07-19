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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
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

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Enumerable;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.polaris.jdbc.type.TypeHandler} Manager
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class TypeHandlerManager implements TypeHandlerResolver {

  public static final TypeHandlerManager sharedInstance = new TypeHandlerManager();

  private final cn.taketoday.polaris.jdbc.type.TypeHandler<Object> unknownTypeHandler;

  private final HashMap<Class<?>, cn.taketoday.polaris.jdbc.type.TypeHandler<?>> typeHandlers = new HashMap<>();

  private Class<? extends cn.taketoday.polaris.jdbc.type.TypeHandler> defaultEnumTypeHandler = EnumerationValueTypeHandler.class;

  private TypeHandlerResolver typeHandlerResolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();

  public TypeHandlerManager() {
    this.unknownTypeHandler = new UnknownTypeHandler(this);
    registerDefaults(this);
  }

  /**
   * Set a default {@link cn.taketoday.polaris.jdbc.type.TypeHandler} class for {@link Enum}.
   * A default {@link cn.taketoday.polaris.jdbc.type.TypeHandler} is {@link EnumTypeHandler}.
   *
   * @param typeHandler a type handler class for {@link Enum}
   */
  public void setDefaultEnumTypeHandler(Class<? extends cn.taketoday.polaris.jdbc.type.TypeHandler> typeHandler) {
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
  public <T> cn.taketoday.polaris.jdbc.type.TypeHandler<T> getTypeHandler(Class<T> type) {
    cn.taketoday.polaris.jdbc.type.TypeHandler<?> typeHandler = typeHandlers.get(type);
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
    return (cn.taketoday.polaris.jdbc.type.TypeHandler<T>) typeHandler;
  }

  @Nullable
  @Override
  public cn.taketoday.polaris.jdbc.type.TypeHandler<?> resolve(BeanProperty beanProperty) {
    return getTypeHandler(beanProperty);
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> cn.taketoday.polaris.jdbc.type.TypeHandler<T> getTypeHandler(BeanProperty property) {
    cn.taketoday.polaris.jdbc.type.TypeHandler<?> typeHandler = typeHandlerResolver.resolve(property);
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
          var enumerated = MergedAnnotations.from(property, property.getAnnotations()).get(Enumerated.class);
          if (!enumerated.isPresent()) {
            enumerated = MergedAnnotations.from(type).get(Enumerated.class);
          }

          if (enumerated.isPresent()) {
            EnumType enumType = enumerated.getEnum(MergedAnnotation.VALUE, EnumType.class);
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

    return (cn.taketoday.polaris.jdbc.type.TypeHandler<T>) typeHandler;
  }

  protected cn.taketoday.polaris.jdbc.type.TypeHandler<?> typeHandlerNotFound(Type type) {
    return unknownTypeHandler;
  }

  public cn.taketoday.polaris.jdbc.type.TypeHandler<Object> getUnknownTypeHandler() {
    return unknownTypeHandler;
  }

  @SuppressWarnings("unchecked")
  public <T> void register(cn.taketoday.polaris.jdbc.type.TypeHandler<T> typeHandler) {
    boolean mappedTypeFound = false;
    var mappedTypes = MergedAnnotations.from(typeHandler.getClass()).get(MappedTypes.class);
    if (mappedTypes.isPresent()) {
      for (Class<?> handledType : mappedTypes.getClassValueArray()) {
        register((Class<T>) handledType, typeHandler);
        mappedTypeFound = true;
      }
    }
    // try to auto-discover the mapped type
    if (!mappedTypeFound && typeHandler instanceof ParameterizedTypeReference typeReference) {
      try {
        register(typeReference, typeHandler);
        mappedTypeFound = true;
      }
      catch (Throwable t) {
        // maybe users define the TypeReference with a different type and are not assignable, so just ignore it
      }
    }
    if (!mappedTypeFound) {
      register((Class<T>) null, typeHandler);
    }
  }

  public <T> void register(Class<T> javaType, cn.taketoday.polaris.jdbc.type.TypeHandler<?> typeHandler) {
    typeHandlers.put(javaType, typeHandler);
  }

  public <T> void register(ParameterizedTypeReference<T> reference, cn.taketoday.polaris.jdbc.type.TypeHandler<T> handler) {
    ResolvableType resolvableType = reference.getResolvableType();
    Class<?> aClass = resolvableType.toClass();
    register(aClass, handler);
  }

  //
  // REGISTER CLASS
  //

  // Only handler type

  public void register(Class<?> typeHandlerClass) {
    boolean mappedTypeFound = false;
    var mappedTypes = MergedAnnotations.from(typeHandlerClass).get(MappedTypes.class);
    if (mappedTypes.isPresent()) {
      for (Class<?> javaTypeClass : mappedTypes.getClassValueArray()) {
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
  public <T> cn.taketoday.polaris.jdbc.type.TypeHandler<T> getInstance(@Nullable Class<?> javaTypeClass, Class<?> typeHandlerClass) {
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
          return (cn.taketoday.polaris.jdbc.type.TypeHandler<T>) BeanUtils.newInstance(constructor, args);
        }
        else {
          return (cn.taketoday.polaris.jdbc.type.TypeHandler<T>) BeanUtils.newInstance(constructor);
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

    registry.register(Short.class, new cn.taketoday.polaris.jdbc.type.ShortTypeHandler());
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
    registry.register(java.sql.Time.class, new SqlTimeTypeHandler());
    registry.register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

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
