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

import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 17:55
 */
final class QueryHandlerFactories implements QueryHandlerFactory {

  final List<QueryHandlerFactory> factories;

  QueryHandlerFactories(EntityMetadataFactory entityMetadataFactory) {
    List<QueryHandlerFactory> list = TodayStrategies.find(QueryHandlerFactory.class);
    list.add(new MapQueryHandlerFactory());
    list.add(new DefaultQueryHandlerFactory(entityMetadataFactory));
    this.factories = List.copyOf(list);
  }

  @Nullable
  @Override
  public QueryStatement createQuery(Object example) {
    for (QueryHandlerFactory factory : factories) {
      QueryStatement query = factory.createQuery(example);
      if (query != null) {
        return query;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public ConditionStatement createCondition(Object example) {
    for (QueryHandlerFactory factory : factories) {
      ConditionStatement condition = factory.createCondition(example);
      if (condition != null) {
        return condition;
      }
    }
    return null;
  }

}
