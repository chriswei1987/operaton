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
package org.operaton.bpm.engine.test.api.runtime.util;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

/**
 * @author Daniel Meyer
 *
 */
public class ChangeVariablePropertyDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {

    execution.setVariableLocal("var", new SimpleSerializableBean());

    SimpleSerializableBean variable = (SimpleSerializableBean) execution.getVariable("var");
    variable.setIntProperty(variable.getIntProperty() + 1);

    boolean shouldExplicitlyUpdateVariable = (Boolean) execution.getVariable("shouldExplicitlyUpdateVariable");

    if (shouldExplicitlyUpdateVariable) {
      execution.setVariableLocal("var", variable);
    }

  }

}
