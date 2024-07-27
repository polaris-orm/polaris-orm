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

package cn.taketoday.polaris.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.ClassUtils;
import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.ObjectUtils;
import cn.taketoday.polaris.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/23 14:10
 */
public abstract class BeanUtils {

  /**
   * Path separator for nested properties.
   * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
   */
  static final char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

  /**
   * Marker that indicates the start of a property key for an
   * indexed or mapped property like "person.addresses[0]".
   */
  static final char PROPERTY_KEY_PREFIX_CHAR = '[';

  /**
   * Marker that indicates the end of a property key for an
   * indexed or mapped property like "person.addresses[0]".
   */
  static final char PROPERTY_KEY_SUFFIX_CHAR = ']';

  private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES = Map.of(
          boolean.class, false,
          byte.class, (byte) 0,
          short.class, (short) 0,
          int.class, 0,
          long.class, 0L,
          float.class, 0F,
          double.class, 0D,
          char.class, '\0'
  );

  /**
   * Convenience method to instantiate a class using the given constructor.
   * <p>Note that this method tries to set the constructor accessible if given a
   * non-accessible (that is, non-public) constructor,
   * with optional parameters and default values.
   *
   * @param constructor the constructor to instantiate
   * @param args the constructor arguments to apply (use {@code null} for an unspecified
   * parameter)
   * @return the new instance
   * @throws BeanInstantiationException if the bean cannot be instantiated
   * @see Constructor#newInstance
   */
  public static <T> T newInstance(Constructor<T> constructor, @Nullable Object... args) {
    if (ObjectUtils.isNotEmpty(args)) {
      if (args.length > constructor.getParameterCount()) {
        throw new BeanInstantiationException(
                constructor, "Illegal arguments for constructor, can't specify more arguments than constructor parameters", null);
      }
      int i = 0;
      Class<?>[] parameterTypes = null;
      for (Object arg : args) {
        if (arg == null) {
          if (parameterTypes == null) {
            parameterTypes = constructor.getParameterTypes();
          }
          Class<?> parameterType = parameterTypes[i];
          // argsWithDefaultValues
          args[i] = parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null;
        }
        i++;
      }
    }
    try {
      ReflectionUtils.makeAccessible(constructor);
      return constructor.newInstance(args);
    }
    catch (InstantiationException ex) {
      throw new BeanInstantiationException(constructor, "Is it an abstract class?", ex);
    }
    catch (IllegalAccessException ex) {
      throw new BeanInstantiationException(constructor, "Is the constructor accessible?", ex);
    }
    catch (IllegalArgumentException ex) {
      throw new BeanInstantiationException(constructor, "Illegal arguments for constructor", ex);
    }
    catch (InvocationTargetException ex) {
      throw new BeanInstantiationException(constructor, "Constructor threw exception", ex.getTargetException());
    }
  }

  /**
   * Obtain a suitable {@link Constructor}.
   * <p>
   * Look for the default constructor, if there is no default constructor, then
   * get all the constructors, if there is only one constructor then use this
   * constructor, if not more than one use the @Autowired constructor if there is
   * no suitable {@link Constructor} will throw an exception
   * <p>
   *
   * @param <T> Target type
   * @param beanClass target bean class
   * @return Suitable constructor
   */
  public static <T> Constructor<T> obtainConstructor(Class<T> beanClass) {
    final Constructor<T> ret = getConstructor(beanClass);
    if (ret == null) {
      throw new ReflectionException("no suitable constructor for class " + beanClass.getName());
    }
    return ret;
  }

  /**
   * Get a suitable {@link Constructor}.
   * <p>
   * Look for the default constructor, if there is no default constructor, then
   * get all the constructors, if there is only one constructor then use this
   * constructor, if not more than one use the @Autowired constructor if there is
   * no suitable {@link Constructor} will throw an exception
   * <p>
   *
   * @param <T> Target type
   * @param beanClass target bean class
   * @return Suitable constructor If there isn't a suitable {@link Constructor}
   * returns null
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> getConstructor(Class<T> beanClass) {
    Assert.notNull(beanClass, "bean-class is required");
    Constructor<?>[] ctors = beanClass.getConstructors();
    if (ctors.length == 1) {
      // A single public constructor
      return (Constructor<T>) ctors[0];
    }
    else if (ctors.length == 0) {
      ctors = beanClass.getDeclaredConstructors();
      if (ctors.length == 1) {
        // A single non-public constructor, e.g. from a non-public record type
        return (Constructor<T>) ctors[0];
      }
    }

    return selectConstructor(ctors);
  }

  /**
   * select a suitable {@link Constructor}.
   *
   * @param <T> Target type
   * @return Suitable constructor If there isn't a suitable {@link Constructor}
   * returns null
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> selectConstructor(Constructor<?>[] ctors) {
    if (ctors.length == 1) {
      // A single constructor
      return (Constructor<T>) ctors[0];
    }

    // iterate all constructors
    for (Constructor<?> constructor : ctors) {
      if (constructor.getParameterCount() == 0) {
        return (Constructor<T>) constructor;
      }
    }
    return null;
  }

  /**
   * Check if the given type represents a "simple" value type for
   * bean property and data binding purposes:
   * a primitive or primitive wrapper, an {@code Enum}, a {@code String}
   * or other {@code CharSequence}, a {@code Number}, a {@code Date},
   * a {@code Temporal}, a {@code UUID}, a {@code URI}, a {@code URL},
   * a {@code Locale}, or a {@code Class}.
   * <p>{@code Void} and {@code void} are not considered simple value types.
   * <p>this method delegates to {@link ClassUtils#isSimpleValueType}
   * as-is but could potentially add further rules for bean property purposes.
   *
   * @param type the type to check
   * @return whether the given type represents a "simple" value type
   * @see ClassUtils#isSimpleValueType(Class)
   */
  public static boolean isSimpleValueType(Class<?> type) {
    return ClassUtils.isSimpleValueType(type);
  }

  /**
   * Determine the first nested property separator in the
   * given property path, ignoring dots in keys (like "map[my.key]").
   *
   * @param propertyPath the property path to check
   * @return the index of the nested property separator, or -1 if none
   */
  public static int getFirstNestedPropertySeparatorIndex(String propertyPath) {
    return getNestedPropertySeparatorIndex(propertyPath, false);
  }

  /**
   * Determine the first (or last) nested property separator in the
   * given property path, ignoring dots in keys (like "map[my.key]").
   *
   * @param propertyPath the property path to check
   * @param last whether to return the last separator rather than the first
   * @return the index of the nested property separator, or -1 if none
   */
  private static int getNestedPropertySeparatorIndex(String propertyPath, boolean last) {
    boolean inKey = false;
    int length = propertyPath.length();
    int i = (last ? length - 1 : 0);
    while (last ? i >= 0 : i < length) {
      switch (propertyPath.charAt(i)) {
        case PROPERTY_KEY_PREFIX_CHAR:
        case PROPERTY_KEY_SUFFIX_CHAR:
          inKey = !inKey;
          break;
        case NESTED_PROPERTY_SEPARATOR_CHAR:
          if (!inKey) {
            return i;
          }
      }
      if (last) {
        i--;
      }
      else {
        i++;
      }
    }
    return -1;
  }

}
