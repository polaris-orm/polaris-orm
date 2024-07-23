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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.MethodSignature;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.ClassGenerator;
import cn.taketoday.bytecode.core.CodeEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.bytecode.core.MethodInfo;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.reflect.GeneratorSupport;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link BeanInstantiator} bytecode Generator
 *
 * @author TODAY 2020/9/11 16:51
 */
class BeanInstantiatorGenerator extends GeneratorSupport<ConstructorAccessor> implements ClassGenerator {

  private static final String superType = "Lcn/taketoday/polaris/beans/ConstructorAccessor;";
  
  private static final MethodInfo newInstanceInfo = MethodInfo.from(
          ReflectionUtils.getMethod(ConstructorAccessor.class, "doInstantiate", Object[].class));

  /** @since 4.0 */
  private static final MethodSignature SIG_CONSTRUCTOR
          = new MethodSignature(MethodSignature.CONSTRUCTOR_NAME, "(Ljava/lang/reflect/Constructor;)V");

  private final Constructor<?> targetConstructor;

  public BeanInstantiatorGenerator(Constructor<?> constructor) {
    this(constructor, constructor.getDeclaringClass());
  }

  public BeanInstantiatorGenerator(Constructor<?> constructor, Class<?> targetClass) {
    super(targetClass);
    Assert.notNull(constructor, "constructor is required");
    this.targetConstructor = constructor;
  }

  @Override
  protected int getArgsIndex() {
    return 1;
  }

  /**
   * @since 4.0
   */
  @Override
  protected void generateConstructor(ClassEmitter ce) {
    CodeEmitter e = ce.beginMethod(Opcodes.ACC_PUBLIC, SIG_CONSTRUCTOR);
    e.loadThis();
    e.loadArg(0);
    e.super_invoke_constructor(SIG_CONSTRUCTOR);
    e.returnValue();
    e.end_method();
  }

  /**
   * Fast call bean's {@link java.lang.reflect.Constructor Constructor}
   */
  @Override
  public void generateClass(ClassVisitor visitor) {
    ClassEmitter classEmitter = beginClass(visitor);

    CodeEmitter codeEmitter = EmitUtils.beginMethod(
            classEmitter, newInstanceInfo, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL);

    Type type = Type.forClass(targetClass);
    codeEmitter.newInstance(type);
    codeEmitter.dup();

    prepareParameters(codeEmitter, this.targetConstructor);

    MethodSignature signature = MethodSignature.from(this.targetConstructor);
    codeEmitter.invokeConstructor(type, signature);

    codeEmitter.returnValue();
    codeEmitter.end_method();
    classEmitter.endClass();
  }

  /**
   * @throws NoSuchMethodException handle in fallback {@link #fallback(Exception)}
   * @since 4.0
   */
  @Override
  protected ConstructorAccessor newInstance(Class<ConstructorAccessor> accessorClass) throws NoSuchMethodException {
    Constructor<ConstructorAccessor> constructor = accessorClass.getDeclaredConstructor(Constructor.class);
    return ReflectionUtils.invokeConstructor(constructor, new Object[] { this.targetConstructor });
  }

  @Override
  protected ConstructorAccessor fallback(Exception exception) {
    LoggerFactory.getLogger(BeanInstantiatorGenerator.class)
            .warn("Cannot access a Constructor: [{}], using fallback instance", targetConstructor, exception);
    return super.fallback(exception);
  }

  @Override
  protected ConstructorAccessor fallbackInstance() {
    return BeanInstantiator.forReflective(targetConstructor);
  }

  @Override
  protected boolean cannotAccess() {
    return Modifier.isPrivate(targetClass.getModifiers())
            || Modifier.isPrivate(targetConstructor.getModifiers());
  }

  @Override
  protected ClassGenerator getClassGenerator() {
    return this;
  }

  @Override
  protected Object cacheKey() {
    return targetConstructor;
  }

  @Override
  protected void appendClassName(StringBuilder builder) {
    builder.append('$').append("class"); // 使用 'class' 代替<init>
    buildClassNameSuffix(builder, targetConstructor);
  }

  @Override
  public String getSuperType() {
    return superType;
  }
}
