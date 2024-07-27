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

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/7/26 22:12
 */
public interface Constant {

  /** The package separator character: {@code '.'}. */
  char PACKAGE_SEPARATOR = '.';

  /** The path separator character: {@code '/'}. */
  char PATH_SEPARATOR = '/';

  int[] EMPTY_INT_ARRAY = {};

  String[] EMPTY_STRING_ARRAY = {};

  byte[] EMPTY_BYTES = {};
  File[] EMPTY_FILES = {};
  Field[] EMPTY_FIELDS = {};
  Method[] EMPTY_METHODS = {};
  Object[] EMPTY_OBJECTS = {};
  Class<?>[] EMPTY_CLASSES = {};
  Annotation[] EMPTY_ANNOTATIONS = {};

  /**
   * Constant defining a value for no default - as a replacement for
   * {@code null} which we cannot use in annotation attributes.
   * <p>This is an artificial arrangement of 16 unicode characters,
   * with its sole purpose being to never match user-declared values.
   */
  String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
