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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.taketoday.polaris.logging.Logger;
import cn.taketoday.polaris.logging.LoggerFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/26 22:40
 */
public abstract class AnnotationUtils {
  private static final Logger logger = LoggerFactory.getLogger(AnnotationUtils.class);

  /**
   * Retrieve the <em>value</em> of the {@code value} attribute of a
   * single-element Annotation, given an annotation instance.
   */
  @Nullable
  public static Object getValue(Annotation annotation) {
    return getValue(annotation, "value");
  }

  /**
   * Retrieve the <em>value</em> of a named attribute, given an annotation instance.
   *
   * @see #getValue(Annotation)
   */
  @Nullable
  public static Object getValue(@Nullable Annotation annotation, @Nullable String attributeName) {
    if (annotation == null || !StringUtils.hasText(attributeName)) {
      return null;
    }
    try {
      for (Method method : annotation.annotationType().getDeclaredMethods()) {
        if (method.getName().equals(attributeName) && method.getParameterCount() == 0) {
          return invokeAnnotationMethod(method, annotation);
        }
      }
    }
    catch (Throwable ex) {
      logger.error("Failed to retrieve value from {}: {}", annotation, ex);
    }
    return null;
  }

  /**
   * Invoke the supplied annotation attribute {@link Method} on the supplied
   * {@link Annotation}.
   * <p>An attempt will first be made to invoke the method via the annotation's
   * {@link InvocationHandler} (if the annotation instance is a JDK dynamic proxy).
   * If that fails, an attempt will be made to invoke the method via reflection.
   *
   * @param method the method to invoke
   * @param annotation the annotation on which to invoke the method
   * @return the value returned from the method invocation
   */
  @Nullable
  static Object invokeAnnotationMethod(Method method, @Nullable Object annotation) {
    if (annotation == null) {
      return null;
    }
    if (Proxy.isProxyClass(annotation.getClass())) {
      try {
        InvocationHandler handler = Proxy.getInvocationHandler(annotation);
        return handler.invoke(annotation, method, null);
      }
      catch (Throwable ex) {
        // Ignore and fall back to reflection below
      }
    }
    return ReflectionUtils.invokeMethod(method, annotation);
  }

}
