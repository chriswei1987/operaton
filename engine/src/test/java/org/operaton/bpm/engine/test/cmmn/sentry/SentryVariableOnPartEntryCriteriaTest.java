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
package org.operaton.bpm.engine.test.cmmn.sentry;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
class SentryVariableOnPartEntryCriteriaTest extends CmmnTest {

  // Basic tests - create, update, delete variable
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSimpleVariableOnPart.cmmn"})
  @Test
  void testVariableCreate() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSimpleVariableOnPart.cmmn"})
  @Test
  void testUnknownVariableCreate() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.setVariable(caseInstanceId, "unknown", "aVariable");
    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testVariableUpdate.cmmn"})
  @Test
  void testVariableUpdate() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    // HumanTask not enabled on variable create
    assertThat(firstHumanTask.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "bVariable");
    firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testVariableDelete.cmmn"})
  @Test
  void testVariableDelete() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.removeVariable(caseInstanceId, "variable_1");
    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    // removing unknown variable would not enable human task
    assertThat(firstHumanTask.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isFalse();

    caseService.removeVariable(caseInstanceId, "variable_1");
    firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isTrue();
  }

  // different variable name and variable event test
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testDifferentVariableName.cmmn"})
  @Test
  void testDifferentVariableName() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    CaseExecution firstHumanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    CaseExecution firstHumanTask2 = queryCaseExecutionByActivityId("HumanTask_2");

    assertThat(firstHumanTask1.isEnabled()).isFalse();
    assertThat(firstHumanTask2.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    firstHumanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask1.isEnabled()).isTrue();

    firstHumanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    // variable_2 is not set
    assertThat(firstHumanTask2.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_2", "aVariable");
    firstHumanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(firstHumanTask2.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testDifferentVariableEvents.cmmn"})
  @Test
  void testDifferentVariableEventsButSameName() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    CaseExecution firstHumanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    CaseExecution firstHumanTask2 = queryCaseExecutionByActivityId("HumanTask_2");

    assertThat(firstHumanTask1.isEnabled()).isFalse();
    assertThat(firstHumanTask2.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    firstHumanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask1.isEnabled()).isTrue();

    firstHumanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    // variable_1 is not updated
    assertThat(firstHumanTask2.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "bVariable");
    firstHumanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(firstHumanTask2.isEnabled()).isTrue();
  }

  // Multiple variableOnParts test
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testMoreVariableOnPart.cmmn"})
  @Test
  void testMultipleVariableOnParts() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    // sentry would not be satisfied as the variable has to updated and deleted as well
    assertThat(firstHumanTask.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "bVariable");
    firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    // sentry would not be satisfied as the variable has to deleted
    assertThat(firstHumanTask.isEnabled()).isFalse();

    caseService.removeVariable(caseInstanceId, "variable_1");
    firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(firstHumanTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testMultipleSentryMultipleVariableOnPart.cmmn"})
  @Test
  void testMultipleSentryMultipleVariableOnParts() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.setVariable(caseInstanceId, "value", 99);
    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("HumanTask_1");
    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    // Sentry1 would not be satisfied as the value has to be > 100
    // Sentry2 would not be satisfied as the humanTask 1 has to completed
    assertThat(secondHumanTask.isEnabled()).isFalse();

    manualStart(firstHumanTask.getId());
    complete(firstHumanTask.getId());

    secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    // Sentry1 would not be satisfied as the value has to be > 100
    // But, Sentry 2 would be satisfied and enables HumanTask2
    assertThat(secondHumanTask.isEnabled()).isTrue();

  }

  // IfPart, OnPart and VariableOnPart combination test
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testOnPartIfPartAndVariableOnPart.cmmn"})
  @Test
  void testOnPartIfPartAndVariableOnPart() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    String firstHumanTaskId = queryCaseExecutionByActivityId("HumanTask_1").getId();

    complete(firstHumanTaskId);

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    // Sentry would not be satisfied as variable_1 is not created and IfPart is not true
    assertThat(secondHumanTask.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "value", 101);
    secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    // Sentry would not be satisfied as variable_1 is not created
    assertThat(secondHumanTask.isEnabled()).isFalse();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");
    secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(secondHumanTask.isEnabled()).isTrue();

  }


  // Variable scope tests
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSimpleVariableScope.cmmn"})
  @Test
  void testVariableCreateScope() {

    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    String firstHumanTaskId = queryCaseExecutionByActivityId("HumanTask_1").getId();

    manualStart(firstHumanTaskId);

    caseService.setVariableLocal(firstHumanTaskId, "variable_1", "aVariable");

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    // Sentry would not be triggered as the scope of the sentry and humanTask1 is different
    assertThat(secondHumanTask.isEnabled()).isFalse();

    caseService.setVariableLocal(secondHumanTask.getId(), "variable_1", "aVariable");
    secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    // Still Sentry would not be triggered as the scope of sentry and the humantask2 is different
    assertThat(secondHumanTask.isEnabled()).isFalse();

    caseService.setVariableLocal(caseInstanceId, "variable_1", "aVariable");
    secondHumanTask = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(secondHumanTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testStageScope.cmmn"})
  @Test
  void testStageScope() {

    caseService.createCaseInstanceByKey("Case_1");

    CaseExecution caseModelHumanTask = queryCaseExecutionByActivityId("CaseModel_HumanTask");
    assertThat(caseModelHumanTask.isEnabled()).isFalse();

    String stageExecutionId = queryCaseExecutionByActivityId("Stage_1").getId();
    // set the variable in the scope of stage such that sentry in the scope of case model does not gets evaluated.
    caseService.setVariableLocal(stageExecutionId, "variable_1", "aVariable");

    CaseExecution stageHumanTask = queryCaseExecutionByActivityId("Stage_HumanTask");
    caseModelHumanTask = queryCaseExecutionByActivityId("CaseModel_HumanTask");
    assertThat(caseModelHumanTask.isEnabled()).isFalse();
    assertThat(stageHumanTask.isEnabled()).isTrue();

    caseService.removeVariable(stageExecutionId, "variable_1");
    // set the variable in the scope of case model that would trigger the sentry outside the scope of the stage
    caseService.setVariable(stageHumanTask.getId(), "variable_1", "aVariable");

    stageHumanTask = queryCaseExecutionByActivityId("Stage_HumanTask");
    caseModelHumanTask = queryCaseExecutionByActivityId("CaseModel_HumanTask");
    assertThat(caseModelHumanTask.isEnabled()).isTrue();
    assertThat(stageHumanTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testStagesScope.cmmn"})
  @Test
  void testStagesScope() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.setVariable(caseInstanceId, "variable_1", "aVariable");

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();

    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();

    CaseExecution humanTask3 = queryCaseExecutionByActivityId("HumanTask_3");
    assertThat(humanTask3.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testStagesScope.cmmn"})
  @Test
  void testStageLocalScope() {
    caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecution1_Id = queryCaseExecutionByActivityId("Stage_1").getId();

    String stageExecution2_Id = queryCaseExecutionByActivityId("Stage_2").getId();

    // variable set to stage 1 scope, so that sentries in stage 2 and in case model should not be triggered
    caseService.setVariableLocal(stageExecution1_Id, "variable_1", "aVariable");

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();

    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isFalse();

    CaseExecution humanTask3 = queryCaseExecutionByActivityId("HumanTask_3");
    assertThat(humanTask3.isEnabled()).isFalse();

    // variable set to stage 2 scope, so that sentries in the scope of case model should not be triggered
    caseService.setVariableLocal(stageExecution2_Id, "variable_1", "aVariable");
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();

    humanTask3 = queryCaseExecutionByActivityId("HumanTask_3");
    assertThat(humanTask3.isEnabled()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testMultipleOnPartsInStage.cmmn"})
  @Test
  void testMultipleOnPartsInStages() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    caseService.setVariable(caseInstanceId, "variable_1", 101);

    CaseExecution humanTask3 = queryCaseExecutionByActivityId("HumanTask_3");
    assertThat(humanTask3.isEnabled()).isTrue();

    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    // Not enabled as the sentry waits for human task 1 to complete
    assertThat(humanTask2.isEnabled()).isFalse();

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    manualStart(humanTask1.getId());
    complete(humanTask1.getId());

    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.sentryEvaluationBeforeCreation.cmmn"})
  @Test
  void testShouldnotEvaluateSentryBeforeSentryCreation() {
    caseService.createCaseInstanceByKey("Case_1").getId();

    CaseExecution stageExecution = queryCaseExecutionByActivityId("Stage_1");
    assertThat(stageExecution.isEnabled()).isTrue();

    CaseExecution humanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask).isNull();

    // set the variable in the scope of stage - should not trigger sentry inside the stage as the sentry is not yet created.
    caseService.setVariableLocal(stageExecution.getId(), "variable_1", "aVariable");

    manualStart(stageExecution.getId());

    humanTask = queryCaseExecutionByActivityId("HumanTask_1");
    // variable event occurred before sentry creation
    assertThat(humanTask.isAvailable()).isTrue();

    caseService.removeVariable(stageExecution.getId(), "variable_1");
    // Sentry is active and would enable human task 1
    caseService.setVariableLocal(stageExecution.getId(), "variable_1", "aVariable");
    humanTask = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask.isEnabled()).isTrue();
  }

  // Evaluation of not affected sentries test
  // i.e: Evaluation of a sentry's ifPart condition even if there are no evaluation of variableOnParts defined in the sentry
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSentryShouldNotBeEvaluatedAfterStageComplete.cmmn"})
  @Test
  void testEvaluationOfNotAffectedSentries() {
    caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecutionId = queryCaseExecutionByActivityId("Stage_1").getId();

    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isAvailable()).isTrue();

    caseService.setVariableLocal(stageExecutionId, "value", 99);
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    // if part is not satisfied
    assertThat(humanTask2.isEnabled()).isFalse();

    caseService.setVariableLocal(stageExecutionId, "value", 101);
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testNotAffectedSentriesInMultipleStageScopes.cmmn"})
  @Test
  void testNotAffectedSentriesInMultipleStageScopes() {
    caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecution1_Id = queryCaseExecutionByActivityId("Stage_1").getId();

    caseService.setVariable(stageExecution1_Id, "value", 99);

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    // if part is not satisfied
    assertThat(humanTask1.isEnabled()).isFalse();

    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    // if part is not satisfied
    assertThat(humanTask2.isEnabled()).isFalse();

    // Evaluates the sentry's IfPart alone
    caseService.setVariable(stageExecution1_Id, "value", 101);
    humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();

    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSameVariableNameInDifferentScopes.cmmn"})
  @Test
  void testSameVariableNameInDifferentScopes() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecution1_Id = queryCaseExecutionByActivityId("Stage_1").getId();

    // inner stage
    String stageExecution2_Id = queryCaseExecutionByActivityId("Stage_2").getId();

    // set the same variable 'value' in the scope of case model
    caseService.setVariable(caseInstanceId, "value", 102);

    // set the variable 'value' in the scope of stage 1
    caseService.setVariableLocal(stageExecution1_Id, "value", 99);

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isAvailable()).isTrue();
    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isAvailable()).isTrue();

    // update the variable 'value' in the case model scope
    caseService.setVariable(caseInstanceId, "value", 102);

    // then sentry of HumanTask 1 gets evaluated and sentry of HumanTask2 is not evaluated.
    humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isFalse();

    // update the variable 'value' in the stage 2/stage 1 scope to evaluate the sentry inside stage 2
    caseService.setVariable(stageExecution2_Id, "value", 103);
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSameVariableNameInDifferentScopes.cmmn"})
  @Test
  void testNestedScopes() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecution1_Id = queryCaseExecutionByActivityId("Stage_1").getId();

    // set the variable 'value' in the scope of the case model
    caseService.setVariable(stageExecution1_Id, "value", 99);

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isAvailable()).isTrue();
    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isAvailable()).isTrue();

    // update the variable 'value' in the case model scope
    caseService.setVariable(caseInstanceId, "value", 102);

    // then sentry of HumanTask 1 and HumanTask 2 gets evaluated.
    humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSameVariableNameInDifferentScopes.cmmn"})
  @Test
  void testNestedScopesWithNullVariableValue() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecution1_Id = queryCaseExecutionByActivityId("Stage_1").getId();

    // set the variable 'value' in the scope of the case model
    caseService.setVariable(caseInstanceId, "value", 99);

    // set the variable 'value' in the scope of the stage 1 with null value
    caseService.setVariableLocal(stageExecution1_Id, "value", null);

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isAvailable()).isTrue();
    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isAvailable()).isTrue();

    // update the variable 'value' in the case model scope
    caseService.setVariable(caseInstanceId, "value", 102);

    // then sentry of HumanTask 1 and HumanTask 2 gets evaluated.
    humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    // Sentry attached to HumanTask 2 is not evaluated because a variable 'value' exists in stage 2 even if the value is null
    assertThat(humanTask2.isEnabled()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testDifferentVariableNameInDifferentScope.cmmn"})
  @Test
  void testNestedScopesOfDifferentVariableNames() {
    String caseInstanceId = caseService.createCaseInstanceByKey("Case_1").getId();

    String stageExecution1_Id = queryCaseExecutionByActivityId("Stage_1").getId();

    // inner stage
    String stageExecution2_Id = queryCaseExecutionByActivityId("Stage_2").getId();

    // set the variable 'value_1' in the scope of the case model
    caseService.setVariable(caseInstanceId, "value_1", 99);
    // set the variable 'value_1' in the scope of the stage 1
    caseService.setVariableLocal(stageExecution1_Id, "value_1", 99);
    // set the variable 'value_2' in the scope of the stage 1
    caseService.setVariableLocal(stageExecution1_Id, "value_2", 99);

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isAvailable()).isTrue();
    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isAvailable()).isTrue();

    // update the variable 'value_1' in the case model scope and stage scope
    caseService.setVariable(caseInstanceId, "value_1", 102);
    caseService.setVariableLocal(stageExecution1_Id, "value_1", 102);

    // then sentry of HumanTask 1 gets evaluated and sentry of HumanTask 2 does not gets evaluated.
    humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isEnabled()).isTrue();
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isFalse();

    caseService.setVariable(stageExecution2_Id, "value_2", 102);
    humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/variableonpart/SentryVariableOnPartEntryCriteriaTest.testSameVariableOnPartAsEntryAndExitCriteria.cmmn"})
  @Test
  void testSameVariableOnPartAsEntryAndExitCriteria() {
    caseService.createCaseInstanceByKey("Case_1").getId();

    CaseExecution stageExecution = queryCaseExecutionByActivityId("Stage_1");

    caseService.setVariable(stageExecution.getId(), "value", 99);

    CaseExecution humanTask = queryCaseExecutionByActivityId("HumanTask_1");
    // exit criteria not satisfied due to the variable 'value' must be greater than 100
    assertThat(humanTask.isEnabled()).isTrue();
    manualStart(humanTask.getId());

    caseService.setVariable(stageExecution.getId(), "value", 101);
    stageExecution = queryCaseExecutionByActivityId("Stage_1");
    assertThat(stageExecution).isNull();
  }
}
