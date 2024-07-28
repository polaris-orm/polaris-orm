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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import cn.taketoday.bytecode.proxy.Enhancer;
import cn.taketoday.bytecode.proxy.MethodInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/28 16:40
 */
class ClassUtilsTests {
  final ClassLoader classLoader = getClass().getClassLoader();

  public static class NestedClass {

    static boolean noArgCalled;
    static boolean argCalled;
    static boolean overloadedCalled;

    public static void staticMethod() {
      noArgCalled = true;
    }

    public static void staticMethod(String anArg) {
      overloadedCalled = true;
    }

    public static void argStaticMethod(String anArg) {
      argCalled = true;
    }
  }

  private static class INNER {

  }

  @Test
  void isPresent() {
    assertThat(ClassUtils.isPresent("java.lang.String", classLoader)).isTrue();
    assertThat(ClassUtils.isPresent("java.lang.MySpecialString", classLoader)).isFalse();
    assert ClassUtils.isPresent("java.lang.Float");
    assert !ClassUtils.isPresent("Float");
  }

  // v2.1.7 test code
  // ----------------------------------------

  @Test
  void testGetUserClass() {
    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(getClass()));

    Enhancer enhancer = new Enhancer();

    enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> null);

    enhancer.setSuperclass(ClassUtilsTests.class);

    final Object create = enhancer.create();

    assertEquals(ClassUtilsTests.class, ClassUtils.getUserClass(create.getClass()));
  }

  // ------


  @ParameterizedTest
  @WrapperTypes
  void isPrimitiveWrapper(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveWrapper(type)).isTrue();
  }

  @ParameterizedTest
  @PrimitiveTypes
  void isPrimitiveOrWrapperWithPrimitive(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
  }

  @ParameterizedTest
  @WrapperTypes
  void isPrimitiveOrWrapperWithWrapper(Class<?> type) {
    assertThat(ClassUtils.isPrimitiveOrWrapper(type)).isTrue();
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ValueSource(classes = { Boolean.class, Character.class, Byte.class, Short.class,
          Integer.class, Long.class, Float.class, Double.class, Void.class })
  @interface WrapperTypes {
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ValueSource(classes = { boolean.class, char.class, byte.class, short.class,
          int.class, long.class, float.class, double.class, void.class })
  @interface PrimitiveTypes {
  }

  @Test
  void getShortName() {
    String className = ClassUtils.getShortName(getClass());
    assertThat(className).as("Class name did not match").isEqualTo("ClassUtilsTests");
  }

  @Test
  void getShortNameForObjectArrayClass() {
    String className = ClassUtils.getShortName(Object[].class);
    assertThat(className).as("Class name did not match").isEqualTo("Object[]");
  }

  @Test
  void getShortNameForMultiDimensionalObjectArrayClass() {
    String className = ClassUtils.getShortName(Object[][].class);
    assertThat(className).as("Class name did not match").isEqualTo("Object[][]");
  }

  @Test
  void getShortNameForPrimitiveArrayClass() {
    String className = ClassUtils.getShortName(byte[].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[]");
  }

  @Test
  void getShortNameForMultiDimensionalPrimitiveArrayClass() {
    String className = ClassUtils.getShortName(byte[][][].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[][][]");
  }

  @Test
  void getShortNameForNestedClass() {
    String className = ClassUtils.getShortName(NestedClass.class);
    assertThat(className).as("Class name did not match").isEqualTo("ClassUtilsTests.NestedClass");
  }

  @Test
  void getPackageName() {
    assertThat(ClassUtils.getPackageName(String.class)).isEqualTo("java.lang");
    assertThat(ClassUtils.getPackageName(getClass())).isEqualTo(getClass().getPackage().getName());
  }

  @Test
  void getQualifiedNameForObjectArrayClass() {
    String className = ClassUtils.getQualifiedName(Object[].class);
    assertThat(className).as("Class name did not match").isEqualTo("java.lang.Object[]");
  }

  @Test
  void getQualifiedNameForMultiDimensionalObjectArrayClass() {
    String className = ClassUtils.getQualifiedName(Object[][].class);
    assertThat(className).as("Class name did not match").isEqualTo("java.lang.Object[][]");
  }

  @Test
  void getQualifiedNameForPrimitiveArrayClass() {
    String className = ClassUtils.getQualifiedName(byte[].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[]");
  }

  @Test
  void getQualifiedNameForMultiDimensionalPrimitiveArrayClass() {
    String className = ClassUtils.getQualifiedName(byte[][].class);
    assertThat(className).as("Class name did not match").isEqualTo("byte[][]");
  }

  public static class AutowirableClass {

    public AutowirableClass(String firstParameter,
            String secondParameter, String thirdParameter,
            String fourthParameter) {
    }

    public AutowirableClass(String notAutowirableParameter) {
    }

    public class InnerAutowirableClass {

      public InnerAutowirableClass(String firstParameter, String secondParameter) {
      }
    }
  }

  private static final Supplier<String> staticLambdaExpression = () -> "static lambda expression";

  private final Supplier<String> instanceLambdaExpression = () -> "instance lambda expressions";

  private static String staticStringFactory() {
    return "static string factory";
  }

  private String instanceStringFactory() {
    return "instance string factory";
  }

  private static class EnigmaSupplier implements Supplier<String> {
    @Override
    public String get() {
      return "enigma";
    }
  }

  private static class Fake$$LambdaSupplier implements Supplier<String> {
    @Override
    public String get() {
      return "fake lambda";
    }
  }

}