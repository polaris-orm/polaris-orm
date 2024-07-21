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

import java.util.ArrayList;

/**
 * Batch execution metadata
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/20 23:25
 */
public class BatchExecution {

  public final String sql;

  public final boolean autoGenerateId;

  public final EntityMetadata entityMetadata;

  public final PropertyUpdateStrategy strategy;

  public final ArrayList<Object> entities = new ArrayList<>();

  BatchExecution(String sql, PropertyUpdateStrategy strategy,
          EntityMetadata entityMetadata, boolean autoGenerateId) {
    this.sql = sql;
    this.strategy = strategy;
    this.entityMetadata = entityMetadata;
    this.autoGenerateId = autoGenerateId;
  }

}
