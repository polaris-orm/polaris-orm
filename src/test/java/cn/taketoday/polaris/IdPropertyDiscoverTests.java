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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.BeanMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/15 13:43
 */
class IdPropertyDiscoverTests {

  @Test
  void composite() {
    cn.taketoday.polaris.IdPropertyDiscover propertyDiscover = cn.taketoday.polaris.IdPropertyDiscover.composite(
            cn.taketoday.polaris.IdPropertyDiscover.forPropertyName("myId"),
            cn.taketoday.polaris.IdPropertyDiscover.forPropertyName("myId_"),
            IdPropertyDiscover.forAnnotation(MyId.class));
    BeanMetadata metadata = BeanMetadata.from(MyIdEntity.class);
    propertyDiscover.isIdProperty(metadata.obtainBeanProperty("id"));
    assertThat(propertyDiscover.isIdProperty(metadata.obtainBeanProperty("id"))).isFalse();
    assertThat(propertyDiscover.isIdProperty(metadata.obtainBeanProperty("myId"))).isTrue();

    assertThat(propertyDiscover.isIdProperty(BeanMetadata.from(MyIdAnnoEntity.class).obtainBeanProperty("id"))).isTrue();

  }

  static class MyIdEntity {

    Long id;

    Long myId;
  }

  static class MyIdAnnoEntity {

    @MyId
    Long id;

    Long myId1;
  }

  @Target({ ElementType.FIELD, ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyId {

  }

}