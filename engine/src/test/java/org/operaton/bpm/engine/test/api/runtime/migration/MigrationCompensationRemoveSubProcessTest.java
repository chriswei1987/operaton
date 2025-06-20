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
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.assertThat;
import static org.operaton.bpm.engine.test.util.ActivityInstanceAssert.describeActivityInstanceTree;
import static org.operaton.bpm.engine.test.util.ExecutionAssert.describeExecutionTree;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.CompensationModels;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Thorben Lindhauer
 *
 */
class MigrationCompensationRemoveSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension rule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  MigrationTestExtension testHelper = new MigrationTestExtension(rule);

  @BeforeEach
  @AfterEach
  void clearExecutionListener() {
    RecorderExecutionListener.clear();
  }

  @Test
  void testCase1() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "compensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCase1AssertActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when
    testHelper.completeTask("userTask2");

    // then
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .activity("compensationHandler")
      .done());
  }

  @Test
  void testCase1AssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask2").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
        .done());
  }

  @Test
  void testCase2() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertEventSubscriptionMigrated("compensationHandler", "compensationHandler", null);

    // and the compensation can be triggered and completed
    testHelper.completeTask("userTask2");
    testHelper.completeTask("compensationHandler");

    testHelper.assertProcessEnded(processInstance.getId());
  }

  @Test
  void testCase2ActivityInstance() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when
    testHelper.completeTask("userTask2");

    // then
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .activity("compensationHandler")
      .done());
  }

  @Test
  void testCase2AssertExecutionTree() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    testHelper.assertExecutionTreeAfterMigration()
      .hasProcessDefinitionId(targetProcessDefinition.getId())
      .matches(
        describeExecutionTree("userTask2").scope().id(testHelper.snapshotBeforeMigration.getProcessInstanceId())
        .done());
  }

  @Test
  void testCanOnlyTriggerCompensationInParentOfRemovedScope() {

    BpmnModelInstance sourceModel = ProcessModels.newModel()
      .startEvent()
      .subProcess("outerSubProcess")
        .embeddedSubProcess()
        .startEvent()
        .userTask("userTask1")
          .boundaryEvent("compensationBoundary")
          .compensateEventDefinition()
          .compensateEventDefinitionDone()
        .moveToActivity("userTask1")
        .subProcess("innerSubProcess")
          .embeddedSubProcess()
          .startEvent()
          .userTask("userTask2")
          .endEvent()
        .subProcessDone()
        .endEvent()
      .subProcessDone()
      .done();
    CompensationModels.addUserTaskCompensationHandler(sourceModel, "compensationBoundary", "compensationHandler");

    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(sourceModel);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_TWO_TASKS_SUBPROCESS_MODEL)
      .endEventBuilder("subProcessEnd")
        .compensateEventDefinition()
        .waitForCompletion(true)
        .compensateEventDefinitionDone()
      .done());

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("innerSubProcess", "subProcess")
      .mapActivities("userTask2", "userTask2")
      .mapActivities("compensationBoundary", "compensationBoundary")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // when
    testHelper.completeTask("userTask2");

    // then compensation is not triggered from inside the inner sub process
    // but only on process definition level
    ActivityInstance activityInstance = rule.getRuntimeService().getActivityInstance(processInstance.getId());

    assertThat(activityInstance).hasStructure(
      describeActivityInstanceTree(targetProcessDefinition.getId())
        .activity("compensationEvent")
        .beginScope("subProcess")
          .activity("compensationHandler")
      .done());
  }

  @Test
  void testCanRemoveEventScopeWithVariables() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Execution subProcessExecution = rule.getRuntimeService()
        .createExecutionQuery()
        .activityId("userTask1")
        .singleResult();
    rule.getRuntimeService().setVariableLocal(subProcessExecution.getId(), "foo", "bar");

    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(rule.getRuntimeService().createVariableInstanceQuery().count()).isZero();
  }

  @Test
  void testDeletesOnlyVariablesFromRemovingScope() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.DOUBLE_SUBPROCESS_MODEL);
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
      .mapActivities("innerSubProcess", "subProcess")
      .mapActivities("userTask2", "userTask2")
      .mapActivities("compensationBoundary", "compensationBoundary")
      .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());

    Execution innerSubProcessExecution = rule.getRuntimeService()
      .createExecutionQuery()
      .activityId("userTask1")
      .singleResult();

    String outerSubProcessExecutionId = ((ExecutionEntity) innerSubProcessExecution).getParentId();

    rule.getRuntimeService().setVariableLocal(outerSubProcessExecutionId, "outerVariable", "outerValue");
    rule.getRuntimeService().setVariableLocal(innerSubProcessExecution.getId(), "innerVariable", "innerValue");

    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(1);

    VariableInstance migratedVariable = testHelper.snapshotAfterMigration.getSingleVariable("innerVariable");
    assertThat(migratedVariable).isNotNull();
    assertThat(migratedVariable.getValue()).isEqualTo("innerValue");
  }

  @Test
  void testNoListenersCalled() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
        .activityBuilder("subProcess")
        .operatonExecutionListenerClass(
            ExecutionListener.EVENTNAME_END,
            RecorderExecutionListener.class.getName())
        .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    testHelper.completeTask("userTask1");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then
    // the listener was only called once when the sub process completed properly
    assertThat(RecorderExecutionListener.getRecordedEvents()).hasSize(1);
  }

  @Test
  void testNoOutputMappingExecuted() {
    // given
    ProcessDefinition sourceProcessDefinition = testHelper.deployAndGetDefinition(modify(CompensationModels.COMPENSATION_ONE_TASK_SUBPROCESS_MODEL)
      .activityBuilder("subProcess")
        .operatonOutputParameter("foo", "${bar}")
      .done());
    ProcessDefinition targetProcessDefinition = testHelper.deployAndGetDefinition(CompensationModels.ONE_COMPENSATION_TASK_MODEL);

    MigrationPlan migrationPlan = rule.getRuntimeService().createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapActivities("userTask2", "userTask2")
        .mapActivities("compensationBoundary", "compensationBoundary")
        .build();

    ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceById(sourceProcessDefinition.getId());
    rule.getRuntimeService().setVariable(processInstance.getId(), "bar", "value1");
    testHelper.completeTask("userTask1"); // => sets "foo" to "value1"

    rule.getRuntimeService().setVariable(processInstance.getId(), "bar", "value2");

    // when
    testHelper.migrateProcessInstance(migrationPlan, processInstance);

    // then "foo" has not been set to "value2"
    assertThat(testHelper.snapshotAfterMigration.getVariables()).hasSize(2); // "foo" and "bar"
    VariableInstance variableInstance = testHelper.snapshotAfterMigration.getSingleVariable("foo");
    assertThat(variableInstance.getValue()).isEqualTo("value1");
  }
}
