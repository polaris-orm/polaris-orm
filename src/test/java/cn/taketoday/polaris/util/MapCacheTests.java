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

import java.util.HashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/28 17:25
 */
class MapCacheTests {

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void mappingFunction() {
    HashMap map = new HashMap<>();
    MapCache<String, String, Object> cache = new MapCache<>(map, Function.identity());
    assertThat(cache.get("1")).isEqualTo("1");
    assertThat(cache).extracting("mapping").isSameAs(map);
    assertThat(cache.get(null, null)).isNull();
    assertThat(cache.get(null)).isNull();

    assertThat(map).contains(Pair.of("1", "1"), Pair.of(null, NullValue.INSTANCE));

    cache.clear();
    assertThat(cache.get(null)).isNull();
    assertThat(map).contains(Pair.of(null, NullValue.INSTANCE));

    assertThat(cache.remove(null)).isSameAs(null);
    assertThat(map).isEmpty();
    assertThat(cache.put(null, null)).isSameAs(null);

    assertThat(map).isNotEmpty();
    assertThat(cache.get(null)).isNull();
  }

}