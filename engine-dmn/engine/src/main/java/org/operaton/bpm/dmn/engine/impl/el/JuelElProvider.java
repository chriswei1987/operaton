/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.dmn.engine.impl.el;

import jakarta.el.ELContext;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElExpression;
import org.operaton.bpm.dmn.engine.impl.spi.el.ElProvider;
import org.operaton.bpm.impl.juel.ExpressionFactoryImpl;
import org.operaton.bpm.impl.juel.SimpleContext;
import org.operaton.bpm.impl.juel.TreeValueExpression;
import jakarta.el.*;

/**
 * A simple implementation of {@link ElProvider} using Juel.
 *
 * @author Daniel Meyer
 *
 */
public class JuelElProvider implements ElProvider {

  protected final ExpressionFactoryImpl factory;
  protected final JuelElContextFactory elContextFactory;
  protected final ELContext parsingElContext;

  public JuelElProvider() {
    this(new ExpressionFactoryImpl(), new JuelElContextFactory(createDefaultResolver()));
  }

  public JuelElProvider(ExpressionFactoryImpl expressionFactory, JuelElContextFactory elContextFactory) {
    this.factory = expressionFactory;
    this.elContextFactory = elContextFactory;
    this.parsingElContext = createDefaultParsingElContext();
  }

  protected SimpleContext createDefaultParsingElContext() {
    return new SimpleContext();
  }

  @Override
  public ElExpression createExpression(String expression) {
    TreeValueExpression juelExpr = factory.createValueExpression(parsingElContext, expression, Object.class);
    return new JuelExpression(juelExpr, elContextFactory);
  }

  public ExpressionFactoryImpl getFactory() {
    return factory;
  }

  public JuelElContextFactory getElContextFactory() {
    return elContextFactory;
  }

  public ELContext getParsingElContext() {
    return parsingElContext;
  }

  protected static ELResolver createDefaultResolver() {
    CompositeELResolver resolver = new CompositeELResolver();
    resolver.add(new VariableContextElResolver());
    resolver.add(new ArrayELResolver(true));
    resolver.add(new ListELResolver(true));
    resolver.add(new MapELResolver(true));
    resolver.add(new ResourceBundleELResolver());
    resolver.add(new BeanELResolver());
    return resolver;
  }
}
