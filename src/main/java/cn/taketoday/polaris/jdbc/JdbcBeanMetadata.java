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

package cn.taketoday.polaris.jdbc;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.polaris.annotation.Column;
import cn.taketoday.polaris.beans.BeanMetadata;
import cn.taketoday.polaris.beans.BeanProperty;
import cn.taketoday.polaris.util.MapCache;
import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.StringUtils;

/**
 * Stores metadata for a POJO
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class JdbcBeanMetadata {

  private static final Cache caseSensitiveFalse = new Cache();

  private static final Cache caseSensitiveTrue = new Cache();

  private final boolean caseSensitive;

  private final boolean autoDeriveColumnNames;

  final boolean throwOnMappingFailure;

  private final BeanMetadata beanMetadata;

  @Nullable
  private HashMap<String, BeanProperty> beanProperties;

  public JdbcBeanMetadata(Class<?> clazz, boolean caseSensitive, boolean autoDeriveColumnNames, boolean throwOnMappingError) {
    this.caseSensitive = caseSensitive;
    this.beanMetadata = BeanMetadata.forClass(clazz);
    this.throwOnMappingFailure = throwOnMappingError;
    this.autoDeriveColumnNames = autoDeriveColumnNames;
  }

  @Nullable
  public BeanProperty getBeanProperty(String colName, @Nullable Map<String, String> columnMappings) {
    if (columnMappings != null) {
      // find in columnMappings
      String propertyName = columnMappings.get(caseSensitive ? colName : colName.toLowerCase());
      if (propertyName != null) {
        // find
        BeanProperty beanProperty = getProperty(propertyName);
        if (beanProperty != null) {
          return beanProperty;
        }
      }
    }

    // try direct
    BeanProperty beanProperty = getProperty(colName);
    if (beanProperty != null) {
      return beanProperty;
    }

    // fallback
    if (autoDeriveColumnNames) {
      String propertyName = StringUtils.underscoreToCamelCase(colName);
      return getProperty(propertyName);
    }

    return null;
  }

  @Nullable
  private BeanProperty getProperty(String propertyName) {
    HashMap<String, BeanProperty> beanProperties = this.beanProperties;
    if (beanProperties == null) {
      beanProperties = (caseSensitive ? caseSensitiveTrue : caseSensitiveFalse).get(beanMetadata.getType(), this);
      this.beanProperties = beanProperties;
    }

    return beanProperties.get(caseSensitive ? propertyName : propertyName.toLowerCase());
  }

  //

  public Class<?> getObjectType() {
    return beanMetadata.getType();
  }

  public Object newInstance() {
    return beanMetadata.newInstance();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof JdbcBeanMetadata that))
      return false;
    return caseSensitive == that.caseSensitive
            && throwOnMappingFailure == that.throwOnMappingFailure
            && autoDeriveColumnNames == that.autoDeriveColumnNames
            && Objects.equals(beanMetadata, that.beanMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseSensitive, throwOnMappingFailure, autoDeriveColumnNames, beanMetadata);
  }

  /**
   * get alias property-name
   *
   * @param property {@link Field}
   */
  static String getPropertyName(BeanProperty property) {
    String propertyName = getAnnotatedPropertyName(property);
    if (propertyName == null) {
      propertyName = property.getName();
    }
    return propertyName;
  }

  @Nullable
  static String getAnnotatedPropertyName(AnnotatedElement propertyElement) {
    // just alias name, cannot override its getter,setter
    Column annotation = propertyElement.getAnnotation(Column.class);
    if (annotation != null) {
      String name = annotation.value();
      if (StringUtils.isNotEmpty(name)) {
        return name;
      }
    }
    return null;
  }

  static class Cache extends MapCache<Class<?>, HashMap<String, BeanProperty>, JdbcBeanMetadata> {

    @Override
    protected HashMap<String, BeanProperty> createValue(Class<?> key, JdbcBeanMetadata params) {
      boolean caseSensitive = params.caseSensitive;
      HashMap<String, BeanProperty> beanPropertyMap = new HashMap<>();
      for (BeanProperty property : params.beanMetadata) {
        String propertyName_ = getPropertyName(property);
        if (caseSensitive) {
          beanPropertyMap.put(propertyName_, property);
        }
        else {
          beanPropertyMap.put(propertyName_.toLowerCase(), property);
        }
      }
      return beanPropertyMap;
    }
  }

}
