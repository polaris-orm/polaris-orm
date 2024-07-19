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

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.lang.Constant;
import cn.taketoday.polaris.model.UserModel;

import static cn.taketoday.beans.BeanProperty.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/31 11:16
 */
class ColumnNameDiscoverTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.FIELD })
  public @interface MyColumn {

    @AliasFor("name")
    String value() default Constant.BLANK;

    @AliasFor("value")
    String name() default Constant.BLANK;

    String myValue() default "";

  }

  @Test
  void forColumnAnnotation() {
    class Model {
      @Column("test-name")
      int test;
    }

    cn.taketoday.polaris.ColumnNameDiscover nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.forColumnAnnotation();
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age")))
            .isEqualTo(null);

    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test")))
            .isEqualTo("test-name");

  }

  @Test
  void forAnnotation() {
    class Model {
      @MyColumn("test-name")
      int test;

      @MyColumn(myValue = "test-name")
      int test1;
    }

    var nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.forAnnotation(MyColumn.class);
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo(null);
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test"))).isEqualTo("test-name");

    var myValueDiscover = cn.taketoday.polaris.ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue");
    assertThat(myValueDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo(null);
    assertThat(myValueDiscover.getColumnName(valueOf(Model.class, "test"))).isEqualTo(null);
    assertThat(myValueDiscover.getColumnName(valueOf(Model.class, "test1"))).isEqualTo("test-name");

  }

  @Test
  void camelCaseToUnderscore() {
    class Model {

      int testName;
    }

    var nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.camelCaseToUnderscore();
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobile_phone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("test_name");

  }

  @Test
  void forPropertyName() {
    class Model {

      int testName;
    }

    var nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.forPropertyName();
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobilePhone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("testName");

  }

  @Test
  void and() {
    class Model {
      int testName;

      @MyColumn(myValue = "test-name")
      int test1;
    }

    var nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.forColumnAnnotation()
            .and(cn.taketoday.polaris.ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue"))
            .and(cn.taketoday.polaris.ColumnNameDiscover.forPropertyName());

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobilePhone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("testName");

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo("age");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test1"))).isEqualTo("test-name");

  }

  @Test
  void composite() {
    class Model {
      int testName;

      @MyColumn(myValue = "test-name")
      int test1;
    }

    var nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.composite(
            cn.taketoday.polaris.ColumnNameDiscover.forColumnAnnotation(),
            cn.taketoday.polaris.ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue"),
            cn.taketoday.polaris.ColumnNameDiscover.forPropertyName()
    );

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "mobilePhone"))).isEqualTo("mobilePhone");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "testName"))).isEqualTo("testName");

    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo("age");
    assertThat(nameDiscover.getColumnName(valueOf(Model.class, "test1"))).isEqualTo("test-name");

    // composite null

    nameDiscover = cn.taketoday.polaris.ColumnNameDiscover.composite(
            List.of(cn.taketoday.polaris.ColumnNameDiscover.forColumnAnnotation(),
                    ColumnNameDiscover.forAnnotation(MyColumn.class, "myValue"))
    );
    assertThat(nameDiscover.getColumnName(valueOf(UserModel.class, "age"))).isEqualTo(null);
  }

}