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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/26 22:48
 */
public class PolarisProperties {

  private static final String PROPERTIES_RESOURCE_LOCATION = "polaris.properties";

  // local application properties file
  private static final Properties localProperties = new Properties();

  static {
    try {
      ClassLoader cl = PolarisProperties.class.getClassLoader();
      URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
              ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
      if (url != null) {
        try (InputStream is = url.openStream()) {
          localProperties.load(is);
        }
      }
    }
    catch (IOException ex) {
      System.err.println("Could not load 'today.properties' file from local classpath: " + ex);
    }
  }

  /**
   * Retrieve the flag for the given property key.
   *
   * @param key the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise
   */
  public static boolean getFlag(String key) {
    String property = getProperty(key);
    return Boolean.parseBoolean(property);
  }

  /**
   * Retrieve the flag for the given property key.
   * <p>
   * If there isn't a key returns defaultFlag
   * </p>
   *
   * @param key the property key
   * @return {@code true} if the property is set to "true", {@code} false
   * otherwise ,If there isn't a key returns defaultFlag
   */
  public static boolean getFlag(String key, boolean defaultFlag) {
    String property = getProperty(key);
    return StringUtils.isEmpty(property) ? defaultFlag : Boolean.parseBoolean(property);
  }

  /**
   * Programmatically set a local flag to "true", overriding an
   * entry in the {@link #PROPERTIES_RESOURCE_LOCATION} file (if any).
   *
   * @param key the property key
   */
  public static void setFlag(String key) {
    localProperties.put(key, Boolean.TRUE.toString());
  }

  /**
   * Programmatically set a local property, overriding an entry in the
   * {@link #PROPERTIES_RESOURCE_LOCATION} file (if any).
   *
   * @param key the property key
   * @param value the associated property value, or {@code null} to reset it
   */
  public static void setProperty(String key, @Nullable String value) {
    if (value != null) {
      localProperties.setProperty(key, value);
    }
    else {
      localProperties.remove(key);
    }
  }

  /**
   * Retrieve the property value for the given key, checking local
   * properties first and falling back to JVM-level system properties.
   *
   * @param key the property key
   * @return the associated property value, or {@code null} if none found
   */
  @Nullable
  public static String getProperty(String key) {
    String value = localProperties.getProperty(key);

    if (value == null) {
      try {
        value = System.getProperty(key);
      }
      catch (Throwable ex) {
        System.err.printf("Could not retrieve system property '%s': %s%n", key, ex);
      }
    }

    return value;
  }

  /**
   * Retrieve the property value for the given key, checking local
   * properties first and falling back to JVM-level system properties.
   *
   * @param key the name of the system property.
   * @param def a default value.
   * @return the string value of the system property,
   * or the default value if there is no property with that key.
   * @see #setProperty
   * @see java.lang.System#getProperties()
   */
  public static String getProperty(String key, String def) {
    String property = getProperty(key);
    if (property == null) {
      property = def;
    }
    return property;
  }

  /**
   * Determines the integer value of the property with the specified name.
   *
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>If there is no property with the specified name, if the
   * specified name is empty or {@code null}, or if the property
   * does not have the correct numeric format, then {@code null} is
   * returned.
   *
   * <p>In other words, this method returns an {@code Integer}
   * object equal to the value of:
   *
   * <blockquote>
   * {@code getInteger(nm, null)}
   * </blockquote>
   *
   * @param key property name.
   * @return the {@code Integer} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  @Nullable
  public static Integer getInteger(String key) {
    return getInteger(key, null);
  }

  /**
   * Determines the integer value of the system property with the
   * specified name.
   *
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>The second argument is the default value. An {@code Integer} object
   * that represents the value of the second argument is returned if there
   * is no property of the specified name, if the property does not have
   * the correct numeric format, or if the specified name is empty or
   * {@code null}.
   *
   * <p>In other words, this method returns an {@code Integer} object
   * equal to the value of:
   *
   * <blockquote>
   * {@code getInteger(nm, new Integer(val))}
   * </blockquote>
   *
   * but in practice it may be implemented in a manner such as:
   *
   * <blockquote><pre>
   * Integer result = getInteger(nm, null);
   * return (result == null) ? new Integer(val) : result;
   * </pre></blockquote>
   *
   * to avoid the unnecessary allocation of an {@code Integer}
   * object when the default value is not needed.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Integer} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  public static int getInt(String key, int val) {
    Integer result = getInteger(key, null);
    return result == null ? val : result;
  }

  /**
   * Returns the integer value of the property with the
   * specified name.
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   * <p>
   * in summary:
   *
   * <ul><li>If the property value begins with the two ASCII characters
   *         {@code 0x} or the ASCII character {@code #}, not
   *      followed by a minus sign, then the rest of it is parsed as a
   *      hexadecimal integer exactly as by the method
   *      {@link Integer#valueOf(java.lang.String, int)} with radix 16.
   * <li>If the property value begins with the ASCII character
   *     {@code 0} followed by another character, it is parsed as an
   *     octal integer exactly as by the method
   *     {@link Integer#valueOf(java.lang.String, int)} with radix 8.
   * <li>Otherwise, the property value is parsed as a decimal integer
   * exactly as by the method {@link Integer#valueOf(java.lang.String, int)}
   * with radix 10.
   * </ul>
   *
   * <p>The second argument is the default value. The default value is
   * returned if there is no property of the specified name, if the
   * property does not have the correct numeric format, or if the
   * specified name is empty or {@code null}.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Integer} value of the property.
   * {@link System#getProperty(String) System.getProperty}
   * @see System#getProperty(java.lang.String)
   * @see System#getProperty(java.lang.String, java.lang.String)
   * @see Integer#decode(String)
   */
  @Nullable
  public static Integer getInteger(String key, @Nullable Integer val) {
    try {
      String v = getProperty(key);
      if (v != null) {
        try {
          return Integer.decode(v);
        }
        catch (NumberFormatException ignored) { }
      }
    }
    catch (IllegalArgumentException | NullPointerException ignored) { }
    return val;
  }

  /**
   * Determines the {@code long} value of the system property
   * with the specified name.
   *
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>If there is no property with the specified name, if the
   * specified name is empty or {@code null}, or if the property
   * does not have the correct numeric format, then {@code null} is
   * returned.
   *
   * <p>In other words, this method returns a {@code Long} object
   * equal to the value of:
   *
   * <blockquote>
   * {@code getLong(nm, null)}
   * </blockquote>
   *
   * @param nm property name.
   * @return the {@code Long} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  @Nullable
  public static Long getLong(String nm) {
    return getLong(nm, null);
  }

  /**
   * Determines the {@code long} value of the system property
   * with the specified name.
   *
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   *
   * <p>The second argument is the default value. A {@code Long} object
   * that represents the value of the second argument is returned if there
   * is no property of the specified name, if the property does not have
   * the correct numeric format, or if the specified name is empty or null.
   *
   * <p>In other words, this method returns a {@code Long} object equal
   * to the value of:
   *
   * <blockquote>
   * {@code getLong(nm, new Long(val))}
   * </blockquote>
   *
   * but in practice it may be implemented in a manner such as:
   *
   * <blockquote><pre>
   * Long result = getLong(nm, null);
   * return (result == null) ? new Long(val) : result;
   * </pre></blockquote>
   *
   * to avoid the unnecessary allocation of a {@code Long} object when
   * the default value is not needed.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Long} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see java.lang.System#getProperty(java.lang.String)
   * @see java.lang.System#getProperty(java.lang.String, java.lang.String)
   */
  public static long getLong(String key, long val) {
    Long result = getLong(key, null);
    return result == null ? val : result;
  }

  /**
   * Returns the {@code long} value of the system property with the specified name.
   * <p>The first argument is treated as the name of a today.properties or
   * system property. properties are accessible through the {@link #getProperty(java.lang.String)}
   * method. The string value of this property is then interpreted as an integer
   * value using the grammar supported by {@link Integer#decode decode} and
   * an {@code Integer} object representing this value is returned.
   * <p>
   * in summary:
   *
   * <ul>
   * <li>If the property value begins with the two ASCII characters
   * {@code 0x} or the ASCII character {@code #}, not followed by
   * a minus sign, then the rest of it is parsed as a hexadecimal integer
   * exactly as for the method {@link Long#valueOf(java.lang.String, int)}
   * with radix 16.
   * <li>If the property value begins with the ASCII character
   * {@code 0} followed by another character, it is parsed as
   * an octal integer exactly as by the method {@link
   * Long#valueOf(java.lang.String, int)} with radix 8.
   * <li>Otherwise the property value is parsed as a decimal
   * integer exactly as by the method
   * {@link Long#valueOf(java.lang.String, int)} with radix 10.
   * </ul>
   *
   * <p>Note that, in every case, neither {@code L}
   * ({@code '\u005Cu004C'}) nor {@code l}
   * ({@code '\u005Cu006C'}) is permitted to appear at the end
   * of the property value as a type indicator, as would be
   * permitted in Java programming language source code.
   *
   * <p>The second argument is the default value. The default value is
   * returned if there is no property of the specified name, if the
   * property does not have the correct numeric format, or if the
   * specified name is empty or {@code null}.
   *
   * @param key property name.
   * @param val default value.
   * @return the {@code Long} value of the property.
   * @throws SecurityException for the same reasons as
   * {@link System#getProperty(String) System.getProperty}
   * @see System#getProperty(java.lang.String)
   * @see System#getProperty(java.lang.String, java.lang.String)
   */
  @Nullable
  public static Long getLong(String key, @Nullable Long val) {
    try {
      String v = getProperty(key);
      if (v != null) {
        try {
          return Long.decode(v);
        }
        catch (NumberFormatException ignored) { }
      }
    }
    catch (IllegalArgumentException | NullPointerException ignored) { }

    return val;
  }

}
