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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * BeanProperty {@link TypeHandler} resolver
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 23:32
 */
public interface TypeHandlerResolver {

  /**
   * BeanProperty {@link TypeHandler} resolver
   *
   * @param beanProperty target property
   */
  @Nullable
  TypeHandler<?> resolve(BeanProperty beanProperty);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default TypeHandlerResolver and(TypeHandlerResolver next) {
    return beanProperty -> {
      TypeHandler<?> resolved = resolve(beanProperty);
      if (resolved == null) {
        resolved = next.resolve(beanProperty);
      }
      return resolved;
    };
  }

  static TypeHandlerResolver composite(TypeHandlerResolver... resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    return composite(List.of(resolvers));
  }

  static TypeHandlerResolver composite(List<TypeHandlerResolver> resolvers) {
    Assert.notNull(resolvers, "TypeHandlerResolver is required");
    return beanProperty -> {
      for (TypeHandlerResolver resolver : resolvers) {
        TypeHandler<?> resolved = resolver.resolve(beanProperty);
        if (resolved != null) {
          return resolved;
        }
      }

      return null;
    };
  }

  /**
   * Use {@link MappedTypeHandler} to resolve {@link TypeHandler}
   *
   * @return Annotation based {@link TypeHandlerResolver}
   * @see MergedAnnotation#getClass(String)
   */
  static TypeHandlerResolver forMappedTypeHandlerAnnotation() {
    return forAnnotation(MappedTypeHandler.class);
  }

  /**
   * Use input {@code annotationType} to resolve {@link TypeHandler}
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link TypeHandlerResolver}
   * @see MergedAnnotation#getClass(String)
   */
  static TypeHandlerResolver forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Use input {@code annotationType} and {@code attributeName} to resolve {@link TypeHandler}
   *
   * @param annotationType Annotation type
   * @param attributeName the attribute name
   * @return Annotation based {@link TypeHandlerResolver}
   * @see MergedAnnotation#getClass(String)
   */
  static TypeHandlerResolver forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    return (BeanProperty property) -> {
      var mappedTypeHandler = MergedAnnotations.from(
              property, property.getAnnotations()).get(annotationType);
      if (mappedTypeHandler.isPresent()) {
        // user defined TypeHandler
        Class<? extends TypeHandler<?>> typeHandlerClass = mappedTypeHandler.getClass(attributeName);
        Constructor<? extends TypeHandler<?>> constructor = BeanUtils.getConstructor(typeHandlerClass);
        if (constructor == null) {
          throw new IllegalStateException("No suitable constructor in " + typeHandlerClass);
        }

        if (constructor.getParameterCount() != 0) {
          Object[] args = new Object[constructor.getParameterCount()];
          Class<?>[] parameterTypes = constructor.getParameterTypes();
          int i = 0;
          for (Class<?> parameterType : parameterTypes) {
            args[i++] = resolveArg(property, parameterType);
          }
          return BeanUtils.newInstance(constructor, args);
        }
        else {
          return BeanUtils.newInstance(constructor);
        }
      }

      return null;
    };
  }

  private static Object resolveArg(BeanProperty beanProperty, Class<?> parameterType) {
    if (parameterType == Class.class) {
      return beanProperty.getType();
    }
    if (parameterType == BeanProperty.class) {
      return beanProperty;
    }
    throw new IllegalArgumentException(
            "TypeHandler Constructor parameterType '" + parameterType.getName() + "' currently not supported");
  }

}
