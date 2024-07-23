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

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.polaris.beans.BeanProperty;
import cn.taketoday.polaris.jdbc.ResultSetIterator;

/**
 * Iterator for a {@link ResultSet}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/22 19:38
 */
public abstract class EntityIterator<T> extends ResultSetIterator<T> {

  private final EntityMetadata entityMetadata;

  protected EntityIterator(ResultSet resultSet, EntityMetadata entityMetadata) {
    super(resultSet);
    this.entityMetadata = entityMetadata;
  }

  /**
   * Convert entities to Map
   *
   * @param mapKey entity property
   */
  @SuppressWarnings("unchecked")
  public <K> Map<K, T> toMap(String mapKey) {
    try {
      LinkedHashMap<K, T> entities = new LinkedHashMap<>();
      BeanProperty beanProperty = entityMetadata.root.obtainBeanProperty(mapKey);
      while (hasNext()) {
        T entity = next();
        Object propertyValue = beanProperty.getValue(entity);
        entities.put((K) propertyValue, entity);
      }
      return entities;
    }
    finally {
      close();
    }
  }

  /**
   * Convert entities to Map
   *
   * @param keyMapper key mapping function
   */
  public <K> Map<K, T> toMap(Function<T, K> keyMapper) {
    try {
      LinkedHashMap<K, T> entities = new LinkedHashMap<>();
      while (hasNext()) {
        T entity = next();
        K key = keyMapper.apply(entity);
        entities.put(key, entity);
      }
      return entities;
    }
    finally {
      close();
    }
  }

}
