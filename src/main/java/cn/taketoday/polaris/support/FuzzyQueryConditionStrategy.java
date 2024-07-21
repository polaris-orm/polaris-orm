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

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.polaris.EntityProperty;
import cn.taketoday.polaris.PropertyConditionStrategy;
import cn.taketoday.polaris.sql.Restriction;
import cn.taketoday.polaris.Like;
import cn.taketoday.polaris.PrefixLike;
import cn.taketoday.polaris.SuffixLike;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/28 22:48
 */
public class FuzzyQueryConditionStrategy implements PropertyConditionStrategy {

  @Nullable
  @Override
  public Condition resolve(EntityProperty entityProperty, Object propertyValue) {
    MergedAnnotation<Like> annotation = entityProperty.getAnnotation(Like.class);
    if (annotation.isPresent()) {
      // get column name
      String column = annotation.getStringValue();
      if (Constant.DEFAULT_NONE.equals(column)) {
        column = entityProperty.columnName;
      }

      // handle string

      if (propertyValue instanceof String string) {
        // trim
        if (annotation.getBoolean("trim")) {
          string = string.trim();
        }
        if (entityProperty.isPresent(PrefixLike.class)) {
          string = string + '%';
        }
        else if (entityProperty.isPresent(SuffixLike.class)) {
          string = '%' + string;
        }
        else {
          string = '%' + string + '%';
        }

        propertyValue = string;
      }

      return new Condition(propertyValue, new LikeRestriction(column), entityProperty);
    }

    return null;
  }

  static class LikeRestriction implements Restriction {

    final String columnName;

    LikeRestriction(String columnName) {
      this.columnName = columnName;
    }

    @Override
    public void render(StringBuilder sqlBuffer) {
      sqlBuffer.append('`')
              .append(columnName)
              .append("` like ?");
    }
  }

}
