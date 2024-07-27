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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/26 21:55
 */
public abstract class CollectionUtils {

  /**
   * Return {@code true} if the supplied Collection is {@code null} or empty.
   * Otherwise, return {@code false}.
   *
   * @param collection the Collection to check
   * @return whether the given Collection is empty
   */
  public static boolean isEmpty(@Nullable Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * @since 1.0
   */
  public static boolean isNotEmpty(@Nullable Collection<?> collection) {
    return collection != null && !collection.isEmpty();
  }

  /**
   * Return {@code true} if the supplied Map is {@code null} or empty. Otherwise,
   * return {@code false}.
   *
   * @param map the Map to check
   * @return whether the given Map is empty
   */
  public static boolean isEmpty(@Nullable Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * @since 1.0
   */
  public static boolean isNotEmpty(@Nullable Map<?, ?> map) {
    return map != null && !map.isEmpty();
  }

  /**
   * Retrieve the first element of the given Iterable, using {@link SortedSet#first()}
   * or otherwise using the iterator.
   *
   * @param iterable the iterable to check (may be {@code null} or empty)
   * @return the first element, or {@code null} if none
   * @see SortedSet
   * @see java.util.Queue
   * @see LinkedHashMap#keySet()
   * @see java.util.LinkedHashSet
   * @since 1.0
   */
  @Nullable
  public static <T> T firstElement(@Nullable Iterable<T> iterable) {
    if (iterable == null
            || iterable instanceof Collection && ((Collection<T>) iterable).isEmpty()) {
      return null;
    }
    if (iterable instanceof SortedSet) {
      return ((SortedSet<T>) iterable).first();
    }

    Iterator<T> it = iterable.iterator();
    T first = null;
    if (it.hasNext()) {
      first = it.next();
    }
    return first;
  }

  /**
   * Retrieve the first element of the given List, accessing the zero index.
   *
   * @param list the List to check (may be {@code null} or empty)
   * @return the first element, or {@code null} if none
   * @since 1.0
   */
  @Nullable
  public static <T> T firstElement(@Nullable List<T> list) {
    return getElement(list, 0);
  }

  /**
   * Retrieve the first element of the given Array, accessing the zero index.
   *
   * @param array the array to check (may be {@code null} or empty)
   * @return the first element, or {@code null} if none
   * @since 1.0
   */
  @Nullable
  public static <T> T firstElement(@Nullable final T[] array) {
    return getElement(array, 0);
  }

  /**
   * Retrieve the last element of the given List, accessing the highest index.
   *
   * @param list the List to check (may be {@code null} or empty)
   * @return the last element, or {@code null} if none
   * @since 1.0
   */
  @Nullable
  public static <T> T lastElement(@Nullable List<T> list) {
    if (isEmpty(list)) {
      return null;
    }
    return list.get(list.size() - 1);
  }

  /**
   * Retrieve the last element of the given array, accessing the highest index.
   *
   * @param array the array to check (may be {@code null} or empty)
   * @return the last element, or {@code null} if none
   * @since 1.0
   */
  @Nullable
  public static <T> T lastElement(@Nullable final T[] array) {
    if (array == null || array.length == 0) {
      return null;
    }
    return array[array.length - 1];
  }

  /**
   * Returns the element at the specified position in this list.
   * <p>list can be {@code null}, then returns {@code null}
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @see List#get(int)
   * @since 1.0
   */
  @Nullable
  public static <T> T getElement(@Nullable final List<T> list, final int index) {
    if (list != null && index >= 0 && index < list.size()) {
      return list.get(index);
    }
    return null;
  }

  /**
   * Returns the element at the specified position in this list.
   * <p>list can be {@code null}, then returns {@code null}
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @see List#get(int)
   * @since 1.0
   */
  @Nullable
  public static <T> T getElement(@Nullable final T[] array, final int index) {
    if (array != null && index >= 0 && index < array.length) {
      return array[index];
    }
    return null;
  }

  /**
   * Adds all of the specified elements to the specified collection.
   * Elements to be added may be specified individually or as an array.
   * The behavior of this convenience method is identical to that of
   * <tt>c.addAll(Arrays.asList(elements))</tt>, but this method is likely
   * to run significantly faster under most implementations.
   *
   * <p>When elements are specified individually, this method provides a
   * convenient way to add a few elements to an existing collection:
   * <pre>
   *     CollectionUtils.addAll(flavors, "Peaches 'n Plutonium", "Rocky Racoon");
   *     CollectionUtils.addAll(flavors, null); // add nothing element can be null
   * </pre>
   *
   * @param c the collection into which <tt>elements</tt> are to be inserted
   * @param elements the elements to insert into <tt>c</tt>
   * @throws UnsupportedOperationException if <tt>c</tt> does not support
   * the <tt>add</tt> operation
   * @throws NullPointerException if <tt>elements</tt> contains one or more
   * null values and <tt>c</tt> does not permit null elements, or
   * if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException if some property of a value in
   * <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#add(Object)
   * @since 1.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(Collection c, @Nullable Object[] elements) {
    if (elements != null) {
      for (Object element : elements) {
        c.add(element);
      }
    }
  }

  /**
   * Adds all of the specified elements to the specified collection.
   * Elements to be added may be specified individually or as Enumeration.
   *
   * @param c the collection into which <tt>elements</tt> are to be inserted
   * @param values the elements to insert into <tt>c</tt>
   * @throws UnsupportedOperationException if <tt>c</tt> does not support
   * the <tt>add</tt> operation
   * @throws NullPointerException if <tt>elements</tt> contains one or more
   * null values and <tt>c</tt> does not permit null elements, or
   * if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException if some property of a value in
   * <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#add(Object)
   * @since 1.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(Collection c, @Nullable Enumeration values) {
    if (values != null) {
      while (values.hasMoreElements()) {
        c.add(values.nextElement());
      }
    }
  }

  /**
   * Adds all of the specified elements to the specified collection.
   * Elements to be added may be specified individually or as an array.
   * The behavior of this convenience method is identical to that of
   * <tt>c.addAll(Arrays.asList(elements))</tt>, but this method is likely
   * to run significantly faster under most implementations.
   *
   * <p>When elements are specified individually, this method provides a
   * convenient way to add a few elements to an existing collection:
   * <pre>
   *     CollectionUtils.addAll(flavors, list);
   *     CollectionUtils.addAll(flavors, null); // add nothing element can be null
   * </pre>
   *
   * @param c the collection into which <tt>elements</tt> are to be inserted
   * @param elements the elements to insert into <tt>c</tt>
   * @throws UnsupportedOperationException if <tt>c</tt> does not support
   * the <tt>add</tt> operation
   * @throws NullPointerException if <tt>elements</tt> contains one or more
   * null values and <tt>c</tt> does not permit null elements, or
   * if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException if some property of a value in
   * <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#addAll(Collection)
   * @since 1.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(Collection c, @Nullable Collection elements) {
    if (elements != null) {
      c.addAll(elements);
    }
  }

  /**
   * Copies all of the mappings from the specified map to target map
   * (optional operation).  The effect of this call is equivalent to that
   * of calling {@link Map#put(Object, Object) put(k, v)} on this map once
   * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
   * specified map.  The behavior of this operation is undefined if the
   * specified map is modified while the operation is in progress.
   *
   * @param mappings mappings to be stored in target map
   * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
   * is not supported by this map
   * @throws ClassCastException if the class of a key or value in the
   * specified map prevents it from being stored in this map
   * @throws NullPointerException if the specified map is null, or if
   * this map does not permit null keys or values, and the
   * specified map contains null keys or values
   * @throws IllegalArgumentException if some property of a key or value in
   * the specified map prevents it from being stored in this map
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void putAll(Map target, @Nullable Map mappings) {
    if (mappings != null) {
      target.putAll(mappings);
    }
  }

}
