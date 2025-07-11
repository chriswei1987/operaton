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
package org.operaton.bpm.engine.test.concurrency.partitioning;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;

/**
 * @author Tassilo Weidner
 */

public class CompetingHistoricByteArrayPartitioningTest extends AbstractPartitioningTest {

  static final String VARIABLE_NAME = "aVariableName";
  static final String VARIABLE_VALUE = "aVariableValue";
  static final String ANOTHER_VARIABLE_VALUE = "anotherVariableValue";

  @Test
  public void shouldSuppressOleOnConcurrentFetchAndDelete() {
    // given
    final String processInstanceId = deployAndStartProcess(PROCESS_WITH_USERTASK,
      Variables.createVariables().putValue(VARIABLE_NAME,
        Variables.byteArrayValue(VARIABLE_VALUE.getBytes())))
      .getId();

    final String[] historicByteArrayId = new String[1];
    commandExecutor.execute((Command<Void>) commandContext -> {

      ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(processInstanceId);

      VariableInstanceEntity varInstance = (VariableInstanceEntity) execution.getVariableInstance(VARIABLE_NAME);
      HistoricVariableInstanceEntity historicVariableInstance = commandContext.getHistoricVariableInstanceManager()
        .findHistoricVariableInstanceByVariableInstanceId(varInstance.getId());

      historicByteArrayId[0] = historicVariableInstance.getByteArrayValueId();

      return null;
    });

    ThreadControl asyncThread = executeControllableCommand(new AsyncThread(processInstanceId, historicByteArrayId[0]));
    asyncThread.reportInterrupts();
    asyncThread.waitForSync();

    commandExecutor.execute((Command<Void>) commandContext -> {

      commandContext.getByteArrayManager()
        .deleteByteArrayById(historicByteArrayId[0]);

      return null;
    });

    commandExecutor.execute((Command<Void>) commandContext -> {

      // assume
      assertThat(commandContext.getDbEntityManager().selectById(ByteArrayEntity.class, historicByteArrayId[0])).isNull();

      return null;
    });

    // when
    asyncThread.makeContinue();
    asyncThread.waitUntilDone();

    // then
    assertThat(runtimeService.createVariableInstanceQuery().singleResult().getName()).isEqualTo(VARIABLE_NAME);
    assertThat(new String((byte[]) runtimeService.createVariableInstanceQuery().singleResult().getValue())).isEqualTo(ANOTHER_VARIABLE_VALUE);
  }

  public class AsyncThread extends ControllableCommand<Void> {

    String processInstanceId;
    String historicByteArrayId;

    AsyncThread(String processInstanceId, String historicByteArrayId) {
      this.processInstanceId = processInstanceId;
      this.historicByteArrayId = historicByteArrayId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      commandContext.getDbEntityManager()
        .selectById(ByteArrayEntity.class, historicByteArrayId); // cache

      monitor.sync();

      runtimeService.setVariable(processInstanceId, VARIABLE_NAME,
        Variables.byteArrayValue(ANOTHER_VARIABLE_VALUE.getBytes()));

      return null;
    }

  }

}
