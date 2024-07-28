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

package cn.taketoday.polaris.util;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Currency;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import cn.taketoday.polaris.Constant;

/**
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ReflectionUtils
 * @since 1.0
 */
public abstract class ClassUtils {

  /** The CGLIB class separator: {@code "$$"}. */
  public static final String CGLIB_CLASS_SEPARATOR = "$$";

  public static final char INNER_CLASS_SEPARATOR = '$';

  /**
   * Map with primitive wrapper type as key and corresponding primitive
   * type as value, for example: Integer.class -> int.class.
   */
  private static final IdentityHashMap<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

  /**
   * Map with primitive type as key and corresponding wrapper
   * type as value, for example: int.class -> Integer.class.
   */
  @Deprecated
  private static final IdentityHashMap<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);

  static {
    primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
    primitiveWrapperTypeMap.put(Byte.class, byte.class);
    primitiveWrapperTypeMap.put(Character.class, char.class);
    primitiveWrapperTypeMap.put(Double.class, double.class);
    primitiveWrapperTypeMap.put(Float.class, float.class);
    primitiveWrapperTypeMap.put(Integer.class, int.class);
    primitiveWrapperTypeMap.put(Long.class, long.class);
    primitiveWrapperTypeMap.put(Short.class, short.class);
    primitiveWrapperTypeMap.put(Void.class, void.class);
  }

  /**
   * Return the default ClassLoader to use: typically the thread context
   * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
   * class will be used as fallback.
   * <p>Call this method if you intend to use the thread context ClassLoader
   * in a scenario where you clearly prefer a non-null ClassLoader reference:
   * for example, for class path resource loading (but not necessarily for
   * {@code Class.forName}, which accepts a {@code null} ClassLoader
   * reference as well).
   *
   * @return the default ClassLoader (only {@code null} if even the system
   * ClassLoader isn't accessible)
   * @see Thread#getContextClassLoader()
   * @see ClassLoader#getSystemClassLoader()
   */
  @Nullable
  public static ClassLoader getDefaultClassLoader() {
    ClassLoader cl = null;
    try {
      cl = Thread.currentThread().getContextClassLoader();
    }
    catch (Throwable ex) {
      // Cannot access thread context ClassLoader - falling back...
    }
    if (cl == null) {
      // No thread context class loader -> use class loader of this class.
      cl = ClassUtils.class.getClassLoader();
      if (cl == null) {
        // getClassLoader() returning null indicates the bootstrap ClassLoader
        try {
          cl = ClassLoader.getSystemClassLoader();
        }
        catch (Throwable ex) {
          // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
        }
      }
    }
    return cl;
  }

  /**
   * Determine whether the {@link Class} identified by the supplied name is present
   * and can be loaded. Will return {@code false} if either the class or
   * one of its dependencies is not present or cannot be loaded.
   * <p> use default class loader
   *
   * @param className the name of the class to check
   * @return whether the specified class is present (including all of its
   * superclasses and interfaces)
   * @throws IllegalStateException if the corresponding class is resolvable but
   * there was a readability mismatch in the inheritance hierarchy of the class
   * (typically a missing dependency declaration in a Jigsaw module definition
   * for a superclass or interface implemented by the class to be checked here)
   */
  public static boolean isPresent(String className) {
    return isPresent(className, (ClassLoader) null);
  }

  /**
   * Determine whether the {@link Class} identified by the supplied name is present
   * and can be loaded. Will return {@code false} if either the class or
   * one of its dependencies is not present or cannot be loaded.
   *
   * @param className the name of the class to check
   * @param classLoader the class loader to use
   * (may be {@code null} which indicates the default class loader)
   * @return whether the specified class is present (including all of its
   * superclasses and interfaces)
   * @throws IllegalStateException if the corresponding class is resolvable but
   * there was a readability mismatch in the inheritance hierarchy of the class
   * (typically a missing dependency declaration in a Jigsaw module definition
   * for a superclass or interface implemented by the class to be checked here)
   */
  public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
    try {
      if (classLoader == null) {
        classLoader = getDefaultClassLoader();
      }
      Class.forName(className, false, classLoader);
      return true;
    }
    catch (IllegalAccessError err) {
      throw new IllegalStateException(
              "Readability mismatch in inheritance hierarchy of class [%s]: %s".formatted(className, err.getMessage()), err);
    }
    catch (Throwable ex) {
      // Typically, ClassNotFoundException or NoClassDefFoundError...
      return false;
    }
  }

  /**
   * Determine whether the {@link Class} identified by the supplied name is present
   * and can be loaded. Will return {@code false} if either the class or
   * one of its dependencies is not present or cannot be loaded.
   *
   * @param className the name of the class to check
   * @param loaderSource use same class loader from loaderSource to use
   * (may be {@code null} which indicates the default class loader)
   * @return whether the specified class is present (including all of its
   * superclasses and interfaces)
   * @throws IllegalStateException if the corresponding class is resolvable but
   * there was a readability mismatch in the inheritance hierarchy of the class
   * (typically a missing dependency declaration in a Jigsaw module definition
   * for a superclass or interface implemented by the class to be checked here)
   */
  public static boolean isPresent(String className, Class<?> loaderSource) {
    return isPresent(className, loaderSource.getClassLoader());
  }

  /**
   * If the class is dynamically generated then the user class will be extracted
   * in a specific format.
   *
   * @param syntheticClass input test class
   * @return The user class
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getUserClass(Class<T> syntheticClass) {
    Assert.notNull(syntheticClass, "syntheticClass is required");
    if (syntheticClass.getName().lastIndexOf(CGLIB_CLASS_SEPARATOR) > -1) {
      Class<?> superclass = syntheticClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return (Class<T>) superclass;
      }
    }
    return syntheticClass;
  }

  //

  /**
   * Determine the name of the package of the given class,
   * e.g. "java.lang" for the {@code java.lang.String} class.
   *
   * @param clazz the class
   * @return the package name, or the empty String if the class
   * is defined in the default package
   */
  public static String getPackageName(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    return getPackageName(clazz.getName());
  }

  /**
   * Determine the name of the package of the given fully-qualified class name,
   * e.g. "java.lang" for the {@code java.lang.String} class name.
   *
   * @param fqClassName the fully-qualified class name
   * @return the package name, or the empty String if the class
   * is defined in the default package
   */
  public static String getPackageName(String fqClassName) {
    Assert.notNull(fqClassName, "Class name is required");
    int lastDotIndex = fqClassName.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    return lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "";
  }

  /**
   * Return the qualified name of the given class: usually simply
   * the class name, but component type class name + "[]" for arrays.
   *
   * @param clazz the class
   * @return the qualified name of the class
   */
  public static String getQualifiedName(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    return clazz.getTypeName();
  }

  /**
   * Get the class name without the qualified package name.
   *
   * @param className the className to get the short name for
   * @return the class name of the class without the package name
   * @throws IllegalArgumentException if the className is empty
   */
  public static String getShortName(String className) {
    Assert.hasLength(className, "Class name must not be empty");
    int lastDotIndex = className.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
    if (nameEndIndex == -1) {
      nameEndIndex = className.length();
    }
    String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
    shortName = shortName.replace(INNER_CLASS_SEPARATOR, Constant.PACKAGE_SEPARATOR);
    return shortName;
  }

  /**
   * Get the class name without the qualified package name.
   *
   * @param clazz the class to get the short name for
   * @return the class name of the class without the package name
   */
  public static String getShortName(Class<?> clazz) {
    return getShortName(getQualifiedName(clazz));
  }

  /**
   * Check if the given class represents a primitive wrapper,
   * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double, or
   * Void.
   *
   * @param clazz the class to check
   * @return whether the given class is a primitive wrapper class
   */
  public static boolean isPrimitiveWrapper(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    return primitiveWrapperTypeMap.containsKey(clazz);
  }

  /**
   * Check if the given class represents a primitive (i.e. boolean, byte,
   * char, short, int, long, float, or double), {@code void}, or a wrapper for
   * those types (i.e. Boolean, Byte, Character, Short, Integer, Long, Float,
   * Double, or Void).
   *
   * @param clazz the class to check
   * @return {@code true} if the given class represents a primitive, void, or
   * a wrapper class
   */
  public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
    Assert.notNull(clazz, "Class is required");
    return clazz.isPrimitive() || isPrimitiveWrapper(clazz);
  }

  /**
   * Determine if the given type represents either {@code Void} or {@code void}.
   *
   * @param type the type to check
   * @return {@code true} if the type represents {@code Void} or {@code void}
   * @see Void
   * @see Void#TYPE
   */
  public static boolean isVoidType(Class<?> type) {
    return (type == void.class || type == Void.class);
  }

  /**
   * @param type the type to check
   * @return whether the given type represents a "simple" value type,
   * suggesting value-based data binding and {@code toString} output
   */
  public static boolean isSimpleValueType(Class<?> type) {
    return !isVoidType(type)
            && (
            isPrimitiveOrWrapper(type)
                    || URI.class == type
                    || URL.class == type
                    || UUID.class == type
                    || Class.class == type
                    || Locale.class == type
                    || Pattern.class == type
                    || Date.class.isAssignableFrom(type)
                    || Enum.class.isAssignableFrom(type)
                    || File.class.isAssignableFrom(type)
                    || Path.class.isAssignableFrom(type)
                    || Number.class.isAssignableFrom(type)
                    || ZoneId.class.isAssignableFrom(type)
                    || Charset.class.isAssignableFrom(type)
                    || TimeZone.class.isAssignableFrom(type)
                    || Temporal.class.isAssignableFrom(type)
                    || Currency.class.isAssignableFrom(type)
                    || InetAddress.class.isAssignableFrom(type)
                    || CharSequence.class.isAssignableFrom(type)
    );
  }

}
