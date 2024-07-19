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

package cn.taketoday.polaris;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/11 14:56
 */
class PageableTests {

  @Test
  void pageNumber() {
    assertThat(cn.taketoday.polaris.Pageable.of(1, 10).pageNumber()).isEqualTo(1);
    assertThat(cn.taketoday.polaris.Pageable.of(3, 10).pageNumber()).isEqualTo(3);
    assertThat(cn.taketoday.polaris.Pageable.of(4, 10).pageNumber()).isEqualTo(4);
    assertThat(cn.taketoday.polaris.Pageable.of(4, 1).pageNumber()).isEqualTo(4);
  }

  @Test
  void pageSize() {
    assertThat(cn.taketoday.polaris.Pageable.of(1, 10).pageSize()).isEqualTo(10);
    assertThat(cn.taketoday.polaris.Pageable.of(1, 10).pageSize(5)).isEqualTo(5);
    assertThat(cn.taketoday.polaris.Pageable.of(2, 10).pageSize(5)).isEqualTo(5);
  }

  @Test
  void offset() {
    assertThat(cn.taketoday.polaris.Pageable.of(1, 10).offset()).isEqualTo(0);
    assertThat(cn.taketoday.polaris.Pageable.of(2, 10).offset()).isEqualTo(10);
    assertThat(cn.taketoday.polaris.Pageable.of(1, 10).offset(5)).isEqualTo(0);

    assertThat(cn.taketoday.polaris.Pageable.of(2, 10).offset(5)).isEqualTo(5);
    assertThat(cn.taketoday.polaris.Pageable.of(3, 10).offset(5)).isEqualTo(10);
  }

  @Test
  void of() {
    assertThat(cn.taketoday.polaris.Pageable.of(1, 10)).isEqualTo(new cn.taketoday.polaris.SimplePageable(1, 10));
  }

  @Test
  void unwrap() {
    assertThat(cn.taketoday.polaris.Pageable.unwrap(1)).isNull();
    assertThat(cn.taketoday.polaris.Pageable.unwrap(null)).isNull();
    assertThat(cn.taketoday.polaris.Pageable.unwrap(cn.taketoday.polaris.Pageable.of(1, 10))).isNotNull();
  }

  @Test
  void toString_() {
    assertThat(Pageable.of(1, 10).toString()).endsWith("pageNumber = 1, pageSize = 10]");
  }

}