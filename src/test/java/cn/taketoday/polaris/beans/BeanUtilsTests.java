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

package cn.taketoday.polaris.beans;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import cn.taketoday.polaris.util.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/28 16:43
 */
class BeanUtilsTests {

  @Test
  void newInstanceWithOptionalNullableType() throws NoSuchMethodException {
    Constructor<BeanWithNullableTypes> ctor = BeanWithNullableTypes.class.getDeclaredConstructor(
            Integer.class, Boolean.class, String.class);
    BeanWithNullableTypes bean = BeanUtils.newInstance(ctor, new Object[] { null, null, "foo" });
    assertThat(bean.getCounter()).isNull();
    assertThat(bean.isFlag()).isNull();
    assertThat(bean.getValue()).isEqualTo("foo");
  }

  @Test
  void newInstanceWithFewerArgsThanParameters() throws NoSuchMethodException {
    Constructor<BeanWithPrimitiveTypes> constructor = getBeanWithPrimitiveTypesConstructor();

    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
            BeanUtils.newInstance(constructor, new Object[] { null, null, "foo" }));
  }

  @Test
  void newInstanceWithMoreArgsThanParameters() throws NoSuchMethodException {
    Constructor<BeanWithPrimitiveTypes> constructor = getBeanWithPrimitiveTypesConstructor();

    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() ->
            BeanUtils.newInstance(constructor, new Object[] { null, null, null, null, null, null, null, null, "foo", null }));
  }

  @Test
  @Disabled
  void newInstanceWithOptionalPrimitiveTypes() throws NoSuchMethodException {
    Constructor<BeanWithPrimitiveTypes> constructor = getBeanWithPrimitiveTypesConstructor();

    BeanWithPrimitiveTypes bean = BeanUtils.newInstance(constructor,
            null, null, null, null, null, null, null, null, "foo");

    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(bean.isFlag()).isFalse();
      softly.assertThat(bean.getByteCount()).isEqualTo((byte) 0);
      softly.assertThat(bean.getShortCount()).isEqualTo((short) 0);
      softly.assertThat(bean.getIntCount()).isEqualTo(0);
      softly.assertThat(bean.getLongCount()).isEqualTo(0L);
      softly.assertThat(bean.getFloatCount()).isEqualTo(0F);
      softly.assertThat(bean.getDoubleCount()).isEqualTo(0D);
      softly.assertThat(bean.getCharacter()).isEqualTo('\0');
      softly.assertThat(bean.getText()).isEqualTo("foo");
    });
  }

  private Constructor<BeanWithPrimitiveTypes> getBeanWithPrimitiveTypesConstructor() throws NoSuchMethodException {
    return BeanWithPrimitiveTypes.class.getConstructor(boolean.class, byte.class, short.class, int.class,
            long.class, float.class, double.class, char.class, String.class);
  }

  @Test
  void instantiatePrivateClassWithPrivateConstructor() throws NoSuchMethodException {
    Constructor<PrivateBeanWithPrivateConstructor> ctor = PrivateBeanWithPrivateConstructor.class.getDeclaredConstructor();
    BeanUtils.newInstance(ctor);
  }

  @ParameterizedTest
  @ValueSource(classes = {
          boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class,
          Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
          DayOfWeek.class, String.class, LocalDateTime.class, Date.class, UUID.class, URI.class, URL.class,
          Locale.class, Class.class
  })
  void isSimpleValueType(Class<?> type) {
    assertThat(BeanUtils.isSimpleValueType(type)).as("Type [" + type.getName() + "] should be a simple value type").isTrue();
  }

  @ParameterizedTest
  @ValueSource(classes = { int[].class, Object.class, List.class, void.class, Void.class })
  void isNotSimpleValueType(Class<?> type) {
    assertThat(BeanUtils.isSimpleValueType(type)).as("Type [" + type.getName() + "] should not be a simple value type").isFalse();
  }

  @SuppressWarnings("unused")
  private static class IntegerListHolder1 {

    private List<Integer> list = new ArrayList<>();

    public List<Integer> getList() {
      return list;
    }

    public void setList(List<Integer> list) {
      this.list = list;
    }
  }

  @SuppressWarnings("unused")
  private static class IntegerListHolder2 {

    private List<Integer> list = new ArrayList<>();

    public List<Integer> getList() {
      return list;
    }

    public void setList(List<Integer> list) {
      this.list = list;
    }
  }

  @SuppressWarnings("unused")
  private static class LongListHolder {

    private List<Long> list = new ArrayList<>();

    public List<Long> getList() {
      return list;
    }

    public void setList(List<Long> list) {
      this.list = list;
    }
  }

  @SuppressWarnings("unused")
  private static class NameAndSpecialProperty {

    private String name;

    private int specialProperty;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void setSpecialProperty(int specialProperty) {
      this.specialProperty = specialProperty;
    }

    public int getSpecialProperty() {
      return specialProperty;
    }
  }

  @SuppressWarnings("unused")
  private static class InvalidProperty {

    private String name;

    private String value;

    private boolean flag1;

    private boolean flag2;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public void setValue(int value) {
      this.value = Integer.toString(value);
    }

    public String getValue() {
      return this.value;
    }

    public void setFlag1(boolean flag1) {
      this.flag1 = flag1;
    }

    public Boolean getFlag1() {
      return this.flag1;
    }

    public void setFlag2(Boolean flag2) {
      this.flag2 = flag2;
    }

    public boolean getFlag2() {
      return this.flag2;
    }
  }

  @SuppressWarnings("unused")
  private static class ContainerBean {

    private ContainedBean[] containedBeans;

    public ContainedBean[] getContainedBeans() {
      return containedBeans;
    }

    public void setContainedBeans(ContainedBean[] containedBeans) {
      this.containedBeans = containedBeans;
    }
  }

  @SuppressWarnings("unused")
  private static class ContainedBean {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @SuppressWarnings("unused")
  private static class MethodSignatureBean {

    public void doSomething() {
    }

    public void doSomethingElse(String s, int x) {
    }

    public void overloaded() {
    }

    public void overloaded(String s) {
    }

    public void doSomethingWithAnArray(String[] strings) {
    }

    public void doSomethingWithAMultiDimensionalArray(String[][] strings) {
    }
  }

  private interface MapEntry<K, V> {

    K getKey();

    void setKey(V value);

    V getValue();

    void setValue(V value);
  }

  private static class Bean implements MapEntry<String, String> {

    private String key;

    private String value;

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public void setKey(String aKey) {
      key = aKey;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public void setValue(String aValue) {
      value = aValue;
    }
  }

  private static class BeanWithNullableTypes {

    private final Integer counter;

    private final Boolean flag;

    private final String value;

    @SuppressWarnings("unused")
    public BeanWithNullableTypes(@Nullable Integer counter, @Nullable Boolean flag, String value) {
      this.counter = counter;
      this.flag = flag;
      this.value = value;
    }

    @Nullable
    public Integer getCounter() {
      return counter;
    }

    @Nullable
    public Boolean isFlag() {
      return flag;
    }

    public String getValue() {
      return value;
    }
  }

  private static class BeanWithPrimitiveTypes {

    private final boolean flag;
    private final byte byteCount;
    private final short shortCount;
    private final int intCount;
    private final long longCount;
    private final float floatCount;
    private final double doubleCount;
    private final char character;
    private final String text;

    @SuppressWarnings("unused")
    public BeanWithPrimitiveTypes(boolean flag, byte byteCount, short shortCount, int intCount, long longCount,
            float floatCount, double doubleCount, char character, String text) {

      this.flag = flag;
      this.byteCount = byteCount;
      this.shortCount = shortCount;
      this.intCount = intCount;
      this.longCount = longCount;
      this.floatCount = floatCount;
      this.doubleCount = doubleCount;
      this.character = character;
      this.text = text;
    }

    public boolean isFlag() {
      return flag;
    }

    public byte getByteCount() {
      return byteCount;
    }

    public short getShortCount() {
      return shortCount;
    }

    public int getIntCount() {
      return intCount;
    }

    public long getLongCount() {
      return longCount;
    }

    public float getFloatCount() {
      return floatCount;
    }

    public double getDoubleCount() {
      return doubleCount;
    }

    public char getCharacter() {
      return character;
    }

    public String getText() {
      return text;
    }

  }

  private static class PrivateBeanWithPrivateConstructor {

    private PrivateBeanWithPrivateConstructor() {
    }
  }

  @SuppressWarnings("unused")
  private static class Order {

    private String id;
    private List<String> lineItems;

    Order() {
    }

    Order(String id, List<String> lineItems) {
      this.id = id;
      this.lineItems = lineItems;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public List<String> getLineItems() {
      return this.lineItems;
    }

    public void setLineItems(List<String> lineItems) {
      this.lineItems = lineItems;
    }

    @Override
    public String toString() {
      return "Order [id=" + this.id + ", lineItems=" + this.lineItems + "]";
    }
  }

  private interface OrderSummary {

    String getId();

    List<String> getLineItems();
  }

  private static class OrderInvocationHandler implements InvocationHandler {

    private final Order order;

    OrderInvocationHandler(Order order) {
      this.order = order;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      try {
        // Ignore args since OrderSummary doesn't declare any methods with arguments,
        // and we're not supporting equals(Object), etc.
        return Order.class.getDeclaredMethod(method.getName()).invoke(this.order);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }
  }

  private static class GenericBaseModel<T> {

    private T id;

    private String name;

    public T getId() {
      return id;
    }

    public void setId(T id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class User extends GenericBaseModel<Integer> {

    private String address;

    public User() {
      super();
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }

}