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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Stream Iterable
 * <p>
 * just use once
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/14 18:46
 */
public class StreamIterable<T> implements Iterable<T> {
  private final Stream<T> stream;

  public StreamIterable(Stream<T> stream) {
    this.stream = stream;
  }

  @Override
  public Iterator<T> iterator() {
    return stream.iterator();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    stream.forEach(action);
  }

  @Override
  public Spliterator<T> spliterator() {
    return stream.spliterator();
  }

}
