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

/**
 * Property Update Strategy
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UpdateStrategySource
 * @since 4.0 2022/12/18 22:20
 */
public interface PropertyUpdateStrategy {

  /**
   * Test input property should be updated?
   */
  boolean shouldUpdate(Object entity, EntityProperty property);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new Strategy
   */
  default PropertyUpdateStrategy and(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) && next.shouldUpdate(entity, property);
  }

  /**
   * returns a new chain
   *
   * @param next next resolver
   * @return returns a new Strategy
   */
  default PropertyUpdateStrategy or(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) || next.shouldUpdate(entity, property);
  }

  /**
   * Update the none null property
   */
  static PropertyUpdateStrategy noneNull() {
    return (entity, property) -> property.getValue(entity) != null;
  }

  /**
   * Always update
   */
  static PropertyUpdateStrategy always() {
    return (entity, property) -> true;
  }

}
