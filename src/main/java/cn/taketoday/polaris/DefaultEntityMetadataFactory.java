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
import java.util.Set;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.polaris.jdbc.type.TypeHandler;
import cn.taketoday.polaris.jdbc.type.TypeHandlerManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * Default EntityHolderFactory
 * <p>
 * <ul>
 * <li> {@link cn.taketoday.polaris.TableNameGenerator} to generate table name
 * <li> {@link cn.taketoday.polaris.IdPropertyDiscover} to find the ID property
 * <li> {@link cn.taketoday.polaris.ColumnNameDiscover} to find column name
 * <li> {@link PropertyFilter} to filter the property
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyFilter
 * @see cn.taketoday.polaris.TableNameGenerator
 * @see cn.taketoday.polaris.IdPropertyDiscover
 * @see cn.taketoday.polaris.ColumnNameDiscover
 * @since 4.0 2022/9/5 11:38
 */
public class DefaultEntityMetadataFactory extends EntityMetadataFactory {

  private PropertyFilter propertyFilter = PropertyFilter.filteredNames(Set.of("class"))
          .and(PropertyFilter.forTransientAnnotation());

  private cn.taketoday.polaris.TableNameGenerator tableNameGenerator = cn.taketoday.polaris.TableNameGenerator.defaultStrategy();

  private cn.taketoday.polaris.IdPropertyDiscover idPropertyDiscover = cn.taketoday.polaris.IdPropertyDiscover.forIdAnnotation()
          .and(cn.taketoday.polaris.IdPropertyDiscover.forPropertyName(cn.taketoday.polaris.IdPropertyDiscover.DEFAULT_ID_PROPERTY));

  private cn.taketoday.polaris.ColumnNameDiscover columnNameDiscover = cn.taketoday.polaris.ColumnNameDiscover.forColumnAnnotation()
          .and(cn.taketoday.polaris.ColumnNameDiscover.camelCaseToUnderscore());

  private TypeHandlerManager typeHandlerManager = TypeHandlerManager.sharedInstance;

  /**
   * set the ColumnNameDiscover to find the column name
   *
   * @param columnNameDiscover ColumnNameDiscover
   */
  public void setColumnNameDiscover(ColumnNameDiscover columnNameDiscover) {
    Assert.notNull(columnNameDiscover, "columnNameDiscover is required");
    this.columnNameDiscover = columnNameDiscover;
  }

  /**
   * set the IdPropertyDiscover to determine the ID property
   *
   * @param idPropertyDiscover a new IdPropertyDiscover
   */
  public void setIdPropertyDiscover(IdPropertyDiscover idPropertyDiscover) {
    Assert.notNull(idPropertyDiscover, "idPropertyDiscover is required");
    this.idPropertyDiscover = idPropertyDiscover;
  }

  /**
   * set the PropertyFilter to filter the property
   *
   * @param propertyFilter a PropertyFilter
   */
  public void setPropertyFilter(PropertyFilter propertyFilter) {
    Assert.notNull(propertyFilter, "propertyFilter is required");
    this.propertyFilter = propertyFilter;
  }

  /**
   * set the TableNameGenerator to generate the table name for the class
   *
   * @param tableNameGenerator a TableNameGenerator
   */
  public void setTableNameGenerator(TableNameGenerator tableNameGenerator) {
    Assert.notNull(tableNameGenerator, "tableNameGenerator is required");
    this.tableNameGenerator = tableNameGenerator;
  }

  /**
   * Set the TypeHandlerManager to find {@link TypeHandler} for the {@link BeanProperty}
   *
   * @param typeHandlerManager TypeHandlerManager
   */
  public void setTypeHandlerManager(TypeHandlerManager typeHandlerManager) {
    Assert.notNull(typeHandlerManager, "TypeHandlerManager is required");
    this.typeHandlerManager = typeHandlerManager;
  }

  @Override
  public EntityMetadata createEntityMetadata(Class<?> entityClass) {
    String tableName = tableNameGenerator.generateTableName(entityClass);
    if (tableName == null) {
      throw new IllegalEntityException("Cannot determine table name for entity: " + entityClass);
    }

    BeanMetadata metadata = BeanMetadata.from(entityClass);
    ArrayList<String> columnNames = new ArrayList<>();
    ArrayList<BeanProperty> beanProperties = new ArrayList<>();
    ArrayList<cn.taketoday.polaris.EntityProperty> entityProperties = new ArrayList<>();

    cn.taketoday.polaris.EntityProperty idProperty = null;
    for (BeanProperty property : metadata) {
      if (isFiltered(property)) {
        continue;
      }

      String columnName = columnNameDiscover.getColumnName(property);
      if (columnName == null) {
        throw new IllegalEntityException("Cannot determine column name for property: %s#%s"
                .formatted(ClassUtils.getShortName(property.getDeclaringClass()), property.getName()));
      }

      columnNames.add(columnName);
      beanProperties.add(property);

      if (idPropertyDiscover.isIdProperty(property)) {
        if (idProperty != null) {
          throw new IllegalEntityException("Only one Id property supported, entity: " + entityClass);
        }
        idProperty = createEntityProperty(property, columnName, true);
        entityProperties.add(idProperty);
      }
      else {
        entityProperties.add(createEntityProperty(property, columnName, false));
      }
    }

    if (idProperty == null && entityProperties.isEmpty()) {
      throw new IllegalEntityException("Cannot determine properties for entity: " + entityClass);
    }

    return new EntityMetadata(metadata, entityClass,
            idProperty, tableName, beanProperties, columnNames, entityProperties);
  }

  private cn.taketoday.polaris.EntityProperty createEntityProperty(BeanProperty property, String columnName, boolean isId) {
    return new EntityProperty(property, columnName, typeHandlerManager.getTypeHandler(property), isId);
  }

  private boolean isFiltered(BeanProperty property) {
    return isInnerClass(property) || propertyFilter.isFiltered(property);
  }

  private static boolean isInnerClass(BeanProperty property) {
    return property.getName().equals("this$0");
  }

}
