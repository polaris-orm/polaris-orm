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

import java.io.Serial;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.reflect.PropertyAccessor;

/**
 * Field based BeanProperty
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/24 15:16
 */
public final class FieldBeanProperty extends BeanProperty {

  @Serial
  private static final long serialVersionUID = 1L;

  FieldBeanProperty(Field field) {
    super(field);
  }

  @Override
  protected PropertyAccessor createAccessor() {
    return PropertyAccessor.forField(field);
  }

  protected TypeDescriptor createDescriptor() {
    ResolvableType resolvableType = ResolvableType.forField(field);
    return new TypeDescriptor(resolvableType, resolvableType.resolve(getType()), this);
  }

  @Override
  protected ResolvableType createResolvableType() {
    return ResolvableType.forField(field);
  }

  @Override
  public int getModifiers() {
    return field.getModifiers();
  }

  @Override
  public boolean isSynthetic() {
    return field.isSynthetic();
  }

  @Override
  public boolean isReadOnly() {
    return Modifier.isFinal(field.getModifiers());
  }

  @Override
  public boolean isReadable() {
    return true;
  }

  @Override
  public boolean isWriteable() {
    return !isReadOnly();
  }

  @Override
  public Field getField() {
    return field;
  }

  @Override
  public Class<?> getType() {
    return field.getType();
  }

  @Override
  public Class<?> getDeclaringClass() {
    return field.getDeclaringClass();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof BeanProperty property) {
      return Objects.equals(field, property.getField());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(field);
  }

  @Override
  public String toString() {
    return getType().getSimpleName() + " " + getName();
  }

}