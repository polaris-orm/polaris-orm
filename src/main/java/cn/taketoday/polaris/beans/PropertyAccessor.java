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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.ReflectionException;
import cn.taketoday.polaris.util.ReflectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/27 12:32
 */
public abstract class PropertyAccessor {

  public abstract Object get(Object obj) throws ReflectionException;

  public abstract void set(Object obj, Object value) throws ReflectionException;

  /**
   * read-only ?
   */
  public boolean isReadOnly() {
    return false;
  }

  // static

  /**
   * PropertyAccessor
   *
   * @param field Field
   * @return PropertyAccessor
   */
  public static PropertyAccessor forField(Field field) {
    // readMethod == null && setMethod == null
    return forReflective(field);
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective PropertyAccessor
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#getField(Field, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  public static PropertyAccessor forReflective(Field field) {
    return forReflective(field, null, null);
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective PropertyAccessor
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#getField(Field, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  public static PropertyAccessor forReflective(@Nullable Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    return new ReflectivePropertyAccessor(field, readMethod, writeMethod);
  }

}
