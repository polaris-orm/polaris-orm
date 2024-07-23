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
import java.lang.reflect.Method;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/23 14:23
 */
public class BeanInstantiationException extends NestedRuntimeException {

  private final Class<?> beanClass;

  private final Method constructingMethod;

  private final Constructor<?> constructor;

  public BeanInstantiationException(String msg, Throwable cause) {
    super(msg, cause);
    this.beanClass = null;
    this.constructor = null;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Class<?> beanClass, String msg) {
    this(beanClass, msg, null);
  }

  public BeanInstantiationException(Class<?> beanClass, String msg, Throwable cause) {
    super("Failed to instantiate [%s]: %s".formatted(beanClass.getName(), msg), cause);
    this.beanClass = beanClass;
    this.constructor = null;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Constructor<?> constructor, String msg, Throwable cause) {
    super("Failed to instantiate [%s]: %s".formatted(constructor.getDeclaringClass().getName(), msg), cause);
    this.beanClass = constructor.getDeclaringClass();
    this.constructor = constructor;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Method constructingMethod, String msg, Throwable cause) {
    super("Failed to instantiate [%s]: %s".formatted(constructingMethod.getReturnType().getName(), msg), cause);
    this.beanClass = constructingMethod.getReturnType();
    this.constructor = null;
    this.constructingMethod = constructingMethod;
  }

  /**
   * Return the offending bean class (never {@code null}).
   *
   * @return the class that was to be instantiated
   */
  @Nullable
  public Class<?> getBeanClass() {
    return this.beanClass;
  }

  /**
   * Return the offending constructor, if known.
   *
   * @return the constructor in use, or {@code null} in case of a factory method
   * or in case of default instantiation
   */
  @Nullable
  public Constructor<?> getConstructor() {
    return this.constructor;
  }

  /**
   * Return the delegate for bean construction purposes, if known.
   *
   * @return the method in use (typically a static factory method), or
   * {@code null} in case of constructor-based instantiation
   */
  @Nullable
  public Method getConstructingMethod() {
    return this.constructingMethod;
  }

}

