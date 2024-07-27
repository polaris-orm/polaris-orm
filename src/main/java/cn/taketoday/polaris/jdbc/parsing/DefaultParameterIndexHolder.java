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

package cn.taketoday.polaris.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import cn.taketoday.polaris.jdbc.ParameterBinder;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0
 */
final class DefaultParameterIndexHolder extends ParameterIndexHolder {

  final int index;

  DefaultParameterIndexHolder(int index) {
    this.index = index;
  }

  @Override
  public void bind(ParameterBinder binder, PreparedStatement statement) throws SQLException {
    binder.bind(statement, index);
  }

  public int getIndex() {
    return index;
  }

  //---------------------------------------------------------------------
  // Implementation of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final DefaultParameterIndexHolder that))
      return false;
    return index == that.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(index);
  }

  @Override
  public String toString() {
    return "index=" + index;
  }

  //---------------------------------------------------------------------
  // Implementation of Iterable interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<>() {
      private boolean hasNext = true;

      @Override
      public boolean hasNext() {
        return hasNext;
      }

      @Override
      public Integer next() {
        if (hasNext) {
          hasNext = false;
          return index;
        }
        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void forEachRemaining(Consumer<? super Integer> action) {
        Objects.requireNonNull(action);
        if (hasNext) {
          action.accept(index);
          hasNext = false;
        }
      }
    };
  }

}
