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

package cn.taketoday.polaris.query;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.polaris.jdbc.Query;
import cn.taketoday.polaris.jdbc.RepositoryManager;
import cn.taketoday.polaris.query.parsing.XMLParsingException;
import cn.taketoday.polaris.query.parsing.XNode;
import cn.taketoday.polaris.query.parsing.XPathParser;
import cn.taketoday.polaris.util.Assert;
import cn.taketoday.polaris.util.Nullable;
import cn.taketoday.polaris.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/5/24 16:13
 */
public class MapperFactory {

  private final RepositoryManager repositoryManager;

  public MapperFactory(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> mapperClass) {
    return (T) Proxy.newProxyInstance(mapperClass.getClassLoader(),
            new Class[] { mapperClass }, new MapperHandler(mapperClass));
  }

  class MapperHandler implements InvocationHandler {

    private final Class<?> mapperClass;

    final Map<String, String> sqlMap = new HashMap<>();

    public MapperHandler(Class<?> mapperClass) {
      this.mapperClass = mapperClass;
      var annotation = mapperClass.getAnnotation(MapperLocation.class);
      if (annotation != null) {
        String stringValue = annotation.value();
        InputStream resourceAsStream = mapperClass.getResourceAsStream(stringValue);
        Assert.state(resourceAsStream != null, "Mapper resource not found");
        try (InputStream inputStream = resourceAsStream) {
          XPathParser parser = new XPathParser(inputStream, false);

          List<XNode> sqlNodes = parser.evalNodes("/mapper/sql");
          for (XNode sqlNode : sqlNodes) {
            String id = sqlNode.getStringAttribute("id");
            if (StringUtils.hasText(id)) {
              String sqlBody = StringUtils.trimWhitespace(sqlNode.getStringBody());
              sqlMap.put(id, sqlBody);
            }
          }
        }
        catch (IOException e) {
          throw new XMLParsingException("Mapper resource read failed", e);
        }

      }
      else {
        // TODO
      }
    }

    @Nullable
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      String sql = sqlMap.get(name);
      if (sql != null) {
        Class<?> returnType = method.getReturnType();

        try (Query query = repositoryManager.createQuery(sql)) {
          for (Object arg : args) {
            query.addParameter(arg);
          }

          return query.fetchFirst(returnType);
        }

      }
      return null;
    }

  }

}
