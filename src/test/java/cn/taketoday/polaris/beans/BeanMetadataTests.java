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

package cn.taketoday.polaris.beans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/28 16:46
 */
class BeanMetadataTests {

  @Test
  void beanProperties() {
    BeanMetadata properties = BeanMetadata.forClass(BeanModel.class);
    assertThat(properties.beanProperties()).hasSize(2);
    assertThat(properties.getPropertySize()).isEqualTo(2);

    assertThat(properties.containsProperty("id")).isTrue();
    assertThat(properties.containsProperty("name")).isTrue();
    assertThat(properties.containsProperty("name1")).isFalse();
  }

  @Test
  void getBeanProperty() {
    BeanMetadata properties = BeanMetadata.forClass(BeanModel.class);
    assertThat(properties.getBeanProperty("id")).isNotNull().extracting("name").isEqualTo("id");
    assertThat(properties.getBeanProperty("name")).isNotNull().extracting("name").isEqualTo("name");
  }

  @Test
  void newInstance(){
    BeanMetadata metadata = BeanMetadata.forClass(BeanModel.class);
    assertThat(metadata.newInstance()).isNotNull().isInstanceOf(BeanModel.class);
    assertThat(metadata.newInstance()).isNotEqualTo(metadata.newInstance());
  }

  static class BeanModel {

    public int id;

    public String name;
  }

  static class PrefixModel {

    public int mId;

    public String mName;

    public int _age;
  }

}