/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.pluginverifier.utils.xml;

import com.jetbrains.pluginverifier.utils.ArrayUtil;
import com.jetbrains.pluginverifier.utils.Comparing;
import org.jdom.*;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * @author mike
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class JDOMUtil {
  private static final ThreadLocal<SoftReference<SAXBuilder>> ourSaxBuilder = new ThreadLocal<SoftReference<SAXBuilder>>();

  private JDOMUtil() { }

  public static boolean areElementsEqual(Element e1, Element e2) {
    if (e1 == null && e2 == null) return true;
    if (e1 == null || e2 == null) return false;

    return Comparing.equal(e1.getName(), e2.getName())
           && attListsEqual(e1.getAttributes(), e2.getAttributes())
           && contentListsEqual(e1.getContent(CONTENT_FILTER), e2.getContent(CONTENT_FILTER));
  }

  private static final EmptyTextFilter CONTENT_FILTER = new EmptyTextFilter();

  private static class EmptyTextFilter implements Filter {
    @Override
    public boolean matches(Object obj) {
      return !(obj instanceof Text) || !((Text)obj).getText().trim().isEmpty();
    }
  }

  private static boolean contentListsEqual(final List c1, final List c2) {
    if (c1 == null && c2 == null) return true;
    if (c1 == null || c2 == null) return false;

    Iterator l1 = c1.listIterator();
    Iterator l2 = c2.listIterator();
    while (l1.hasNext() && l2.hasNext()) {
      if (!contentsEqual((Content)l1.next(), (Content)l2.next())) {
        return false;
      }
    }

    return l1.hasNext() == l2.hasNext();
  }

  private static boolean contentsEqual(Content c1, Content c2) {
    if (!(c1 instanceof Element) && !(c2 instanceof Element)) {
      return c1.getValue().equals(c2.getValue());
    }

    return c1 instanceof Element && c2 instanceof Element && areElementsEqual((Element)c1, (Element)c2);

  }

  private static boolean attListsEqual(@NotNull List a1, @NotNull List a2) {
    if (a1.size() != a2.size()) return false;
    for (int i = 0; i < a1.size(); i++) {
      if (!attEqual((Attribute)a1.get(i), (Attribute)a2.get(i))) return false;
    }
    return true;
  }

  private static boolean attEqual(@NotNull Attribute a1, @NotNull Attribute a2) {
    return a1.getName().equals(a2.getName()) && a1.getValue().equals(a2.getValue());
  }

  @NotNull
  public static Document loadDocument(URL url) throws JDOMException, IOException {
    return loadDocument(URLUtil.openStream(url));
  }

  @NotNull
  public static Document loadDocument(@NotNull InputStream stream) throws JDOMException, IOException {
    InputStreamReader reader = new InputStreamReader(stream, Charset.forName("UTF-8"));
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      saxBuilder.setEntityResolver(new EntityResolver() {
        @Override
        @NotNull
        public InputSource resolveEntity(String publicId, String systemId) {
          return new InputSource(new CharArrayReader(ArrayUtil.EMPTY_CHAR_ARRAY));
        }
      });

      return saxBuilder.build(reader);
    }
    finally {
      reader.close();
    }
  }

  @NotNull
  public static Document loadResourceDocument(URL url) throws JDOMException, IOException {
    return loadDocument(URLUtil.openResourceStream(url));
  }

  public static boolean isEmpty(@NotNull Element element) {
    return element.getAttributes().isEmpty() && element.getContent().isEmpty();
  }
}