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

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.polaris.util.SerializableTypeWrapper.ParameterTypeProvider;
import cn.taketoday.polaris.util.SerializableTypeWrapper.TypeProvider;

/**
 * Encapsulates a Java {@link Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link Class}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #forParameter(Executable, int)
 * @see #forReturnType(Method)
 * @see #forClass(Class)
 * @see #forType(Type)
 * @since 1.0
 */
public class ResolvableType implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * {@code ResolvableType} returned when no value is available. {@code NONE} is used
   * in preference to {@code null} so that multiple method calls can be safely chained.
   */
  public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, null, 0);

  public static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

  private static final ConcurrentHashMap<ResolvableType, ResolvableType> cache =
          new ConcurrentHashMap<>(256);

  /**
   * The underlying Java type being managed.
   */
  private final Type type;

  /**
   * Optional provider for the type.
   */
  @Nullable
  private final TypeProvider typeProvider;

  /**
   * The {@code VariableResolver} to use or {@code null} if no resolver is available.
   */
  @Nullable
  private final VariableResolver variableResolver;

  /**
   * The component type for an array or {@code null} if the type should be deduced.
   */
  @Nullable
  private final ResolvableType componentType;

  @Nullable
  private final Integer hash;

  @Nullable
  private Class<?> resolved;

  @Nullable
  private ResolvableType superType;

  @Nullable
  private ResolvableType[] interfaces;

  @Nullable
  private ResolvableType[] generics;

  @Nullable
  private volatile Boolean unresolvableGenerics;

  /**
   * Private constructor used to create a new {@link ResolvableType} for cache key purposes,
   * with no upfront resolution.
   */
  private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
          @Nullable VariableResolver variableResolver) {
    this.type = type;
    this.typeProvider = typeProvider;
    this.variableResolver = variableResolver;
    this.componentType = null;
    this.hash = calculateHashCode();
    this.resolved = null;
  }

  /**
   * Private constructor used to create a new {@link ResolvableType} for cache value purposes,
   * with upfront resolution and a pre-calculated hash.
   */
  private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
          @Nullable VariableResolver variableResolver, @Nullable Integer hash) {
    this.type = type;
    this.typeProvider = typeProvider;
    this.variableResolver = variableResolver;
    this.componentType = null;
    this.hash = hash;
    this.resolved = resolveClass();
  }

  /**
   * Private constructor used to create a new {@link ResolvableType} for uncached purposes,
   * with upfront resolution but lazily calculated hash.
   */
  private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
          @Nullable VariableResolver variableResolver, @Nullable ResolvableType componentType) {

    this.type = type;
    this.typeProvider = typeProvider;
    this.variableResolver = variableResolver;
    this.componentType = componentType;
    this.hash = null;
    this.resolved = resolveClass();
  }

  /**
   * Private constructor used to create a new {@link ResolvableType} on a {@link Class} basis.
   * Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
   */
  private ResolvableType(@Nullable Class<?> clazz) {
    this.resolved = (clazz != null ? clazz : Object.class);
    this.type = this.resolved;
    this.typeProvider = null;
    this.variableResolver = null;
    this.componentType = null;
    this.hash = null;
  }

  /**
   * Return the underling Java {@link Type} being managed.
   */
  public Type getType() {
    return SerializableTypeWrapper.unwrap(this.type);
  }

  /**
   * Return the underlying Java {@link Class} being managed, if available;
   * otherwise {@code null}.
   */
  @Nullable
  public Class<?> getRawClass() {
    if (this.type == this.resolved) {
      return this.resolved;
    }
    Type rawType = this.type;
    if (rawType instanceof ParameterizedType) {
      rawType = ((ParameterizedType) rawType).getRawType();
    }

    if (rawType instanceof Class) {
      return (Class<?>) rawType;
    }
    return null;
  }

  /**
   * Return the underlying source of the resolvable type.
   */
  public Object getSource() {
    if (typeProvider != null) {
      Object source = typeProvider.getSource();
      if (source != null) {
        return source;
      }
    }
    // fallback
    return type;
  }

  /**
   * Return this type as a resolved {@code Class}, falling back to
   * {@link Object} if no specific class can be resolved.
   *
   * @return the resolved {@link Class} or the {@code Object} fallback
   * @see #getRawClass()
   * @see #resolve(Class)
   */
  public Class<?> toClass() {
    return resolve(Object.class);
  }

  /**
   * Determine whether the given object is an instance of this {@code ResolvableType}.
   *
   * @param obj the object to check
   * @see #isAssignableFrom(Class)
   */
  public boolean isInstance(@Nullable Object obj) {
    return obj != null && isAssignableFrom(obj.getClass());
  }

  /**
   * Determine whether this {@code ResolvableType} is assignable from the
   * specified other type.
   *
   * @param other the type to be checked against (as a {@code Class})
   * @see #isAssignableFrom(ResolvableType)
   */
  public boolean isAssignableFrom(Class<?> other) {
    // shortcut assignability check for top-level Class references
    return this.type instanceof Class<?> clazz
            ? ClassUtils.isAssignable(clazz, other)
            : isAssignableFrom(forClass(other), false, null, false);
  }

  /**
   * Determine whether this {@code ResolvableType} is assignable from the
   * specified other type.
   * <p>Attempts to follow the same rules as the Java compiler, considering
   * whether both the {@link #resolve() resolved} {@code Class} is
   * {@link Class#isAssignableFrom(Class) assignable from} the given type
   * as well as whether all {@link #getGenerics() generics} are assignable.
   *
   * @param other the type to be checked against (as a {@code ResolvableType})
   * @return {@code true} if the specified other type can be assigned to this
   * {@code ResolvableType}; {@code false} otherwise
   */
  public boolean isAssignableFrom(ResolvableType other) {
    return isAssignableFrom(other, false, null, false);
  }

  /**
   * Determine whether this {@code ResolvableType} is assignable from the
   * specified other type, as far as the other type is actually resolvable.
   *
   * @param other the type to be checked against (as a {@code ResolvableType})
   * @return {@code true} if the specified other type can be assigned to this
   * {@code ResolvableType} as far as it is resolvable; {@code false} otherwise
   */
  public boolean isAssignableFromResolvedPart(ResolvableType other) {
    return isAssignableFrom(other, false, null, true);
  }

  private boolean isAssignableFrom(ResolvableType other, boolean strict,
          @Nullable Map<Type, Type> matchedBefore, boolean upUntilUnresolvable) {

    Assert.notNull(other, "ResolvableType is required");

    // If we cannot resolve types, we are not assignable
    if (this == NONE || other == NONE) {
      return false;
    }

    if (matchedBefore != null) {
      if (matchedBefore.get(this.type) == other.type) {
        return true;
      }
    }
    else {
      // shortcut assignability check for top-level Class references
      if (this.type instanceof Class<?> clazz && other.type instanceof Class<?> otherClazz) {
        return (strict ? clazz.isAssignableFrom(otherClazz) : ClassUtils.isAssignable(clazz, otherClazz));
      }
    }

    // Deal with array by delegating to the component type
    if (isArray()) {
      return (other.isArray() && getComponentType().isAssignableFrom(
              other.getComponentType(), true, matchedBefore, upUntilUnresolvable));
    }

    if (upUntilUnresolvable && (other.isUnresolvableTypeVariable() || other.isWildcardWithoutBounds())) {
      return true;
    }

    boolean exactMatch = (strict && matchedBefore != null);  // We're checking nested generic variables now...

    // Deal with wildcard bounds
    WildcardBounds ourBounds = WildcardBounds.get(this);
    WildcardBounds typeBounds = WildcardBounds.get(other);

    // In the form X is assignable to <? extends Number>
    if (typeBounds != null) {
      if (ourBounds != null) {
        return ourBounds.isSameKind(typeBounds)
                && ourBounds.isAssignableFrom(typeBounds.bounds, matchedBefore);
      }
      else if (upUntilUnresolvable) {
        return typeBounds.isAssignableFrom(this, matchedBefore);
      }
      else if (!exactMatch) {
        return typeBounds.isAssignableTo(this, matchedBefore);
      }
      else {
        return false;
      }
    }

    // In the form <? extends Number> is assignable to X...
    if (ourBounds != null) {
      return ourBounds.isAssignableFrom(other, matchedBefore);
    }

    // Main assignability check about to follow
    boolean checkGenerics = true;
    Class<?> ourResolved = null;
    if (this.type instanceof TypeVariable<?> variable) {
      // Try default variable resolution
      if (this.variableResolver != null) {
        ResolvableType resolved = this.variableResolver.resolveVariable(variable);
        if (resolved != null) {
          ourResolved = resolved.resolve();
        }
      }
      if (ourResolved == null) {
        // Try variable resolution against target type
        if (other.variableResolver != null) {
          ResolvableType resolved = other.variableResolver.resolveVariable(variable);
          if (resolved != null) {
            ourResolved = resolved.resolve();
            checkGenerics = false;
          }
        }
      }
      if (ourResolved == null) {
        // Unresolved type variable, potentially nested -> never insist on exact match
        exactMatch = false;
      }
    }
    if (ourResolved == null) {
      ourResolved = toClass();
    }
    Class<?> otherResolved = other.toClass();

    // We need an exact type match for generics
    // List<CharSequence> is not assignable from List<String>
    if (exactMatch ? !ourResolved.equals(otherResolved) :
            (strict ? !ourResolved.isAssignableFrom(otherResolved) :
                    !ClassUtils.isAssignable(ourResolved, otherResolved))) {
      return false;
    }

    if (checkGenerics) {
      // Recursively check each generic
      ResolvableType[] ourGenerics = getGenerics();
      ResolvableType[] typeGenerics = other.as(ourResolved).getGenerics();
      if (ourGenerics.length != typeGenerics.length) {
        return false;
      }
      if (ourGenerics.length > 0) {
        if (matchedBefore == null) {
          matchedBefore = new IdentityHashMap<>(1);
        }
        matchedBefore.put(this.type, other.type);
        for (int i = 0; i < ourGenerics.length; i++) {
          if (!ourGenerics[i].isAssignableFrom(typeGenerics[i], true, matchedBefore, upUntilUnresolvable)) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * Return {@code true} if this type resolves to a Class that represents an array.
   *
   * @see #getComponentType()
   */
  public boolean isArray() {
    if (this == NONE) {
      return false;
    }
    Type type = this.type;
    return (type instanceof Class && ((Class<?>) type).isArray())
            || type instanceof GenericArrayType
            || resolveType(type).isArray();
  }

  /**
   * Return the ResolvableType representing the component type of the array or
   * {@link #NONE} if this type does not represent an array.
   *
   * @see #isArray()
   */
  public ResolvableType getComponentType() {
    if (this == NONE) {
      return NONE;
    }
    if (this.componentType != null) {
      return this.componentType;
    }
    Type type = this.type;
    if (type instanceof Class) {
      Class<?> componentType = ((Class<?>) type).getComponentType();
      return forType(componentType, this.variableResolver);
    }
    if (type instanceof GenericArrayType) {
      return forType(((GenericArrayType) type).getGenericComponentType(), this.variableResolver);
    }
    return resolveType(type).getComponentType();
  }

  /**
   * Convenience method to return this type as a resolvable {@link Collection} type.
   * Returns {@link #NONE} if this type does not implement or extend
   * {@link Collection}.
   *
   * @see #as(Class)
   * @see #asMap()
   */
  public ResolvableType asCollection() {
    return as(Collection.class);
  }

  /**
   * Convenience method to return this type as a resolvable {@link Map} type.
   * Returns {@link #NONE} if this type does not implement or extend
   * {@link Map}.
   *
   * @see #as(Class)
   * @see #asCollection()
   */
  public ResolvableType asMap() {
    return as(Map.class);
  }

  /**
   * Return this type as a {@link ResolvableType} of the specified class. Searches
   * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
   * hierarchies to find a match, returning {@link #NONE} if this type does not
   * implement or extend the specified class.
   *
   * @param type the required type (typically narrowed)
   * @return a {@link ResolvableType} representing this object as the specified
   * type, or {@link #NONE} if not resolvable as that type
   * @see #asCollection()
   * @see #asMap()
   * @see #getSuperType()
   * @see #getInterfaces()
   */
  public ResolvableType as(Class<?> type) {
    if (this == NONE) {
      return NONE;
    }
    if (resolved == null || resolved == type) {
      return this;
    }
    for (ResolvableType interfaceType : getInterfaces()) {
      ResolvableType interfaceAsType = interfaceType.as(type);
      if (interfaceAsType != NONE) {
        return interfaceAsType;
      }
    }
    return getSuperType().as(type);
  }

  /**
   * Return a {@link ResolvableType} representing the direct supertype of this type.
   * If no supertype is available this method returns {@link #NONE}.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @see #getInterfaces()
   */
  public ResolvableType getSuperType() {
    Class<?> resolved = resolve();
    if (resolved == null) {
      return NONE;
    }
    try {
      Type superclass = resolved.getGenericSuperclass();
      if (superclass == null) {
        return NONE;
      }
      ResolvableType superType = this.superType;
      if (superType == null) {
        superType = forType(superclass, this);
        this.superType = superType;
      }
      return superType;
    }
    catch (TypeNotPresentException ex) {
      // Ignore non-present types in generic signature
      return NONE;
    }
  }

  /**
   * Return a {@link ResolvableType} array representing the direct interfaces
   * implemented by this type. If this type does not implement any interfaces an
   * empty array is returned.
   * <p>Note: The resulting {@link ResolvableType} instances may not be {@link Serializable}.
   *
   * @see #getSuperType()
   */
  public ResolvableType[] getInterfaces() {
    Class<?> resolved = resolve();
    if (resolved == null) {
      return EMPTY_TYPES_ARRAY;
    }
    ResolvableType[] interfaces = this.interfaces;
    if (interfaces == null) {
      Type[] genericIfcs = resolved.getGenericInterfaces();
      interfaces = new ResolvableType[genericIfcs.length];
      for (int i = 0; i < genericIfcs.length; i++) {
        interfaces[i] = forType(genericIfcs[i], this);
      }
      this.interfaces = interfaces;
    }
    return interfaces;
  }

  /**
   * Return {@code true} if this type contains generic parameters.
   *
   * @see #getGeneric(int...)
   * @see #getGenerics()
   */
  public boolean hasGenerics() {
    return getGenerics().length > 0;
  }

  /**
   * Return {@code true} if this type contains at least a generic type
   * that is resolved. In other words, this returns {@code false} if
   * the type contains unresolvable generics only, that is, no substitute
   * for any of its declared type variables.
   */
  public boolean hasResolvableGenerics() {
    if (this == NONE) {
      return false;
    }
    ResolvableType[] generics = getGenerics();
    for (ResolvableType generic : generics) {
      if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the underlying type has any unresolvable generics:
   * either through an unresolvable type variable on the type itself
   * or through implementing a generic interface in a raw fashion,
   * i.e. without substituting that interface's type variables.
   * The result will be {@code true} only in those two scenarios.
   */
  public boolean hasUnresolvableGenerics() {
    if (this == NONE) {
      return false;
    }
    return hasUnresolvableGenerics(null);
  }

  private boolean hasUnresolvableGenerics(@Nullable Set<Type> alreadySeen) {
    Boolean unresolvableGenerics = this.unresolvableGenerics;
    if (unresolvableGenerics == null) {
      unresolvableGenerics = determineUnresolvableGenerics(alreadySeen);
      this.unresolvableGenerics = unresolvableGenerics;
    }
    return unresolvableGenerics;
  }

  private boolean determineUnresolvableGenerics(@Nullable Set<Type> alreadySeen) {
    if (alreadySeen != null && alreadySeen.contains(this.type)) {
      // Self-referencing generic -> not unresolvable
      return false;
    }

    ResolvableType[] generics = getGenerics();
    for (ResolvableType generic : generics) {
      if (generic.isUnresolvableTypeVariable() || generic.isWildcardWithoutBounds()
              || generic.hasUnresolvableGenerics(currentTypeSeen(alreadySeen))) {
        return true;
      }
    }
    Class<?> resolved = resolve();
    if (resolved != null) {
      try {
        for (Type genericInterface : resolved.getGenericInterfaces()) {
          if (genericInterface instanceof Class<?> clazz) {
            if (clazz.getTypeParameters().length > 0) {
              return true;
            }
          }
        }
      }
      catch (TypeNotPresentException ex) {
        // Ignore non-present types in generic signature
      }
      Class<?> superclass = resolved.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getSuperType().hasUnresolvableGenerics(currentTypeSeen(alreadySeen));
      }
    }
    return false;
  }

  private Set<Type> currentTypeSeen(@Nullable Set<Type> alreadySeen) {
    if (alreadySeen == null) {
      alreadySeen = new HashSet<>(4);
    }
    alreadySeen.add(this.type);
    return alreadySeen;
  }

  /**
   * Determine whether the underlying type is a type variable that
   * cannot be resolved through the associated variable resolver.
   */
  private boolean isUnresolvableTypeVariable() {
    if (this.type instanceof TypeVariable<?> variable) {
      if (this.variableResolver == null) {
        return true;
      }
      ResolvableType resolved = this.variableResolver.resolveVariable(variable);
      return resolved == null || resolved.isUnresolvableTypeVariable() || resolved.isWildcardWithoutBounds();
    }
    return false;
  }

  /**
   * Determine whether the underlying type represents a wildcard
   * without specific bounds (i.e., equal to {@code ? extends Object}).
   */
  private boolean isWildcardWithoutBounds() {
    if (this.type instanceof WildcardType wt && wt.getLowerBounds().length == 0) {
      Type[] upperBounds = wt.getUpperBounds();
      return upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0]);
    }
    return false;
  }

  /**
   * Return a {@link ResolvableType} for the specified nesting level.
   * See {@link #getNested(int, Map)} for details.
   *
   * @param nestingLevel the nesting level
   * @return the {@link ResolvableType} type, or {@code #NONE}
   */
  public ResolvableType getNested(int nestingLevel) {
    return getNested(nestingLevel, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified nesting level.
   * <p>The nesting level refers to the specific generic parameter that should be returned.
   * A nesting level of 1 indicates this type; 2 indicates the first nested generic;
   * 3 the second; and so on. For example, given {@code List<Set<Integer>>} level 1 refers
   * to the {@code List}, level 2 the {@code Set}, and level 3 the {@code Integer}.
   * <p>The {@code typeIndexesPerLevel} map can be used to reference a specific generic
   * for the given level. For example, an index of 0 would refer to a {@code Map} key;
   * whereas, 1 would refer to the value. If the map does not contain a value for a
   * specific level the last generic will be used (e.g. a {@code Map} value).
   * <p>Nesting levels may also apply to array types; for example given
   * {@code String[]}, a nesting level of 2 refers to {@code String}.
   * <p>If a type does not {@link #hasGenerics() contain} generics the
   * {@link #getSuperType() supertype} hierarchy will be considered.
   *
   * @param nestingLevel the required nesting level, indexed from 1 for the
   * current type, 2 for the first nested generic, 3 for the second and so on
   * @param typeIndexesPerLevel a map containing the generic index for a given
   * nesting level (may be {@code null})
   * @return a {@link ResolvableType} for the nested level, or {@link #NONE}
   */
  public ResolvableType getNested(int nestingLevel, @Nullable Map<Integer, Integer> typeIndexesPerLevel) {
    ResolvableType result = this;
    for (int i = 2; i <= nestingLevel; i++) {
      if (result.isArray()) {
        result = result.getComponentType();
      }
      else {
        // Handle derived types
        while (result != ResolvableType.NONE && !result.hasGenerics()) {
          result = result.getSuperType();
        }
        Integer index = null;
        if (typeIndexesPerLevel != null) {
          index = typeIndexesPerLevel.get(i);
        }

        if (index == null) {
          index = result.getGenerics().length - 1;
        }
        result = result.getGeneric(index);
      }
    }
    return result;
  }

  /**
   * Return a {@link ResolvableType} representing the generic parameter for the
   * given indexes. Indexes are zero based; for example given the type
   * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
   * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
   * for example {@code getGeneric(1, 0)} will access the {@code String} from the
   * nested {@code List}. For convenience, if no indexes are specified the first
   * generic is returned.
   * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
   *
   * @param indexes the indexes that refer to the generic parameter
   * (may be omitted to return the first generic)
   * @return a {@link ResolvableType} for the specified generic, or {@link #NONE}
   * @see #hasGenerics()
   * @see #getGenerics()
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  public ResolvableType getGeneric(@Nullable int... indexes) {
    ResolvableType[] generics = getGenerics();
    if (indexes == null || indexes.length == 0) {
      return (generics.length == 0 ? NONE : generics[0]);
    }
    ResolvableType generic = this;
    for (int index : indexes) {
      generics = generic.getGenerics();
      if (index < 0 || index >= generics.length) {
        return NONE;
      }
      generic = generics[index];
    }
    return generic;
  }

  /**
   * Return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters of
   * this type. If no generics are available an empty array is returned. If you need to
   * access a specific generic consider using the {@link #getGeneric(int...)} method as
   * it allows access to nested generics and protects against
   * {@code IndexOutOfBoundsExceptions}.
   *
   * @return an array of {@link ResolvableType ResolvableTypes} representing the generic parameters
   * (never {@code null})
   * @see #hasGenerics()
   * @see #getGeneric(int...)
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  public ResolvableType[] getGenerics() {
    if (this == NONE) {
      return EMPTY_TYPES_ARRAY;
    }
    ResolvableType[] generics = this.generics;
    if (generics == null) {
      if (type instanceof Class<?> t) {
        Type[] typeParams = t.getTypeParameters();
        generics = new ResolvableType[typeParams.length];
        for (int i = 0; i < generics.length; i++) {
          generics[i] = ResolvableType.forType(typeParams[i], this);
        }
      }
      else if (type instanceof ParameterizedType pt) {
        Type[] actualTypeArguments = pt.getActualTypeArguments();
        generics = new ResolvableType[actualTypeArguments.length];
        for (int i = 0; i < actualTypeArguments.length; i++) {
          generics[i] = forType(actualTypeArguments[i], this.variableResolver);
        }
      }
      else {
        generics = resolveType(type).getGenerics();
      }
      this.generics = generics;
    }
    return generics;
  }

  /**
   * Convenience method that will {@link #getGenerics() get} and
   * {@link #resolve() resolve} generic parameters.
   *
   * @return an array of resolved generic parameters (the resulting array
   * will never be {@code null}, but it may contain {@code null} elements})
   * @see #getGenerics()
   * @see #resolve()
   */
  public Class<?>[] resolveGenerics() {
    ResolvableType[] generics = getGenerics();
    Class<?>[] resolvedGenerics = new Class<?>[generics.length];
    for (int i = 0; i < generics.length; i++) {
      resolvedGenerics[i] = generics[i].resolve();
    }
    return resolvedGenerics;
  }

  /**
   * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
   * resolve} generic parameters, using the specified {@code fallback} if any type
   * cannot be resolved.
   *
   * @param fallback the fallback class to use if resolution fails
   * @return an array of resolved generic parameters
   * @see #getGenerics()
   * @see #resolve()
   */
  public Class<?>[] resolveGenerics(Class<?> fallback) {
    ResolvableType[] generics = getGenerics();
    Class<?>[] resolvedGenerics = new Class<?>[generics.length];
    for (int i = 0; i < generics.length; i++) {
      resolvedGenerics[i] = generics[i].resolve(fallback);
    }
    return resolvedGenerics;
  }

  /**
   * Convenience method that will {@link #getGeneric(int...) get} and
   * {@link #resolve() resolve} a specific generic parameters.
   *
   * @param indexes the indexes that refer to the generic parameter
   * (maybe omitted to return the first generic)
   * @return a resolved {@link Class} or {@code null}
   * @see #getGeneric(int...)
   * @see #resolve()
   */
  @Nullable
  public Class<?> resolveGeneric(int... indexes) {
    return getGeneric(indexes).resolve();
  }

  /**
   * Resolve this type to a {@link Class}, returning {@code null}
   * if the type cannot be resolved. This method will consider bounds of
   * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
   * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
   * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
   * returns {@code false}, the given type effectively wraps a plain {@code Class},
   * allowing for plain {@code Class} processing if desirable.
   *
   * @return the resolved {@link Class}, or {@code null} if not resolvable
   * @see #resolve(Class)
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  @Nullable
  public Class<?> resolve() {
    return this.resolved;
  }

  /**
   * Convenience method that indicates {@link #resolved} status
   *
   * @see #resolve()
   */
  public boolean isResolved() {
    return resolved != null;
  }

  /**
   *
   */
  public boolean is(Class<?> clazz) {
    return resolved == clazz;
  }

  /**
   *
   */
  public boolean isCollection() {
    return resolved != null && Collection.class.isAssignableFrom(resolved);
  }

  /**
   * Is this type a {@link Map} type?
   */
  public boolean isMap() {
    return resolved != null && Map.class.isAssignableFrom(resolved);
  }

  /**
   * Resolve this type to a {@link Class}, returning the specified
   * {@code fallback} if the type cannot be resolved. This method will consider bounds
   * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
   * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
   *
   * @param fallback the fallback class to use if resolution fails
   * @return the resolved {@link Class} or the {@code fallback}
   * @see #resolve()
   * @see #resolveGeneric(int...)
   * @see #resolveGenerics()
   */
  public Class<?> resolve(Class<?> fallback) {
    return this.resolved != null ? this.resolved : fallback;
  }

  @Nullable
  private Class<?> resolveClass() {
    Type type = this.type;
    if (type == EmptyType.INSTANCE) {
      return null;
    }
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof GenericArrayType) {
      Class<?> resolvedComponent = getComponentType().resolve();
      return resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null;
    }
    return resolveType(type).resolve();
  }

  /**
   * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
   * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
   * as it cannot be serialized.
   */
  ResolvableType resolveType() {
    return resolveType(type);
  }

  /**
   * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
   * <p>Note: The returned {@link ResolvableType} should only be used as an intermediary
   * as it cannot be serialized.
   */
  private ResolvableType resolveType(Type type) {
    if (type instanceof ParameterizedType) {
      return forType(((ParameterizedType) type).getRawType(), variableResolver);
    }
    if (type instanceof WildcardType wildcardType) {
      Type resolved = resolveBounds(wildcardType.getUpperBounds());
      if (resolved == null) {
        resolved = resolveBounds(wildcardType.getLowerBounds());
      }
      return forType(resolved, variableResolver);
    }
    if (type instanceof TypeVariable<?> variable) {
      VariableResolver variableResolver = this.variableResolver;
      // Try default variable resolution
      if (variableResolver != null) {
        ResolvableType resolved = variableResolver.resolveVariable(variable);
        if (resolved != null) {
          return resolved;
        }
      }
      // Fallback to bounds
      return forType(resolveBounds(variable.getBounds()), variableResolver);
    }
    return NONE;
  }

  @Nullable
  private Type resolveBounds(Type[] bounds) {
    if (bounds.length == 0 || bounds[0] == Object.class) {
      return null;
    }
    return bounds[0];
  }

  @Nullable
  private ResolvableType resolveVariable(TypeVariable<?> variable) {
    Type type = this.type;
    if (type instanceof TypeVariable) {
      return resolveType(type).resolveVariable(variable);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      Class<?> resolved = resolve();
      if (resolved == null) {
        return null;
      }
      TypeVariable<?>[] variables = resolved.getTypeParameters();
      for (int i = 0; i < variables.length; i++) {
        if (Objects.equals(variables[i].getName(), variable.getName())) {
          Type actualType = parameterizedType.getActualTypeArguments()[i];
          return forType(actualType, this.variableResolver);
        }
      }
      Type ownerType = parameterizedType.getOwnerType();
      if (ownerType != null) {
        return forType(ownerType, this.variableResolver).resolveVariable(variable);
      }
    }
    if (type instanceof WildcardType) {
      ResolvableType resolved = resolveType(type).resolveVariable(variable);
      if (resolved != null) {
        return resolved;
      }
    }
    if (this.variableResolver != null) {
      return this.variableResolver.resolveVariable(variable);
    }
    return null;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || other.getClass() != getClass()) {
      return false;
    }
    ResolvableType otherType = (ResolvableType) other;
    if (!equalsType(otherType)) {
      return false;
    }
    if (typeProvider != otherType.typeProvider
            && (
            typeProvider == null
                    || otherType.typeProvider == null
                    || !Objects.equals(this.typeProvider.getType(), otherType.typeProvider.getType())
    )) {
      return false;
    }
    return variableResolver == otherType.variableResolver
            || (variableResolver != null
            && otherType.variableResolver != null
            && ObjectUtils.nullSafeEquals(variableResolver.getSource(), otherType.variableResolver.getSource())
    );
  }

  /**
   * Check for type-level equality with another {@code ResolvableType}.
   * <p>In contrast to {@link #equals(Object)} or {@link #isAssignableFrom(ResolvableType)},
   * this works between different sources as well, e.g. method parameters and return types.
   *
   * @param otherType the {@code ResolvableType} to match against
   * @return whether the declared type and type variables match
   */
  public boolean equalsType(ResolvableType otherType) {
    return Objects.equals(this.type, otherType.type)
            && Objects.equals(this.componentType, otherType.componentType);
  }

  @Override
  public int hashCode() {
    return this.hash != null ? this.hash : calculateHashCode();
  }

  private int calculateHashCode() {
    int hashCode = ObjectUtils.nullSafeHashCode(this.type);
    if (this.componentType != null) {
      hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.componentType);
    }
    if (this.typeProvider != null) {
      hashCode = 31 * hashCode + Objects.hashCode(this.typeProvider.getType());
    }
    if (this.variableResolver != null) {
      hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
    }

    return hashCode;
  }

  /**
   * Adapts this {@link ResolvableType} to a {@link VariableResolver}.
   */
  @Nullable
  VariableResolver asVariableResolver() {
    if (this == NONE) {
      return null;
    }
    return new DefaultVariableResolver(this);
  }

  /**
   * Custom serialization support for {@link #NONE}.
   */
  @Serial
  private Object readResolve() {
    return this.type == EmptyType.INSTANCE ? NONE : this;
  }

  /**
   * Return a String representation of this type in its fully resolved form
   * (including any generic parameters).
   */
  @Override
  public String toString() {
    if (isArray()) {
      return getComponentType() + "[]";
    }
    if (this.resolved == null) {
      return "?";
    }
    if (this.type instanceof TypeVariable<?> variable) {
      if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
        // Don't bother with variable boundaries for toString()...
        // Can cause infinite recursions in case of self-references
        return "?";
      }
    }
    if (hasGenerics()) {
      return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
    }
    return this.resolved.getName();
  }

  // Factory methods

  /**
   * Return a {@link ResolvableType} for the specified {@link Executable} parameter.
   *
   * @param executable the source Executable (must not be {@code null})
   * @param parameterIndex the parameter index
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int, Class)
   * @see #forParameter(Parameter)
   */
  public static ResolvableType forParameter(Executable executable, int parameterIndex) {
    return forParameter(executable, parameterIndex, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
   * given implementation. Use this variant when the class that declares the method
   * includes generic parameter variables that are satisfied by the implementation class.
   *
   * @param executable the source method or constructor (must not be {@code null})
   * @param parameterIndex the parameter index
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Parameter)
   */
  public static ResolvableType forParameter(Executable executable, int parameterIndex, @Nullable Class<?> implementationClass) {
    Parameter parameter = ReflectionUtils.getParameter(executable, parameterIndex);
    Class<?> declaringClass = executable.getDeclaringClass();
    ResolvableType owner = implementationClass == null
            ? forType(declaringClass)
            : forType(implementationClass).as(declaringClass);
    return forType(null, new ParameterTypeProvider(parameter, parameterIndex), owner.asVariableResolver());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Parameter}.
   *
   * @param parameter the source method parameter (must not be {@code null})
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   */
  public static ResolvableType forParameter(Parameter parameter) {
    return forParameter(parameter, (Type) null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Parameter} with a
   * given implementation type. Use this variant when the class that declares the method
   * includes generic parameter variables that are satisfied by the implementation type.
   *
   * @param parameter the source method parameter (must not be {@code null})
   * @param implementationType the implementation type
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Parameter)
   */
  public static ResolvableType forParameter(Parameter parameter, @Nullable ResolvableType implementationType) {
    Assert.notNull(parameter, "Parameter is required");
    Executable executable = parameter.getDeclaringExecutable();
    Class<?> declaringClass = executable.getDeclaringClass();

    ResolvableType owner;
    if (implementationType != null) {
      owner = implementationType.as(declaringClass);
    }
    else {
      owner = forType(declaringClass);
    }

    return forType(null, new ParameterTypeProvider(parameter), owner.asVariableResolver());
  }

  public static ResolvableType forParameter(Parameter parameter, Class<?> implementationType) {
    return forParameter(parameter, forType(implementationType));
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Parameter},
   * overriding the target type to resolve with a specific given type.
   *
   * @param parameter the source method parameter (must not be {@code null})
   * @param targetType the type to resolve (a part of the method parameter's type)
   * @return a {@link ResolvableType} for the specified method parameter
   * @see #forParameter(Executable, int)
   */
  public static ResolvableType forParameter(Parameter parameter, @Nullable Type targetType) {
    Assert.notNull(parameter, "Parameter is required");
    Executable executable = parameter.getDeclaringExecutable();
    Class<?> declaringClass = executable.getDeclaringClass();
    ResolvableType owner = forType(declaringClass);
    return forType(targetType, new ParameterTypeProvider(parameter), owner.asVariableResolver());
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Class},
   * using the full generic type information for assignability checks.
   * For example: {@code ResolvableType.fromClass(MyArrayList.class)}.
   *
   * @param clazz the class to introspect ({@code null} is semantically
   * equivalent to {@code Object.class} for typical use cases here)
   * @return a {@link ResolvableType} for the specified class
   * @see #forClass(Class, Class)
   */
  public static ResolvableType forClass(@Nullable Class<?> clazz) {
    return new ResolvableType(clazz);
  }

  /**
   * Return a {@link ResolvableType} for the specified base type
   * (interface or base class) with a given implementation class.
   * For example: {@code ResolvableType.fromClass(List.class, MyArrayList.class)}.
   *
   * @param baseType the base type (must not be {@code null})
   * @param implementationClass the implementation class
   * @return a {@link ResolvableType} for the specified base type backed by the
   * given implementation class
   * @see #forClass(Class)
   */
  public static ResolvableType forClass(Class<?> baseType, Class<?> implementationClass) {
    Assert.notNull(baseType, "Base type is required");
    ResolvableType asType = forType(implementationClass).as(baseType);
    return (asType == NONE ? forType(baseType) : asType);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type}.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @param type the source type (potentially {@code null})
   * @return a {@link ResolvableType} for the specified {@link Type}
   * @see #forType(Type, ResolvableType)
   */
  public static ResolvableType forType(@Nullable Type type) {
    return forType(type, null, null);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type} backed by the given
   * owner type.
   * <p>Note: The resulting {@link ResolvableType} instance may not be {@link Serializable}.
   *
   * @param type the source type or {@code null}
   * @param owner the owner type used to resolve variables
   * @return a {@link ResolvableType} for the specified {@link Type} and owner
   * @see #forType(Type)
   */
  public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
    VariableResolver variableResolver = null;
    if (owner != null) {
      variableResolver = owner.asVariableResolver();
    }
    return forType(type, variableResolver);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
   * {@link VariableResolver}.
   *
   * @param type the source type or {@code null}
   * @param variableResolver the variable resolver or {@code null}
   * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
   */
  public static ResolvableType forType(@Nullable Type type, @Nullable VariableResolver variableResolver) {
    return forType(type, null, variableResolver);
  }

  /**
   * Return a {@link ResolvableType} for the specified {@link Type} backed by a given
   * {@link VariableResolver}.
   *
   * @param type the source type or {@code null}
   * @param typeProvider the type provider or {@code null}
   * @param variableResolver the variable resolver or {@code null}
   * @return a {@link ResolvableType} for the specified {@link Type} and {@link VariableResolver}
   */
  public static ResolvableType forType(@Nullable Type type,
          @Nullable TypeProvider typeProvider, @Nullable VariableResolver variableResolver) {

    if (type == null && typeProvider != null) {
      type = SerializableTypeWrapper.forTypeProvider(typeProvider);
    }
    if (type == null) {
      return NONE;
    }

    // For simple Class references, build the wrapper right away -
    // no expensive resolution necessary, so not worth caching...
    if (type instanceof Class) {
      return new ResolvableType(type, typeProvider, variableResolver, (ResolvableType) null);
    }

    // Check the cache - we may have a ResolvableType which has been resolved before...
    ResolvableType resultType = new ResolvableType(type, typeProvider, variableResolver);
    ResolvableType cachedType = cache.get(resultType);
    if (cachedType == null) {
      cachedType = new ResolvableType(type, typeProvider, variableResolver, resultType.hash);
      cache.put(cachedType, cachedType);
    }
    resultType.resolved = cachedType.resolved;
    return resultType;
  }

  /**
   * Clear the internal {@code ResolvableType}/{@code SerializableTypeWrapper} cache.
   */
  public static void clearCache() {
    cache.clear();
    SerializableTypeWrapper.cache.clear();
  }

  /**
   * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
   */
  public interface VariableResolver extends Serializable {

    /**
     * Return the source of the resolver (used for hashCode and equals).
     */
    Object getSource();

    /**
     * Resolve the specified variable.
     *
     * @param variable the variable to resolve
     * @return the resolved variable, or {@code null} if not found
     */
    @Nullable
    ResolvableType resolveVariable(TypeVariable<?> variable);
  }

  static class DefaultVariableResolver implements VariableResolver {
    @Serial
    private static final long serialVersionUID = 1L;

    final ResolvableType source;

    DefaultVariableResolver(ResolvableType resolvableType) {
      this.source = resolvableType;
    }

    @Nullable
    @Override
    public ResolvableType resolveVariable(TypeVariable<?> variable) {
      return this.source.resolveVariable(variable);
    }

    @Override
    public Object getSource() {
      return this.source;
    }
  }

  record TypeVariablesVariableResolver(TypeVariable<?>[] variables, ResolvableType[] generics)
          implements VariableResolver {

    @Override
    public ResolvableType resolveVariable(TypeVariable<?> variable) {
      TypeVariable<?> variableToCompare = SerializableTypeWrapper.unwrap(variable);
      TypeVariable<?>[] variables = this.variables;
      for (int i = 0; i < variables.length; i++) {
        TypeVariable<?> resolvedVariable = SerializableTypeWrapper.unwrap(variables[i]);
        if (Objects.equals(resolvedVariable, variableToCompare)) {
          return this.generics[i];
        }
      }
      return null;
    }

    @Override
    public Object getSource() {
      return this.generics;
    }
  }

  record SyntheticParameterizedType(Type rawType, Type[] typeArguments)
          implements ParameterizedType, Serializable {

    @Override
    public String getTypeName() {
      String typeName = this.rawType.getTypeName();
      if (this.typeArguments.length > 0) {
        StringJoiner stringJoiner = new StringJoiner(", ", "<", ">");
        for (Type argument : this.typeArguments) {
          stringJoiner.add(argument.getTypeName());
        }
        return typeName + stringJoiner;
      }
      return typeName;
    }

    @Nullable
    @Override
    public Type getOwnerType() {
      return null;
    }

    @Override
    public Type getRawType() {
      return this.rawType;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return this.typeArguments;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ParameterizedType otherType)) {
        return false;
      }
      return otherType.getOwnerType() == null
              && this.rawType.equals(otherType.getRawType())
              && Arrays.equals(this.typeArguments, otherType.getActualTypeArguments());
    }

    @Override
    public int hashCode() {
      return (this.rawType.hashCode() * 31 + Arrays.hashCode(this.typeArguments));
    }

    @Override
    public String toString() {
      return getTypeName();
    }
  }

  /**
   * Internal helper to handle bounds from {@link WildcardType WildcardTypes}.
   */
  private static class WildcardBounds {

    public final Kind kind;

    public final ResolvableType[] bounds;

    /**
     * Internal constructor to create a new {@link WildcardBounds} instance.
     *
     * @param kind the kind of bounds
     * @param bounds the bounds
     * @see #get(ResolvableType)
     */
    public WildcardBounds(Kind kind, ResolvableType[] bounds) {
      this.kind = kind;
      this.bounds = bounds;
    }

    /**
     * Return {@code true} if these bounds are the same kind as the specified bounds.
     */
    public boolean isSameKind(WildcardBounds bounds) {
      return this.kind == bounds.kind;
    }

    /**
     * Return {@code true} if these bounds are assignable from all the specified types.
     *
     * @param types the types to test against
     * @return {@code true} if these bounds are assignable from all types
     */
    public boolean isAssignableFrom(ResolvableType[] types, @Nullable Map<Type, Type> matchedBefore) {
      for (ResolvableType type : types) {
        if (!isAssignableFrom(type, matchedBefore)) {
          return false;
        }
      }
      return true;
    }

    /**
     * Return {@code true} if these bounds are assignable from the specified type.
     *
     * @param type the type to test against
     * @return {@code true} if these bounds are assignable from the type
     */
    public boolean isAssignableFrom(ResolvableType type, @Nullable Map<Type, Type> matchedBefore) {
      for (ResolvableType bound : this.bounds) {
        if (this.kind == Kind.UPPER
                ? !bound.isAssignableFrom(type, false, matchedBefore, false)
                : !type.isAssignableFrom(bound, false, matchedBefore, false)) {
          return false;
        }
      }
      return true;
    }

    /**
     * Return {@code true} if these bounds are assignable to the specified type.
     *
     * @param type the type to test against
     * @return {@code true} if these bounds are assignable to the type
     */
    public boolean isAssignableTo(ResolvableType type, @Nullable Map<Type, Type> matchedBefore) {
      if (this.kind == Kind.UPPER) {
        for (ResolvableType bound : this.bounds) {
          if (type.isAssignableFrom(bound, false, matchedBefore, false)) {
            return true;
          }
        }
        return false;
      }
      else {
        return (type.resolve() == Object.class);
      }
    }

    /**
     * Get a {@link WildcardBounds} instance for the specified type, returning
     * {@code null} if the specified type cannot be resolved to a {@link WildcardType}
     * or an equivalent unresolvable type variable.
     *
     * @param type the source type
     * @return a {@link WildcardBounds} instance or {@code null}
     */
    @Nullable
    public static WildcardBounds get(ResolvableType type) {
      ResolvableType candidate = type;
      while (!(candidate.getType() instanceof WildcardType || candidate.isUnresolvableTypeVariable())) {
        if (candidate == NONE) {
          return null;
        }
        candidate = candidate.resolveType();
      }
      Kind boundsType;
      Type[] bounds;
      if (candidate.getType() instanceof WildcardType wildcardType) {
        boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER : Kind.UPPER);
        bounds = (boundsType == Kind.UPPER ? wildcardType.getUpperBounds() : wildcardType.getLowerBounds());
      }
      else {
        boundsType = Kind.UPPER;
        bounds = ((TypeVariable<?>) candidate.getType()).getBounds();
      }
      ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
      for (int i = 0; i < bounds.length; i++) {
        resolvableBounds[i] = ResolvableType.forType(bounds[i], type.variableResolver);
      }
      return new WildcardBounds(boundsType, resolvableBounds);
    }

    /**
     * The various kinds of bounds.
     */
    enum Kind {UPPER, LOWER}
  }

  /**
   * Internal {@link Type} used to represent an empty value.
   */
  static class EmptyType implements Type, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    static final Type INSTANCE = new EmptyType();

    @Serial
    Object readResolve() {
      return INSTANCE;
    }
  }

}
