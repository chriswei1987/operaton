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
package org.operaton.bpm.qa.performance.engine.dmn;

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.qa.performance.engine.junit.ProcessEnginePerformanceTestCase;
import org.operaton.bpm.qa.performance.engine.steps.StartProcessInstanceStep;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Execute process definitions which contains a DMN business rule task.
 *
 * @author Philipp Ossler
 *
 */
class DmnBusinessRuleTaskTest extends ProcessEnginePerformanceTestCase {

  private static final String BPMN = "org/operaton/bpm/qa/performance/engine/dmn/DmnBusinessRuleTaskTest.businessRuleTask.bpmn";
  private static final String DMN_DIR = "org/operaton/bpm/qa/performance/engine/dmn/";

  private static final String PROCESS_DEFINITION_KEY = "Process";

  // decision definition keys
  private static final String TWO_RULES = "twoRules";
  private static final String FIVE_RULES = "fiveRules";
  private static final String TEN_RULES = "tenRules";
  private static final String ONE_HUNDRED_RULES = "oneHundredRules";

  // 1.0 => 100% - all rules of the decision table will match
  // 0.5 => 50% - half of the rules of the decision table will match
  private static final double NUMBER_OF_MATCHING_RULES = 1.0;

  @Test
  @Deployment(resources = {BPMN, DMN_DIR + "DmnEnginePerformanceTest.twoRules.dmn"})
  void twoRules() {
    performanceTest()
      .step(startProcessInstanceStep(TWO_RULES))
    .run();
  }

  @Test
  @Deployment(resources = {BPMN, DMN_DIR + "DmnEnginePerformanceTest.fiveRules.dmn"})
  void fiveRules() {
    performanceTest()
      .step(startProcessInstanceStep(FIVE_RULES))
    .run();
  }

  @Test
  @Deployment(resources = {BPMN, DMN_DIR + "DmnEnginePerformanceTest.tenRules.dmn"})
  void tenRules() {
    performanceTest()
      .step(startProcessInstanceStep(TEN_RULES))
    .run();
  }

  @Test
  @Deployment(resources = {BPMN, DMN_DIR + "DmnEnginePerformanceTest.oneHundredRules.dmn"})
  void oneHundredRules() {
    performanceTest()
      .step(startProcessInstanceStep(ONE_HUNDRED_RULES))
    .run();
  }

  @Test
  @Deployment
  void noop() {
    performanceTest()
      .step(new StartProcessInstanceStep(engine, PROCESS_DEFINITION_KEY))
    .run();
  }

  private StartProcessInstanceStep startProcessInstanceStep(String decisionDefinitionKey) {
    Map<String, Object> variables = createVariables(decisionDefinitionKey);

    return new StartProcessInstanceStep(engine, PROCESS_DEFINITION_KEY, variables);
  }

  private Map<String, Object> createVariables(String decisionDefinitionKey) {
    return Variables.createVariables()
        .putValue("decisionKey", decisionDefinitionKey)
        .putValue("input", NUMBER_OF_MATCHING_RULES);
  }

}
