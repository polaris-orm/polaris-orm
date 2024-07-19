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

import cn.taketoday.util.MapCache;

/**
 * {@link EntityMetadata} Factory
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:28
 */
public abstract class EntityMetadataFactory {

  final MapCache<Class<?>, EntityMetadata, EntityMetadataFactory> entityCache = new MapCache<>() {

    @Override
    protected EntityMetadata createValue(Class<?> entityClass, EntityMetadataFactory entityMetadataFactory) {
      return entityMetadataFactory.createEntityMetadata(entityClass);
    }
  };

  /**
   * Get a EntityMetadata instance
   *
   * @param entityClass entity Class
   * @return EntityMetadata may be a cached instance
   * @throws IllegalEntityException entity definition is not legal
   */
  public EntityMetadata getEntityMetadata(Class<?> entityClass) throws IllegalEntityException {
    return entityCache.get(entityClass, this);
  }

  /**
   * create a new EntityMetadata instance
   *
   * @param entityClass entity class
   * @return a new EntityMetadata
   */
  public abstract EntityMetadata createEntityMetadata(Class<?> entityClass) throws IllegalEntityException;

}
