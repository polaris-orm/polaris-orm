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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal utility class that can be used to obtain wrapped {@link Serializable}
 * variants of {@link java.lang.reflect.Type java.lang.reflect.Types}.
 *
 * <p>The returned type will either be a {@link Class} or a serializable proxy of
 * {@link GenericArrayType}, {@link ParameterizedType}, {@link TypeVariable} or
 * {@link WildcardType}. With the exception of {@link Class} (which is final) calls
 * to methods that return further {@link Type Types} (for example
 * {@link GenericArrayType#getGenericComponentType()}) will be automatically wrapped.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
final class SerializableTypeWrapper {

  private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
          GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class
  };

  static final ConcurrentHashMap<Type, Type> cache = new ConcurrentHashMap<>(256);

  private SerializableTypeWrapper() { }

  /**
   * Return a {@link Serializable} variant of {@link Field#getGenericType()}.
   */
  @Nullable
  public static Type forField(Field field) {
    return forTypeProvider(new FieldTypeProvider(field));
  }

  /**
   * Unwrap the given type, effectively returning the original non-serializable type.
   *
   * @param type the type to unwrap
   * @return the original non-serializable type
   */
  @SuppressWarnings("unchecked")
  public static <T extends Type> T unwrap(T type) {
    Type unwrapped = null;
    if (type instanceof SerializableTypeProxy) {
      unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
    }
    return (unwrapped != null ? (T) unwrapped : type);
  }

  /**
   * Return a {@link Serializable} {@link Type} backed by a {@link TypeProvider} .
   * <p>If type artifacts are generally not serializable in the current runtime
   * environment, this delegate will simply return the original {@code Type} as-is.
   */
  @Nullable
  static Type forTypeProvider(TypeProvider provider) {
    Type providedType = provider.getType();
    if (providedType == null || providedType instanceof Serializable) {
      // No serializable type wrapping necessary (e.g. for java.lang.Class)
      return providedType;
    }

    if (!Serializable.class.isAssignableFrom(Class.class)) {
      // Let's skip any wrapping attempts if types are generally not serializable in
      // the current runtime environment (even java.lang.Class itself, e.g. on GraalVM native images)
      return providedType;
    }

    // Obtain a serializable type proxy for the given provider...
    Type cached = cache.get(providedType);
    if (cached != null) {
      return cached;
    }
    for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
      if (type.isInstance(providedType)) {
        ClassLoader classLoader = provider.getClass().getClassLoader();
        Class<?>[] interfaces = new Class<?>[] { type, SerializableTypeProxy.class, Serializable.class };
        InvocationHandler handler = new TypeProxyInvocationHandler(provider);
        cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
        cache.put(providedType, cached);
        return cached;
      }
    }
    throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
  }

  /**
   * Additional interface implemented by the type proxy.
   */
  interface SerializableTypeProxy {

    /**
     * Return the underlying type provider.
     */
    TypeProvider getTypeProvider();
  }

  /**
   * A {@link Serializable} interface providing access to a {@link Type}.
   */
  interface TypeProvider extends Serializable {

    /**
     * Return the (possibly non {@link Serializable}) {@link Type}.
     */
    @Nullable
    Type getType();

    /**
     * Return the source of the type, or {@code null} if not known.
     * <p>The default implementations returns {@code null}.
     */
    @Nullable
    default Object getSource() {
      return null;
    }
  }

  /**
   * {@link Serializable} {@link InvocationHandler} used by the proxied {@link Type}.
   * Provides serialization support and enhances any methods that return {@code Type}
   * or {@code Type[]}.
   */
  record TypeProxyInvocationHandler(TypeProvider provider) implements InvocationHandler, Serializable {

    @Nullable
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      switch (method.getName()) {
        case "equals" -> {
          Object other = args[0];
          // Unwrap proxies for speed
          if (other instanceof Type) {
            other = unwrap((Type) other);
          }
          return Objects.equals(provider.getType(), other);
        }
        case "hashCode" -> {
          return Objects.hashCode(provider.getType());
        }
        case "getTypeProvider" -> {
          return provider;
        }
      }

      if (ObjectUtils.isEmpty(args)) {
        Class<?> returnType = method.getReturnType();
        if (Type.class == returnType) {
          return forTypeProvider(new MethodInvokeTypeProvider(provider, method, -1));
        }
        else if (Type[].class == returnType) {
          Object returnValue = ReflectionUtils.invokeMethod(method, this.provider.getType());
          if (returnValue == null) {
            return null;
          }

          Type[] result = new Type[((Type[]) returnValue).length];
          for (int i = 0; i < result.length; i++) {
            result[i] = forTypeProvider(new MethodInvokeTypeProvider(provider, method, i));
          }
          return result;
        }
      }
      return ReflectionUtils.invokeMethod(method, provider.getType(), args);
    }
  }

  /**
   * {@link TypeProvider} for {@link Type Types} obtained from a {@link Field}.
   */
  static class FieldTypeProvider implements TypeProvider {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final Class<?> declaringClass;
    private transient Field field;

    public FieldTypeProvider(Field field) {
      this.fieldName = field.getName();
      this.declaringClass = field.getDeclaringClass();
      this.field = field;
    }

    @Override
    public Type getType() {
      return this.field.getGenericType();
    }

    @Override
    public Object getSource() {
      return this.field;
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      try {
        this.field = this.declaringClass.getDeclaredField(this.fieldName);
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Could not find original class structure", ex);
      }
    }
  }

  /**
   * {@link TypeProvider} for {@link Type Types} obtained from a {@link Parameter}.
   */
  static class ParameterTypeProvider implements TypeProvider {
    @Serial
    private static final long serialVersionUID = 1L;

    @Nullable
    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final Class<?> declaringClass;
    private final int parameterIndex;
    private transient Parameter methodParameter;

    public ParameterTypeProvider(Parameter parameter) {
      this(parameter, ReflectionUtils.getParameterIndex(parameter));
    }

    public ParameterTypeProvider(Parameter parameter, int parameterIndex) {
      Executable executable = parameter.getDeclaringExecutable();
      this.methodParameter = parameter;
      this.parameterIndex = parameterIndex;
      this.methodName = executable instanceof Method method ? method.getName() : null;
      this.parameterTypes = executable.getParameterTypes();
      this.declaringClass = executable.getDeclaringClass();
    }

    @Override
    public Type getType() {
      return this.methodParameter.getParameterizedType();
    }

    @Override
    public Object getSource() {
      return this.methodParameter;
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      try {
        if (this.methodName != null) {
          Method declaredMethod = this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes);
          this.methodParameter = declaredMethod.getParameters()[parameterIndex];
        }
        else {
          Constructor<?> constructor = this.declaringClass.getDeclaredConstructor(this.parameterTypes);
          this.methodParameter = constructor.getParameters()[parameterIndex];
        }
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Could not find original class structure", ex);
      }
    }
  }

  /**
   * {@link TypeProvider} for {@link Type Types} obtained by invoking a no-arg method.
   */
  static class MethodInvokeTypeProvider implements TypeProvider {
    @Serial
    private static final long serialVersionUID = 1L;

    private final TypeProvider provider;
    private final String methodName;
    private final Class<?> declaringClass;
    private final int index;
    private transient Method method;
    @Nullable
    private transient volatile Object result;

    public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
      this.provider = provider;
      this.methodName = method.getName();
      this.declaringClass = method.getDeclaringClass();
      this.index = index;
      this.method = method;
    }

    @Override
    public Type getType() {
      Object result = this.result;
      if (result == null) {
        // Lazy invocation of the target method on the provided type
        result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
        // Cache the result for further calls to getType()
        this.result = result;
      }
      return result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result;
    }

    @Override
    public Object getSource() {
      return null;
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
      if (method == null) {
        throw new IllegalStateException("Cannot find method on deserialization: " + this.methodName);
      }
      if (method.getReturnType() != Type.class && method.getReturnType() != Type[].class) {
        throw new IllegalStateException(
                "Invalid return type on deserialized method - needs to be Type or Type[]: " + method);
      }
      this.method = method;
    }
  }

}
