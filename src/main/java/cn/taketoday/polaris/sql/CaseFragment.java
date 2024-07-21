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


package cn.taketoday.polaris.sql;

import java.util.LinkedHashMap;

/**
 * Abstract SQL case fragment renderer
 *
 * @author Gavin King, Simon Harris
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0
 */
public abstract class CaseFragment {

  public abstract String toFragmentString();

  protected String returnColumnName;

  protected LinkedHashMap<String, String> cases = new LinkedHashMap<>();

  public CaseFragment setReturnColumnName(String returnColumnName) {
    this.returnColumnName = returnColumnName;
    return this;
  }

  public CaseFragment setReturnColumnName(String returnColumnName, String suffix) {
    return setReturnColumnName(new Alias(suffix).toAliasString(returnColumnName));
  }

  public CaseFragment addWhenColumnNotNull(String alias, String columnName, String value) {
    cases.put(qualify(alias, columnName), value);
    return this;
  }

  public static String qualify(String prefix, String name) {
    if (name == null || prefix == null) {
      throw new NullPointerException("prefix or name were null attempting to build qualified name");
    }
    return prefix + '.' + name;
  }
}
