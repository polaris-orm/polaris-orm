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

import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Optional;

import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.ReflectionUtils;

/**
 * Field is first considered then readMethod
 * <p>
 * AnnotatedElement -> Field -> readMethod -> writeMethod
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #isWriteable()
 * @see #isReadable()
 * @since 1.0
 */
public sealed class BeanProperty extends Property implements Member, AnnotatedElement, Serializable permits FieldBeanProperty {

  @Serial
  private static final long serialVersionUID = 1L;

  private transient PropertyAccessor propertyAccessor;

  private transient BeanInstantiator instantiator;

  BeanProperty(String name, Field field) {
    super(name, field);
    this.field = field;
  }

  BeanProperty(Field field) {
    this(field.getName(), field);
  }

  BeanProperty(@Nullable String name, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    super(name, readMethod, writeMethod, declaringClass);
  }

  BeanProperty(PropertyDescriptor descriptor, Class<?> declaringClass) {
    super(descriptor.getName(), descriptor.getReadMethod(), descriptor.getWriteMethod(), declaringClass);
  }

  /**
   * instantiate property value
   */
  public Object instantiate() {
    return instantiate(null);
  }

  /**
   * instantiate property value
   */
  public Object instantiate(@Nullable Object[] args) {
    BeanInstantiator constructor = this.instantiator;
    if (constructor == null) {
      Class<?> type = getType();
      if (BeanUtils.isSimpleValueType(type)) {
        throw new BeanInstantiationException(type, "Cannot be instantiated a simple type");
      }
      constructor = BeanInstantiator.forConstructor(type);
      this.instantiator = constructor;
    }
    return constructor.instantiate(args);
  }

  /**
   * get property of this {@code object}
   *
   * @param object object
   * @return property value
   */
  public Object getValue(Object object) {
    return obtainAccessor().get(object);
  }

  /**
   *
   */
  public final void setValue(Object obj, Object value) {
    value = handleOptional(value, getType());
    setDirectly(obj, value);
  }

  // @since 1.0
  @Nullable
  static Object handleOptional(Object value, Class<?> propertyType) {
    // convertedValue == null
    if (value == null && propertyType == Optional.class) {
      value = Optional.empty();
    }
    return value;
  }

  /**
   *
   */
  public final void setDirectly(Object obj, Object value) {
    obtainAccessor().set(obj, value);
  }

  // PropertyAccessor

  public final PropertyAccessor obtainAccessor() {
    PropertyAccessor accessor = this.propertyAccessor;
    if (accessor == null) {
      accessor = createAccessor();
      this.propertyAccessor = accessor;
    }
    return accessor;
  }

  protected PropertyAccessor createAccessor() {
    Field field = getField();
    if (field == null) {
      return PropertyAccessor.forReflective(null, readMethod, writeMethod);
    }
    return PropertyAccessor.forField(field);
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  // static

  /**
   * @since 1.0
   */
  public static BeanProperty valueOf(Field field) {
    Assert.notNull(field, "field is required");
    return new FieldBeanProperty(field);
  }

  public static BeanProperty valueOf(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    Assert.state(field != null, () -> "bean property not found: " + name);
    return new FieldBeanProperty(field);
  }

  /**
   * @param writeMethod can be null (read only)
   */
  public static BeanProperty valueOf(Method readMethod, @Nullable Method writeMethod) {
    return valueOf(readMethod, writeMethod, null);
  }

  /**
   * construct with read-method and write-method
   *
   * @param writeMethod can be null (read only)
   * @param declaringClass the implementation class
   */
  public static BeanProperty valueOf(@Nullable Method readMethod, @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    return valueOf(null, readMethod, writeMethod, declaringClass);
  }

  /**
   * construct with read-method and write-method and property-name
   * <p>
   * <b>NOTE:</b> read-write method cannot be null at the same time
   * </p>
   *
   * @param propertyName user specified property name
   * @param writeMethod can be null (read only)
   * @param declaringClass the implementation class
   */
  public static BeanProperty valueOf(@Nullable String propertyName, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalStateException("Property is neither readable nor writeable");
    }
    return new BeanProperty(propertyName, readMethod, writeMethod, declaringClass);
  }

}
