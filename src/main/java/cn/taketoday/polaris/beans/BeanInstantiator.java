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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.reflect.ReflectionException;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/23 14:22
 */
public abstract class BeanInstantiator {

  /**
   * get internal Constructor
   *
   * @see Constructor
   */
  @Nullable
  public Constructor<?> getConstructor() {
    return null;
  }

  /**
   * Invoke {@link java.lang.reflect.Constructor} with given args
   *
   * @return returns T
   * @throws BeanInstantiationException cannot instantiate a bean
   */
  public final Object instantiate(@Nullable Object[] args) {
    try {
      return doInstantiate(args);
    }
    catch (BeanInstantiationException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new BeanInstantiationException(this + " cannot instantiate a bean", e);
    }
  }

  // internal new-instance impl @since 4.0
  protected abstract Object doInstantiate(@Nullable Object[] args)
          throws Throwable;

  //---------------------------------------------------------------------
  // Static Factory Methods
  //---------------------------------------------------------------------

  public static BeanInstantiator forClass(final Class<?> targetClass) {
    Constructor<?> suitableConstructor = BeanUtils.obtainConstructor(targetClass);
    return forConstructor(suitableConstructor);
  }

  /**
   * use default constructor
   *
   * @param target target class
   */
  public static <T> BeanInstantiator forConstructor(final Class<T> target) {
    Assert.notNull(target, "target class is required");
    try {
      final Constructor<T> constructor = target.getDeclaredConstructor();
      return forConstructor(constructor);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException(
              "Target class: '%s' has no default constructor".formatted(target), e);
    }
  }

  /**
   * use BeanInstantiatorGenerator to create bytecode Constructor access
   * or fallback to java reflect Constructor cause private access
   *
   * @param constructor java reflect Constructor
   * @return BeanInstantiator to construct target T
   */
  public static BeanInstantiator forConstructor(Constructor<?> constructor) {
    return new BeanInstantiatorGenerator(constructor).create();
  }

  /**
   * @param constructor java reflect Constructor
   * @return ReflectiveConstructor
   * @see ReflectiveInstantiator
   */
  public static ConstructorAccessor forReflective(Constructor<?> constructor) {
    Assert.notNull(constructor, "Constructor is required");
    ReflectionUtils.makeAccessible(constructor);
    return new ReflectiveInstantiator(constructor);
  }

}
