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

package cn.taketoday.polaris.query.parsing.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import cn.taketoday.polaris.logging.Logger;
import cn.taketoday.polaris.logging.LoggerFactory;
import cn.taketoday.polaris.util.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/7/11 11:29
 */
public class XPathParser {

  private static final Logger log = LoggerFactory.getLogger(XPathParser.class);

  private final Document document;

  private final boolean validation;

  @Nullable
  private final EntityResolver entityResolver;

  @Nullable
  private final Properties variables;

  private final XPath xpath;

  public XPathParser(String xml) {
    this(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  public XPathParser(InputStream inputStream) {
    this(inputStream, true, null);
  }

  public XPathParser(InputStream inputStream, boolean validation) {
    this(inputStream, validation, null);
  }

  public XPathParser(InputStream inputStream, boolean validation, @Nullable Properties variables) {
    this(inputStream, validation, variables, null);
  }

  public XPathParser(InputStream inputStream, boolean validation,
          @Nullable Properties variables, @Nullable EntityResolver entityResolver) {
    this.validation = validation;
    this.entityResolver = entityResolver;
    this.variables = variables;
    this.xpath = XPathFactory.newInstance().newXPath();
    this.document = createDocument(new InputSource(inputStream));
  }

  @Nullable
  public String evalString(String expression) {
    return evalString(document, expression);
  }

  @Nullable
  public String evalString(Object root, String expression) {
    return (String) evaluate(expression, root, XPathConstants.STRING);
  }

  @Nullable
  public Boolean evalBoolean(String expression) {
    return evalBoolean(document, expression);
  }

  @Nullable
  public Boolean evalBoolean(Object root, String expression) {
    return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
  }

  @Nullable
  public Short evalShort(String expression) {
    return evalShort(document, expression);
  }

  @Nullable
  public Short evalShort(Object root, String expression) {
    return Short.valueOf(evalString(root, expression));
  }

  public Integer evalInteger(String expression) {
    return evalInteger(document, expression);
  }

  @Nullable
  public Integer evalInteger(Object root, String expression) {
    return Integer.valueOf(evalString(root, expression));
  }

  public Long evalLong(String expression) {
    return evalLong(document, expression);
  }

  public Long evalLong(Object root, String expression) {
    return Long.valueOf(evalString(root, expression));
  }

  public Float evalFloat(String expression) {
    return evalFloat(document, expression);
  }

  public Float evalFloat(Object root, String expression) {
    return Float.valueOf(evalString(root, expression));
  }

  @Nullable
  public Double evalDouble(String expression) {
    return evalDouble(document, expression);
  }

  @Nullable
  public Double evalDouble(Object root, String expression) {
    return (Double) evaluate(expression, root, XPathConstants.NUMBER);
  }

  public List<XNode> evalNodes(String expression) {
    return evalNodes(document, expression);
  }

  public List<XNode> evalNodes(Object root, String expression) {
    List<XNode> xnodes = new ArrayList<>();
    NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
    if (nodes != null) {
      for (int i = 0; i < nodes.getLength(); i++) {
        xnodes.add(new XNode(this, nodes.item(i), variables));
      }
    }
    return xnodes;
  }

  @Nullable
  public XNode evalNode(String expression) {
    return evalNode(document, expression);
  }

  @Nullable
  public XNode evalNode(Object root, String expression) {
    Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
    if (node == null) {
      return null;
    }
    return new XNode(this, node, variables);
  }

  @Nullable
  private Object evaluate(String expression, Object root, QName returnType) {
    try {
      return xpath.evaluate(expression, root, returnType);
    }
    catch (Exception e) {
      throw new XMLParsingException("Error evaluating XPath.", e);
    }
  }

  private Document createDocument(InputSource inputSource) {
    // important: this must only be called AFTER common constructor
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setValidating(validation);

      factory.setNamespaceAware(false);
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(false);
      factory.setCoalescing(false);
      factory.setExpandEntityReferences(true);

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(entityResolver);
      builder.setErrorHandler(new ErrorHandler() {
        @Override
        public void error(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
          log.warn("XML parsing error: {}", exception.getMessage(), exception);
        }
      });
      return builder.parse(inputSource);
    }
    catch (Exception e) {
      throw new XMLParsingException("Error creating document instance.", e);
    }
  }

}