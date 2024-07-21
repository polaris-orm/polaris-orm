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

package cn.taketoday.polaris.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.EntityProperty;
import cn.taketoday.polaris.PropertyConditionStrategy;
import cn.taketoday.polaris.sql.Restriction;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/28 22:43
 */
public class DefaultConditionStrategy implements PropertyConditionStrategy {

  @Nullable
  @Override
  public Condition resolve(EntityProperty entityProperty, Object propertyValue) {
    return new Condition(propertyValue, Restriction.equal(entityProperty.columnName), entityProperty);
  }

}
