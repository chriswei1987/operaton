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
package org.operaton.bpm.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.SignalCatchModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationSignalCatchEventTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @Test
  void testMigrateEventSubscription() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("signalCatch", "signalCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionMigrated("signalCatch", "signalCatch", SignalCatchModels.SIGNAL_NAME);

    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child("signalCatch").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("signalCatch"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("signalCatch", testHelper.getSingleActivityInstanceBeforeMigration("signalCatch").getId())
      .done());

    // and it is possible to trigger the event
    rule.getRuntimeService().signalEventReceived(SignalCatchModels.SIGNAL_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionChangeActivityId() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS)
        .changeElementId("signalCatch", "newSignalCatch"));

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("signalCatch", "newSignalCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionMigrated("signalCatch", "newSignalCatch", SignalCatchModels.SIGNAL_NAME);

    // and it is possible to trigger the event
    rule.getRuntimeService().signalEventReceived(SignalCatchModels.SIGNAL_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionPreserveSignalName() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
        .startEvent()
        .intermediateCatchEvent("signalCatch")
          .signal("new" + SignalCatchModels.SIGNAL_NAME)
        .userTask("userTask")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("signalCatch", "signalCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then the signal name of the event subscription has not changed
    testHelper.assertEventSubscriptionMigrated("signalCatch", "signalCatch", SignalCatchModels.SIGNAL_NAME);

    // and it is possible to trigger the event
    rule.getRuntimeService().signalEventReceived(SignalCatchModels.SIGNAL_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionUpdateSignalName() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
        .startEvent()
        .intermediateCatchEvent("signalCatch")
          .signal("new" + SignalCatchModels.SIGNAL_NAME)
        .userTask("userTask")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("signalCatch", "signalCatch")
          .updateEventTrigger()
        .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then the message event subscription's event name has not changed
    testHelper.assertEventSubscriptionMigrated(
        "signalCatch", SignalCatchModels.SIGNAL_NAME,
        "signalCatch", "new" + SignalCatchModels.SIGNAL_NAME);

    // and it is possible to trigger the event
    rule.getRuntimeService().signalEventReceived("new" + SignalCatchModels.SIGNAL_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateJobAddParentScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.SUBPROCESS_SIGNAL_CATCH_PROCESS);

    MigrationPlan migrationPlan = rule.getRuntimeService()
      .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("signalCatch", "signalCatch")
      .build();

    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan);

    // then
    testHelper.assertEventSubscriptionMigrated("signalCatch", "signalCatch", SignalCatchModels.SIGNAL_NAME);

    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree(null).scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
          .child(null).scope()
            .child("signalCatch").scope().id(testHelper.getSingleExecutionIdForActivityBeforeMigration("signalCatch"))
        .done());

    testHelper.assertActivityTreeAfterMigration().hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .beginScope("subProcess")
          .activity("signalCatch", testHelper.getSingleActivityInstanceBeforeMigration("signalCatch").getId())
      .done());

    // and it is possible to trigger the event
    rule.getRuntimeService().signalEventReceived(SignalCatchModels.SIGNAL_NAME);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testMigrateEventSubscriptionUpdateSignalExpressionNameWithVariables() {
    // given
    String newSignalName = "new" + SignalCatchModels.SIGNAL_NAME + "-${var}";
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(SignalCatchModels.ONE_SIGNAL_CATCH_PROCESS);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(ProcessModels.newModel()
        .startEvent()
        .intermediateCatchEvent("signalCatch")
        .signal(newSignalName)
        .userTask("userTask")
        .endEvent()
        .done());

    MigrationPlan migrationPlan = rule.getRuntimeService()
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("signalCatch", "signalCatch")
        .updateEventTrigger()
        .build();

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("var", "foo");


    // when
    ProcessInstance processInstance = testHelper.createProcessInstanceAndMigrate(migrationPlan, variables);

    // then there should be a variable
    VariableInstance beforeMigration = testHelper.snapshotBeforeMigration.getSingleVariable("var");
    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);
    testHelper.assertVariableMigratedToExecution(beforeMigration, beforeMigration.getExecutionId());

    // and the signal event subscription's event name has changed
    String resolvedSignalName = "new" + SignalCatchModels.SIGNAL_NAME + "-foo";
    testHelper.assertEventSubscriptionMigrated(
        "signalCatch", SignalCatchModels.SIGNAL_NAME,
        "signalCatch", resolvedSignalName);

    // and it is possible to trigger the event and complete the task afterwards
    rule.getRuntimeService().signalEventReceived(resolvedSignalName);

    testHelper.completeTask("userTask");
    testHelper.assertProcessEnded(processInstance.getId());
  }

}
