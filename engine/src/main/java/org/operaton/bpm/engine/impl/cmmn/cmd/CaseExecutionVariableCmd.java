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
package org.operaton.bpm.engine.impl.cmmn.cmd;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.operaton.bpm.engine.exception.cmmn.CaseExecutionNotFoundException;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.cmmn.CaseExecutionCommandBuilderImpl;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;

/**
 * @author Roman Smirnov
 *
 */
public class CaseExecutionVariableCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String caseExecutionId;
  protected Map<String, Object> variables;
  protected Map<String, Object> variablesLocal;

  protected Collection<String> variablesDeletions;
  protected Collection<String> variablesLocalDeletions;

  protected CaseExecutionEntity caseExecution;

  public CaseExecutionVariableCmd(String caseExecutionId, Map<String, Object> variables, Map<String, Object> variablesLocal,
      Collection<String> variablesDeletions, Collection<String> variablesLocalDeletions) {
    this.caseExecutionId = caseExecutionId;
    this.variables = variables;
    this.variablesLocal = variablesLocal;
    this.variablesDeletions = variablesDeletions;
    this.variablesLocalDeletions = variablesLocalDeletions;

  }

  public CaseExecutionVariableCmd(CaseExecutionCommandBuilderImpl builder) {
    this(builder.getCaseExecutionId(), builder.getVariables(), builder.getVariablesLocal(),
         builder.getVariableDeletions(), builder.getVariableLocalDeletions());
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotNull("caseExecutionId", caseExecutionId);

    caseExecution = commandContext
      .getCaseExecutionManager()
      .findCaseExecutionById(caseExecutionId);

    ensureNotNull(CaseExecutionNotFoundException.class, "There does not exist any case execution with id: '" + caseExecutionId + "'", "caseExecution", caseExecution);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkUpdateCaseInstance(caseExecution);
    }

    if (variablesDeletions != null && !variablesDeletions.isEmpty()) {
      caseExecution.removeVariables(variablesDeletions);
    }

    if (variablesLocalDeletions != null && !variablesLocalDeletions.isEmpty()) {
      caseExecution.removeVariablesLocal(variablesLocalDeletions);
    }

    if (variables != null && !variables.isEmpty()) {
      caseExecution.setVariables(variables);
    }

    if (variablesLocal != null && !variablesLocal.isEmpty()) {
      caseExecution.setVariablesLocal(variablesLocal);
    }

    return null;
  }

  public CaseExecutionEntity getCaseExecution() {
    return caseExecution;
  }

}
