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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static cn.taketoday.polaris.util.AnnotationUtils.VALUE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/28 11:17
 */
class AnnotationUtilsTests {

  @Test
  void getValueFromAnnotation() throws Exception {
    Method method = SimpleFoo.class.getMethod("something", String.class);
    Order order = method.getAnnotation(Order.class);

    assertThat(AnnotationUtils.getValue(order, VALUE)).isEqualTo(1);
    assertThat(AnnotationUtils.getValue(order)).isEqualTo(1);
  }

  @Test
  void getValueFromNonPublicAnnotation() throws Exception {
    Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
    assertThat(declaredAnnotations.length).isEqualTo(1);
    Annotation annotation = declaredAnnotations[0];
    assertThat(annotation).isNotNull();
    assertThat(annotation.annotationType().getSimpleName()).isEqualTo("NonPublicAnnotation");
    assertThat(AnnotationUtils.getValue(annotation, VALUE)).isEqualTo(42);
    assertThat(AnnotationUtils.getValue(annotation)).isEqualTo(42);
  }

  @Test
  void getValue() {
    assertThat(AnnotationUtils.getValue(null)).isNull();
    assertThat(AnnotationUtils.getValue(null, null)).isNull();
    assertThat(AnnotationUtils.getValue(null, "")).isNull();
    assertThat(AnnotationUtils.getValue(null, "  ")).isNull();
    assertThat(AnnotationUtils.getValue(new ThrowOrder())).isNull();
    assertThat(AnnotationUtils.getValue(new Order() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }

      @Override
      public int value() {
        return 0;
      }
    })).isNull();

    assertThat(AnnotationUtils.getValue(new OrderImpl())).isEqualTo(1);
    assertThat(AnnotationUtils.getValue(new OrderImpl())).isEqualTo(1);

  }

  @Test
  void invokeAnnotationMethod() throws NoSuchMethodException {
    Class<Order> orderClass = Order.class;
    Method method = orderClass.getDeclaredMethod("value");

    Order annotation = SimpleFoo.class.getAnnotation(Order.class);
    assertThat(AnnotationUtils.invokeAnnotationMethod(method, annotation)).isEqualTo(2);
    assertThat(AnnotationUtils.invokeAnnotationMethod(method, null)).isNull();

    assertThat(AnnotationUtils.invokeAnnotationMethod(method, new OrderImpl())).isEqualTo(1);

  }

  @Order(2)
  public static class SimpleFoo {

    @Order(1)
    public void something(final String arg) {
    }
  }

  @NonPublicAnnotation(42)
  public static class NonPublicAnnotatedClass {

  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
  public @interface Order {

    int value();

  }

  static class OrderImpl implements Order {

    @Override
    public int value() {
      return 1;
    }

    public int value(int v) {
      return v;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return OrderImpl.class;
    }
  }

  static class ThrowOrder implements Order {

    @Override
    public int value() {
      throw new RuntimeException();
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return Order.class;
    }
  }

  static class ProxyOrder implements Order {

    @Override
    public int value() {
      return 0;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return Order.class;
    }

  }

}

@Retention(RetentionPolicy.RUNTIME)
@interface NonPublicAnnotation {

  int value() default -1;
}
