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
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a generic pair of two values.
 * <p>
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.
 * <p>
 * An example of decomposing it into values:
 *
 * @param <A> type of the first value.
 * @param <B> type of the second value.
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/8/24 14:45
 */
public class Pair<A, B> implements Map.Entry<A, B>, Serializable {

  @SuppressWarnings({ "rawtypes" })
  public static final Pair EMPTY = of(null, null);

  @Serial
  private static final long serialVersionUID = 1L;

  public final A first;

  public final B second;

  private Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  public final A getFirst() {
    return first;
  }

  public final B getSecond() {
    return second;
  }

  @Override
  public A getKey() {
    return first;
  }

  @Override
  public B getValue() {
    return second;
  }

  @Override
  public B setValue(B value) {
    throw new UnsupportedOperationException();
  }

  public Pair<A, B> withFirst(A first) {
    if (first == this.first) {
      return this;
    }
    return new Pair<>(first, second);
  }

  public Pair<A, B> withSecond(B second) {
    if (second == this.second) {
      return this;
    }
    return new Pair<>(first, second);
  }

  public Optional<A> first() {
    return Optional.ofNullable(first);
  }

  public Optional<B> second() {
    return Optional.ofNullable(second);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Pair<?, ?> pair))
      return false;
    return Objects.equals(first, pair.first)
            && Objects.equals(second, pair.second);
  }

  @Override
  public int hashCode() {
    return 31 * (31 + Objects.hashCode(first)) + Objects.hashCode(second);
  }

  /**
   * Returns string representation of the [Pair] including its [first] and [second] values.
   */
  @Override
  public String toString() {
    return "<" + first + "," + second + ">";
  }

  // Static

  @SuppressWarnings("unchecked")
  public static <A, B> Pair<A, B> empty() {
    return EMPTY;
  }

  public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    return new Pair<>(first, second);
  }

  @Nullable
  public static <T> T getFirst(@Nullable Pair<T, ?> pair) {
    return pair != null ? pair.first : null;
  }

  @Nullable
  public static <T> T getSecond(@Nullable Pair<?, T> pair) {
    return pair != null ? pair.second : null;
  }

  /**
   * @param <A> first value type (Comparable)
   * @param <B> second value type
   * @return a comparator that compares pair values by first value
   */
  public static <A extends Comparable<? super A>, B> Comparator<Pair<A, B>> comparingFirst() {
    return Comparator.comparing(o -> o.first);
  }

  /**
   * @param <A> first value type
   * @param <B> second value type (Comparable)
   * @return a comparator that compares pair values by second value
   */
  public static <A, B extends Comparable<? super B>> Comparator<Pair<A, B>> comparingSecond() {
    return Comparator.comparing(o -> o.second);
  }

}
