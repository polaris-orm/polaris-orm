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

import java.lang.annotation.Annotation;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultTableNameGenerator
 * @since 1.0 2022/8/16 21:19
 */
public interface TableNameGenerator {

  /**
   * Generate table name from {@code entityClass}
   *
   * @param entityClass entity-class
   * @return table-name
   */
  @Nullable
  String generateTableName(Class<?> entityClass);

  /**
   * returns a new resolving chain
   *
   * @param next next resolver
   * @return returns a new resolving chain
   */
  default TableNameGenerator and(TableNameGenerator next) {
    return entityClass -> {
      String name = generateTableName(entityClass);
      if (name == null) {
        return next.generateTableName(entityClass);
      }
      return name;
    };
  }

  // Static Factory Methods

  /**
   *
   */
  static DefaultTableNameGenerator defaultStrategy() {
    return new DefaultTableNameGenerator();
  }

  /**
   * use {@link Table#name()}
   */
  static TableNameGenerator forTableAnnotation() {
    return forAnnotation(Table.class);
  }

  /**
   * Use input {@code annotationType} to resolve table name
   *
   * @param annotationType Annotation type
   * @return Annotation based {@link TableNameGenerator}
   * @see MergedAnnotation#getString(String)
   */
  static TableNameGenerator forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Use input {@code annotationType} and {@code attributeName} to resolve table name
   *
   * @param annotationType Annotation type
   * @param attributeName the attribute name
   * @return Annotation based {@link TableNameGenerator}
   * @see MergedAnnotation#getString(String)
   */
  static TableNameGenerator forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    class ForAnnotation implements TableNameGenerator {

      @Override
      public String generateTableName(Class<?> entityClass) {
        MergedAnnotations annotations = MergedAnnotations.from(entityClass);
        var annotation = annotations.get(annotationType);
        if (annotation.isPresent()) {
          String name = annotation.getString(attributeName);
          if (StringUtils.hasText(name)) {
            return name;
          }
        }

        var ref = annotations.get(EntityRef.class);
        if (ref.isPresent()) {
          Class<?> classValue = ref.getClassValue();
          return generateTableName(classValue);
        }
        return null;
      }
    }

    return new ForAnnotation();
  }
}
