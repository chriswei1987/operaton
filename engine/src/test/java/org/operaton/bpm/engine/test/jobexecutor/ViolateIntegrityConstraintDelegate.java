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
package org.operaton.bpm.engine.test.jobexecutor;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbEntityOperation;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperationType;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;

/**
 * @author Daniel Meyer
 *
 */
public class ViolateIntegrityConstraintDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    String existingId = execution.getId();

    // insert an execution referencing the current execution

    ExecutionEntity newExecution = new ExecutionEntity();
    newExecution.setId("someId");
    newExecution.setParentId(existingId);

    DbEntityOperation insertOperation = new DbEntityOperation();
    insertOperation.setOperationType(DbOperationType.INSERT);
    insertOperation.setEntity(newExecution);

    Context.getCommandContext()
      .getDbSqlSession()
      .executeDbOperation(insertOperation);

  }

}
