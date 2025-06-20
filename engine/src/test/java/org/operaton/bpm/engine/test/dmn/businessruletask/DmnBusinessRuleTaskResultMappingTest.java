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
package org.operaton.bpm.engine.test.dmn.businessruletask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * Tests the mapping of the decision result.
 *
 * @author Philipp Ossler
 */
class DmnBusinessRuleTaskResultMappingTest {

  protected static final String TEST_DECISION = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.dmn11.xml";
  protected static final String CUSTOM_MAPPING_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testCustomOutputMapping.bpmn20.xml";
  protected static final String SINGLE_ENTRY_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testSingleEntry.bpmn20.xml";
  protected static final String SINGLE_RESULT_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testSingleResult.bpmn20.xml";
  protected static final String COLLECT_ENTRIES_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testCollectEntries.bpmn20.xml";
  protected static final String RESULT_LIST_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testResultList.bpmn20.xml";
  protected static final String DEFAULT_MAPPING_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testDefaultMapping.bpmn20.xml";
  protected static final String INVALID_MAPPING_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testInvalidMapping.bpmn20.xml";
  protected static final String OVERRIDE_DECISION_RESULT_BPMN = "org/operaton/bpm/engine/test/dmn/result/DmnBusinessRuleTaskResultMappingTest.testOverrideVariable.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  HistoryService historyService;

  @Deployment(resources = {CUSTOM_MAPPING_BPMN, TEST_DECISION})
  @Test
  void testCustomOutputMapping() {
    ProcessInstance processInstance = startTestProcess("multiple entries");

    assertThat(runtimeService.getVariable(processInstance.getId(), "result1")).isEqualTo("foo");
    assertThat(runtimeService.<StringValue>getVariableTyped(processInstance.getId(), "result1")).isEqualTo(Variables.stringValue("foo"));

    assertThat(runtimeService.getVariable(processInstance.getId(), "result2")).isEqualTo("bar");
    assertThat(runtimeService.<StringValue>getVariableTyped(processInstance.getId(), "result2")).isEqualTo(Variables.stringValue("bar"));
  }

  @Deployment(resources = {SINGLE_ENTRY_BPMN, TEST_DECISION})
  @Test
  void testSingleEntryMapping() {
    ProcessInstance processInstance = startTestProcess("single entry");

    assertThat(runtimeService.getVariable(processInstance.getId(), "result")).isEqualTo("foo");
    assertThat(runtimeService.<StringValue>getVariableTyped(processInstance.getId(), "result")).isEqualTo(Variables.stringValue("foo"));
  }

  @Deployment(resources = {SINGLE_RESULT_BPMN, TEST_DECISION})
  @Test
  void testSingleResultMapping() {
    ProcessInstance processInstance = startTestProcess("multiple entries");

    @SuppressWarnings("unchecked")
    Map<String, Object> output = (Map<String, Object>) runtimeService.getVariable(processInstance.getId(), "result");

    assertThat(output)
            .hasSize(2)
            .containsEntry("result1", "foo")
            .containsEntry("result2", "bar");
  }

  @Deployment(resources = {COLLECT_ENTRIES_BPMN, TEST_DECISION})
  @Test
  void testCollectEntriesMapping() {
    ProcessInstance processInstance = startTestProcess("single entry list");

    @SuppressWarnings("unchecked")
    List<String> output = (List<String>) runtimeService.getVariable(processInstance.getId(), "result");

    assertThat(output).hasSize(2);
    assertThat(output.get(0)).isEqualTo("foo");
    assertThat(output.get(1)).isEqualTo("foo");
  }

  @Deployment(resources = {RESULT_LIST_BPMN, TEST_DECISION})
  @Test
  void testResultListMapping() {
    ProcessInstance processInstance = startTestProcess("multiple entries list");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> resultList = (List<Map<String, Object>>) runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(resultList).hasSize(2);

    for (Map<String, Object> valueMap : resultList) {
      assertThat(valueMap)
              .hasSize(2)
              .containsEntry("result1", "foo")
              .containsEntry("result2", "bar");
    }
  }

  @Deployment(resources = {DEFAULT_MAPPING_BPMN, TEST_DECISION})
  @Test
  void testDefaultResultMapping() {
    ProcessInstance processInstance = startTestProcess("multiple entries list");

    // default mapping is 'resultList'
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> resultList = (List<Map<String, Object>>) runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(resultList).hasSize(2);

    for (Map<String, Object> valueMap : resultList) {
      assertThat(valueMap)
              .hasSize(2)
              .containsEntry("result1", "foo")
              .containsEntry("result2", "bar");
    }
  }

  @Deployment(resources = {SINGLE_ENTRY_BPMN, TEST_DECISION})
  @Test
  void testSingleEntryMappingFailureMultipleOutputs() {
    try {
      startTestProcess("single entry list");

      fail("expect exception");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("ENGINE-22001", e.getMessage());
    }
  }

  @Deployment(resources = {SINGLE_ENTRY_BPMN, TEST_DECISION})
  @Test
  void testSingleEntryMappingFailureMultipleValues() {
    try {
      startTestProcess("multiple entries");

      fail("expect exception");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("ENGINE-22001", e.getMessage());
    }
  }

  @Deployment(resources = {SINGLE_RESULT_BPMN, TEST_DECISION})
  @Test
  void testSingleResultMappingFailure() {
    try {
      startTestProcess("single entry list");

      fail("expect exception");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("ENGINE-22001", e.getMessage());
    }
  }

  @Deployment(resources = {COLLECT_ENTRIES_BPMN, TEST_DECISION})
  @Test
  void testCollectEntriesMappingFailure() {
    try {
      startTestProcess("multiple entries");

      fail("expect exception");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("ENGINE-22002", e.getMessage());
    }
  }

  @Test
  void testInvalidMapping() {
    var deploymentBuilder = repositoryService
          .createDeployment()
          .addClasspathResource(INVALID_MAPPING_BPMN);
    try {
      testRule.deploy(deploymentBuilder);

      fail("expect parse exception");
    } catch (ParseException e) {
      testRule.assertTextPresent("No decision result mapper found for name 'invalid'", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors()).hasSize(1);
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("ruleTask");
    }
  }

  @Deployment(resources = {DEFAULT_MAPPING_BPMN, TEST_DECISION})
  @Test
  void testTransientDecisionResult() {
    // when a decision is evaluated and the result is stored in a transient variable "decisionResult"
    ProcessInstance processInstance = startTestProcess("single entry");

    // then the variable should not be available outside the business rule task
    assertThat(runtimeService.getVariable(processInstance.getId(), "decisionResult")).isNull();
    // and should not create an entry in history since it is not persistent
    assertThat(historyService.createHistoricVariableInstanceQuery().variableName("decisionResult").singleResult()).isNull();
  }

  @Deployment(resources = {OVERRIDE_DECISION_RESULT_BPMN, TEST_DECISION})
  @Test
  void testFailedToOverrideDecisionResultVariable() {
    try {
      // the transient variable "decisionResult" should not be overridden by the task result variable
      startTestProcess("single entry");
      fail("expect exception");

    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("transient variable with name 'decisionResult' to non-transient", e.getMessage());
    }
  }

  @Deployment(resources = {SINGLE_ENTRY_BPMN, TEST_DECISION})
  @Test
  void testSingleEntryEmptyResult() {
    ProcessInstance processInstance = startTestProcess("empty result");

    Object result = runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(result).isNull();
    TypedValue resultTyped = runtimeService.getVariableTyped(processInstance.getId(), "result");
    assertThat(resultTyped).isEqualTo(Variables.untypedNullValue());
  }

  @Deployment(resources = {SINGLE_RESULT_BPMN, TEST_DECISION})
  @Test
  void testSingleResultEmptyResult() {
    ProcessInstance processInstance = startTestProcess("empty result");

    Object result = runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(result).isNull();
    TypedValue resultTyped = runtimeService.getVariableTyped(processInstance.getId(), "result");
    assertThat(resultTyped).isEqualTo(Variables.untypedNullValue());
  }

  @Deployment(resources = {COLLECT_ENTRIES_BPMN, TEST_DECISION})
  @SuppressWarnings("unchecked")
  @Test
  void testCollectEntriesEmptyResult() {
    ProcessInstance processInstance = startTestProcess("empty result");

    List<Object> result = (List<Object>) runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(result).isEmpty();
  }

  @Deployment(resources = {RESULT_LIST_BPMN, TEST_DECISION})
  @SuppressWarnings("unchecked")
  @Test
  void testResultListEmptyResult() {
    ProcessInstance processInstance = startTestProcess("empty result");

    List<Object> result = (List<Object>) runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(result).isEmpty();
  }

  @Deployment(resources = {DEFAULT_MAPPING_BPMN, TEST_DECISION})
  @SuppressWarnings("unchecked")
  @Test
  void testDefaultMappingEmptyResult() {
    ProcessInstance processInstance = startTestProcess("empty result");

    List<Object> result = (List<Object>) runtimeService.getVariable(processInstance.getId(), "result");
    assertThat(result).isEmpty();
  }

  protected ProcessInstance startTestProcess(String input) {
    return runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("input", input));
  }

}
