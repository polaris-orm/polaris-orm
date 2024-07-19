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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * default {@link TableNameGenerator}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 23:09
 */
public class DefaultTableNameGenerator implements TableNameGenerator {

  private final TableNameGenerator annotationGenerator = TableNameGenerator.forTableAnnotation();

  @Nullable
  private String prefixToAppend;

  @Nullable
  private String[] suffixArrayToRemove;

  private boolean lowercase = true;

  private boolean camelCaseToUnderscore = true;

  public void setLowercase(boolean lowercase) {
    this.lowercase = lowercase;
  }

  public void setCamelCaseToUnderscore(boolean camelCaseToUnderscore) {
    this.camelCaseToUnderscore = camelCaseToUnderscore;
  }

  public void setPrefixToAppend(@Nullable String prefixToAppend) {
    this.prefixToAppend = prefixToAppend;
  }

  public void setSuffixToRemove(@Nullable String suffixArrayToRemove) {
    if (suffixArrayToRemove == null) {
      this.suffixArrayToRemove = null;
    }
    else {
      this.suffixArrayToRemove = new String[] { suffixArrayToRemove };
    }
  }

  public void setSuffixArrayToRemove(@Nullable String... suffixToRemove) {
    this.suffixArrayToRemove = suffixToRemove;
  }

  @Override
  public String generateTableName(Class<?> entityClass) {
    String name = annotationGenerator.generateTableName(entityClass);
    if (name != null) {
      return name;
    }

    String simpleName = entityClass.getSimpleName();

    // append the prefix like "t_" -> t_user, t_order
    StringBuilder tableName = new StringBuilder();
    if (StringUtils.hasText(prefixToAppend)) {
      tableName.append(prefixToAppend);
    }

    // remove the common suffix like UserModel - Model = User, UserEntity - Entity = User
    if (ObjectUtils.isNotEmpty(suffixArrayToRemove)) {
      for (String suffix : suffixArrayToRemove) {
        if (simpleName.endsWith(suffix)) {
          simpleName = simpleName.substring(0, simpleName.length() - suffix.length());
          break;
        }
      }
    }

    // UserOrder -> user_order
    if (camelCaseToUnderscore) {
      simpleName = StringUtils.camelCaseToUnderscore(simpleName);
    }
    else if (lowercase) {
      // User -> user
      simpleName = simpleName.toLowerCase();
    }

    tableName.append(simpleName);
    return tableName.toString();
  }
}
