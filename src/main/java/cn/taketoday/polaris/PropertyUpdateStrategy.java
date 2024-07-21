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
 * 字段更新策略
 * <p>
 * {@link #shouldUpdate} 方法决定你要不要更新这个字段
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UpdateStrategySource
 * @since 1.0 2022/12/18 22:20
 */
public interface PropertyUpdateStrategy {

  /**
   * 决定该字段要不要更新
   *
   * @return 如果返回 true 说明输入的 {@code property} 需要更新。
   */
  boolean shouldUpdate(Object entity, EntityProperty property);

  /**
   * 返回一个新的实例，这个新对象组合了当前对象和 {@code next} 对象。
   * <p>
   * 新的策略要在这两个策略都要满足才会返回 true
   *
   * @param next 下一个策略
   * @return 返回一个组合的策略
   */
  default PropertyUpdateStrategy and(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) && next.shouldUpdate(entity, property);
  }

  /**
   * 返回一个新的实例，这个新对象组合了当前对象和 {@code next} 对象。
   * <p>
   * 新的策略要在这两个策略其中一个满足就会返回 true
   *
   * @param next 下一个策略
   * @return 返回一个组合的策略
   */
  default PropertyUpdateStrategy or(PropertyUpdateStrategy next) {
    return (entity, property) -> shouldUpdate(entity, property) || next.shouldUpdate(entity, property);
  }

  // Static factory methods

  /**
   * 创建一个更新字段不为 {@code null} 的策略
   */
  static PropertyUpdateStrategy noneNull() {
    return (entity, property) -> property.getValue(entity) != null;
  }

  /**
   * 创建一个 所有字段都更新的策略
   */
  static PropertyUpdateStrategy always() {
    return (entity, property) -> true;
  }

}
