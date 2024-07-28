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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/28 17:26
 */
class CollectionUtilsTests {

  @Test
  void isEmpty() {
    assertThat(CollectionUtils.isEmpty((Set<Object>) null)).isTrue();
    assertThat(CollectionUtils.isEmpty((Map<String, String>) null)).isTrue();
    assertThat(CollectionUtils.isEmpty(new HashMap<String, String>())).isTrue();
    assertThat(CollectionUtils.isEmpty(new HashSet<>())).isTrue();

    List<Object> list = new ArrayList<>();
    list.add(new Object());
    assertThat(CollectionUtils.isEmpty(list)).isFalse();

    Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    assertThat(CollectionUtils.isEmpty(map)).isFalse();
  }

  @Test
  void contains() {

    List<String> list = new ArrayList<>();
    list.add("myElement");

    Hashtable<String, String> ht = new Hashtable<>();
    ht.put("myElement", "myValue");

    //  contains(@Nullable Object[] array, Object element) {

    Object[] array = list.toArray();
    assertThat(ObjectUtils.containsElement(array, "myElement")).isTrue();
    assertThat(ObjectUtils.containsElement(array, "myElements")).isFalse();
    assertThat(ObjectUtils.containsElement((Object[]) null, "myElements")).isFalse();

  }

  @Test
  void containsAny() throws Exception {
    List<String> source = new ArrayList<>();
    source.add("abc");
    source.add("def");
    source.add("ghi");

    List<String> candidates = new ArrayList<>();
    candidates.add("xyz");
    candidates.add("def");
    candidates.add("abc");

  }

  @Test
  void getElement() {
    List<String> list = new ArrayList<>();
    list.add("myElement");
    list.add("myOtherElement");

    assertThat(CollectionUtils.getElement(list, 0)).isNotNull().isNotEmpty().isEqualTo("myElement");
    assertThat(CollectionUtils.getElement(list, 1)).isNotNull().isNotEmpty().isEqualTo("myOtherElement");
    assertThat(CollectionUtils.getElement(list, -1)).isNull();
    assertThat(CollectionUtils.getElement(list, 10)).isNull();
  }

  @Test
  void firstElement() {
    List<String> list = new ArrayList<>();
    list.add("myElement");
    list.add("myOtherElement");
    assertThat(CollectionUtils.firstElement(list)).isNotNull().isNotEmpty().isEqualTo("myElement");
    assertThat(CollectionUtils.firstElement((Collection<String>) list)).isNotNull().isNotEmpty().isEqualTo("myElement");

    //
    assertThat(CollectionUtils.firstElement((List<?>) null)).isNull();
    assertThat(CollectionUtils.firstElement((Collection<?>) null)).isNull();

    TreeSet<String> objects = new TreeSet<>();

    objects.add("myElement");
    objects.add("myOtherElement");
    assertThat(CollectionUtils.firstElement(objects)).isNotNull().isNotEmpty().isEqualTo("myElement");

    // lastElement

    assertThat(CollectionUtils.lastElement((List<?>) null)).isNull();
    assertThat(CollectionUtils.lastElement(list)).isNotNull().isNotEmpty().isEqualTo("myOtherElement");
  }

}