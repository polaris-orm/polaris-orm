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
import cn.taketoday.polaris.util.ReflectionUtils;

/**
 * java reflect {@link Field} implementation
 *
 * @author TODAY 2020/9/11 17:56
 */
final class ReflectivePropertyAccessor extends PropertyAccessor {

  @Nullable
  private final Field field;

  private final Method readMethod;
  private final Method writeMethod;

  ReflectivePropertyAccessor(@Nullable Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    this.field = field;
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    if (field != null) {
      ReflectionUtils.makeAccessible(field);
    }
  }

  @Override
  public Object get(final Object obj) {
    if (field != null) {
      return ReflectionUtils.getField(field, obj);
    }
    return ReflectionUtils.invokeMethod(readMethod, obj);
  }

  @Override
  public void set(Object obj, Object value) {
    if (field != null) {
      ReflectionUtils.setField(field, obj, value);
    }
    else {
      ReflectionUtils.invokeMethod(writeMethod, obj, value);
    }
  }

}
