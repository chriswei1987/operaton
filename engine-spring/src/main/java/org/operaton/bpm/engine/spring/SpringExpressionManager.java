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
package org.operaton.bpm.engine.spring;

import java.util.Map;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.el.JuelExpressionManager;
import org.operaton.bpm.engine.impl.el.ReadOnlyMapELResolver;
import org.operaton.bpm.engine.impl.el.VariableContextElResolver;
import org.operaton.bpm.engine.impl.el.VariableScopeElResolver;
import org.operaton.bpm.engine.impl.mock.MockElResolver;
import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELResolver;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import org.springframework.context.ApplicationContext;


/**
 * {@link ExpressionManager} that exposes the full application-context or a limited set
 * of beans in expressions.
 *
 * @author Tom Baeyens
 */
public class SpringExpressionManager extends JuelExpressionManager {

  protected ApplicationContext applicationContext;

  /**
   * @param applicationContext
   *          the applicationContext to use. Ignored when 'beans' parameter is
   *          not null.
   * @param beans
   *          a map of custom beans to expose. If null, all beans in the
   *          application-context will be exposed.
   */
  public SpringExpressionManager(ApplicationContext applicationContext, Map<Object, Object> beans) {
    super(beans);
    this.applicationContext = applicationContext;
  }

  /**
   * @param applicationContext
   *          the applicationContext to use.
   * @see #SpringExpressionManager(ApplicationContext, Map)
   */
  public SpringExpressionManager(ApplicationContext applicationContext) {
    this(applicationContext, null);
  }

  @Override
  protected ELResolver createElResolver() {
    CompositeELResolver compositeElResolver = new CompositeELResolver();
    compositeElResolver.add(new VariableScopeElResolver());
    compositeElResolver.add(new VariableContextElResolver());
    compositeElResolver.add(new MockElResolver());

    if(beans != null) {
      // Only expose limited set of beans in expressions
      compositeElResolver.add(new ReadOnlyMapELResolver(beans));
    } else {
      // Expose full application-context in expressions
      compositeElResolver.add(new ApplicationContextElResolver(applicationContext));
    }

    compositeElResolver.add(new ArrayELResolver());
    compositeElResolver.add(new ListELResolver());
    compositeElResolver.add(new MapELResolver());
    compositeElResolver.add(new BeanELResolver());

    return compositeElResolver;
  }


}
