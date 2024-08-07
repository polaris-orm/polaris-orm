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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.polaris.Constant;
import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.ReflectionUtils;
import cn.taketoday.polaris.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public abstract class Property implements Member, AnnotatedElement {

  private static final ConcurrentHashMap<Property, Annotation[]> annotationCache = new ConcurrentHashMap<>();

  // Nullable
  protected Field field;

  protected final String name;

  /**  */
  @Nullable
  protected final Method readMethod;

  /**  */
  @Nullable
  protected final Method writeMethod;

  @Nullable
  private Class<?> propertyType;

  @Nullable
  private Class<?> declaringClass;

  private boolean fieldIsNull;

  @Nullable
  private transient Annotation[] annotations;

  public Property(String name, Field field) {
    this.name = name;
    this.field = field;
    this.propertyType = field.getType();
    this.readMethod = null;
    this.writeMethod = null;
  }

  public Property(@Nullable String name, @Nullable Method readMethod,
          @Nullable Method writeMethod, @Nullable Class<?> declaringClass) {
    if (readMethod == null && writeMethod == null) {
      throw new IllegalArgumentException("Property '%s' in '%s' is neither readable nor writeable"
              .formatted(name, declaringClass));
    }
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = declaringClass;
    if (name == null) {
      name = ReflectionUtils.getPropertyName(readMethod, writeMethod);
    }
    this.name = name;
  }

  Property(Class<?> declaringClass, String name, Field field,
          @Nullable Method readMethod, @Nullable Method writeMethod) {
    this.name = name;
    this.field = field;
    this.readMethod = readMethod;
    this.writeMethod = writeMethod;
    this.declaringClass = declaringClass;
  }

  /**
   * Determines if the specified {@code Object} is assignment-compatible
   * with the object represented by this {@code Property}.  This method is
   * the dynamic equivalent of the Java language {@code instanceof}
   * operator. The method returns {@code true} if the specified
   * {@code Object} argument is non-null and can be cast to the
   * reference type represented by this {@code Property} object without
   * raising a {@code ClassCastException.} It returns {@code false}
   * otherwise.
   *
   * <p> Specifically, if this {@code Property} object represents a
   * declared class, this method returns {@code true} if the specified
   * {@code Object} argument is an instance of the represented class (or
   * of any of its subclasses); it returns {@code false} otherwise. If
   * this {@code Property} object represents an array class, this method
   * returns {@code true} if the specified {@code Object} argument
   * can be converted to an object of the array class by an identity
   * conversion or by a widening reference conversion; it returns
   * {@code false} otherwise. If this {@code Property} object
   * represents an interface, this method returns {@code true} if the
   * class or any superclass of the specified {@code Object} argument
   * implements this interface; it returns {@code false} otherwise. If
   * this {@code Property} object represents a primitive type, this method
   * returns {@code false}.
   *
   * @param value the object to check
   * @return true if {@code obj} is an instance of this property-type
   * @see Class#isInstance(Object)
   */
  public boolean isInstance(Object value) {
    return getType().isInstance(value);
  }

  /**
   * Returns a {@code Class} object that identifies the
   * declared type for the field represented by this
   * {@code Field} object.
   *
   * @return a {@code Class} object identifying the declared
   * type of the field represented by this object
   */
  public Class<?> getType() {
    if (propertyType == null) {
      if (readMethod != null) {
        propertyType = readMethod.getReturnType();
      }
      else if (writeMethod != null) {
        propertyType = writeMethod.getParameterTypes()[0];
      }
      else if (field != null) {
        propertyType = field.getType();
      }
      else {
        throw new IllegalStateException("should never get here");
      }
    }
    return propertyType;
  }

  /**
   * get or find a Field
   *
   * @return returns null show that isSynthetic
   */
  @Nullable
  public Field getField() {
    if (field == null && !fieldIsNull) {
      String name = getName();
      if (StringUtils.isEmpty(name)) {
        return null;
      }
      Class<?> declaringClass = getDeclaringClass();
      if (declaringClass != null) {
        field = ReflectionUtils.findField(declaringClass, name);
        if (field == null) {
          field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
          if (field == null) {
            field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
          }
        }
      }
      fieldIsNull = field == null;
    }
    return field;
  }

  /**
   * original property name
   */
  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getModifiers() {
    if (readMethod != null) {
      return readMethod.getModifiers();
    }
    else if (writeMethod != null) {
      return writeMethod.getModifiers();
    }
    return Modifier.PRIVATE;
  }

  @Override
  public boolean isSynthetic() {
    if (readMethod != null) {
      return readMethod.isSynthetic();
    }
    else if (writeMethod != null) {
      return writeMethod.isSynthetic();
    }
    return false;
  }

  /**
   * read only
   */
  public boolean isReadOnly() {
    return writeMethod == null;
  }

  /**
   * can write
   */
  public boolean isWriteable() {
    return writeMethod != null;
  }

  /**
   * can read
   */
  public boolean isReadable() {
    // todo maybe can access field
    return readMethod != null;
  }

  /**
   * is primitive
   *
   * @see Boolean#TYPE
   * @see Character#TYPE
   * @see Byte#TYPE
   * @see Short#TYPE
   * @see Integer#TYPE
   * @see Long#TYPE
   * @see Float#TYPE
   * @see Double#TYPE
   * @see Void#TYPE
   * @see Class#isPrimitive()
   */
  public boolean isPrimitive() {
    return getType().isPrimitive();
  }

  /**
   * Return whether this property which can be {@code null}:
   * either in the form of any variant of a parameter-level {@code Nullable}
   * annotation (such as from JSR-305 or the FindBugs set of annotations),
   * or a language-level nullable type declaration
   */
  public boolean isNullable() {
    for (Annotation ann : getAnnotations(false)) {
      if ("Nullable".equals(ann.annotationType().getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the {@code Class} object representing the class or interface
   * that declares the field represented by this {@code Field} object.
   */
  @Override
  public Class<?> getDeclaringClass() {
    if (declaringClass == null) {
      if (readMethod != null) {
        declaringClass = readMethod.getDeclaringClass();
      }
      else if (writeMethod != null) {
        declaringClass = writeMethod.getDeclaringClass();
      }
    }
    return declaringClass;
  }

  @Nullable
  public Method getReadMethod() {
    return readMethod;
  }

  @Nullable
  public Method getWriteMethod() {
    return writeMethod;
  }

  // AnnotatedElement

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    for (Annotation annotation : getAnnotations(false)) {
      if (annotation.annotationType() == annotationClass) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Nullable
  @SuppressWarnings("unchecked")
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    for (Annotation annotation : getAnnotations(false)) {
      if (annotation.annotationType() == annotationClass) {
        return (T) annotation;
      }
    }
    return null;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getAnnotations(true);
  }

  @Override
  public Annotation[] getAnnotations() {
    return getAnnotations(true);
  }

  private Annotation[] getAnnotations(boolean clone) {
    Annotation[] annotations = this.annotations;
    if (annotations == null) {
      annotations = resolveAnnotations();
      this.annotations = annotations;
    }
    if (clone && annotations.length > 0) {
      return annotations.clone();
    }
    return annotations;
  }

  private Annotation[] resolveAnnotations() {
    Annotation[] annotations = annotationCache.get(this);
    if (annotations == null) {
      var annotationMap = new LinkedHashMap<Class<? extends Annotation>, Annotation>();
      addAnnotationsToMap(annotationMap, getReadMethod());
      addAnnotationsToMap(annotationMap, getWriteMethod());
      addAnnotationsToMap(annotationMap, getField());
      annotations = annotationMap.isEmpty() ? Constant.EMPTY_ANNOTATIONS
              : annotationMap.values().toArray(Constant.EMPTY_ANNOTATIONS);
      annotationCache.put(this, annotations);
    }
    return annotations;
  }

  private void addAnnotationsToMap(Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {
    if (object != null) {
      for (Annotation annotation : object.getAnnotations()) {
        annotationMap.put(annotation.annotationType(), annotation);
      }
    }
  }

  //---------------------------------------------------------------------
  // Override method of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof Property property) {
      return Objects.equals(name, property.name)
              && Objects.equals(readMethod, property.readMethod)
              && Objects.equals(writeMethod, property.writeMethod)
              && Objects.equals(propertyType, property.propertyType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, name, readMethod, writeMethod);
  }

  @Override
  public String toString() {
    return getType().getSimpleName() + " " + getName();
  }

}
