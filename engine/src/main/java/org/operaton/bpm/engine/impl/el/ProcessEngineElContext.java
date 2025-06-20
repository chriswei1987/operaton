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
package org.operaton.bpm.engine.impl.el;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;


/**
 * {@link ELContext} used by the process engine.
 *
 * @author Joram Barrez
 * @author Daniel Meyer
 */
public class ProcessEngineElContext extends ELContext {

  protected ELResolver elResolver;

  protected FunctionMapper functionMapper;

  public ProcessEngineElContext(FunctionMapper functionMapper, ELResolver elResolver) {
    this(functionMapper);
    this.elResolver = elResolver;
  }


  public ProcessEngineElContext(FunctionMapper functionMapper) {
    this.functionMapper = functionMapper;
  }

  public ELResolver getELResolver() {
    return elResolver;
  }

  public FunctionMapper getFunctionMapper() {
    return functionMapper;
  }

  public VariableMapper getVariableMapper() {
    return null;
  }

}
