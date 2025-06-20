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
package org.operaton.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.instance.Definitions;
import org.operaton.bpm.model.bpmn.instance.Process;
import org.operaton.bpm.model.bpmn.instance.StartEvent;
import org.operaton.bpm.model.bpmn.instance.UserTask;

class GenerateIdTest {

  @Test
  void shouldNotGenerateIdsOnRead() {
    BpmnModelInstance modelInstance = Bpmn.readModelFromStream(GenerateIdTest.class.getResourceAsStream("GenerateIdTest.bpmn"));
    Definitions definitions = modelInstance.getDefinitions();
    assertThat(definitions.getId()).isNull();

    Process process = modelInstance.getModelElementsByType(Process.class).iterator().next();
    assertThat(process.getId()).isNull();

    StartEvent startEvent = modelInstance.getModelElementsByType(StartEvent.class).iterator().next();
    assertThat(startEvent.getId()).isNull();

    UserTask userTask = modelInstance.getModelElementsByType(UserTask.class).iterator().next();
    assertThat(userTask.getId()).isNull();
  }

  @Test
  void shouldGenerateIdsOnCreate() {
    BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
    Definitions definitions = modelInstance.newInstance(Definitions.class);
    assertThat(definitions.getId()).isNotNull();

    Process process = modelInstance.newInstance(Process.class);
    assertThat(process.getId()).isNotNull();

    StartEvent startEvent = modelInstance.newInstance(StartEvent.class);
    assertThat(startEvent.getId()).isNotNull();

    UserTask userTask = modelInstance.newInstance(UserTask.class);
    assertThat(userTask.getId()).isNotNull();
  }

}
