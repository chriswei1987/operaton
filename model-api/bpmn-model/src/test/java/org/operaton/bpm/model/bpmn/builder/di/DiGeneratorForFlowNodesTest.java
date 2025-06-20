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
package org.operaton.bpm.model.bpmn.builder.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.BOUNDARY_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.CALL_ACTIVITY_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.CATCH_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.CONDITION_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.END_EVENT_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SEND_TASK_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SERVICE_TASK_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.START_EVENT_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.SUB_PROCESS_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.TASK_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.TEST_CONDITION;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.TRANSACTION_ID;
import static org.operaton.bpm.model.bpmn.BpmnTestConstants.USER_TASK_ID;

import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnShape;

class DiGeneratorForFlowNodesTest {

  private BpmnModelInstance instance;

  @AfterEach
  void validateModel() {
    if (instance != null) {
      Bpmn.validateModel(instance);
    }
  }

  @Test
  void shouldGeneratePlaneForProcess() {

    // when
    instance = Bpmn.createExecutableProcess("process").done();

    // then
    Collection<BpmnDiagram> bpmnDiagrams = instance.getModelElementsByType(BpmnDiagram.class);
    assertEquals(1, bpmnDiagrams.size());

    BpmnDiagram diagram = bpmnDiagrams.iterator().next();
    assertNotNull(diagram.getId());

    assertNotNull(diagram.getBpmnPlane());
    assertEquals(diagram.getBpmnPlane().getBpmnElement(), instance.getModelElementById("process"));
  }

  @Test
  void shouldGenerateShapeForStartEvent() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertEventShapeProperties(START_EVENT_ID);
  }

  @Test
  void shouldGenerateShapeForUserTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .userTask(USER_TASK_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(USER_TASK_ID);
  }

  @Test
  void shouldGenerateShapeForSendTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .sendTask(SEND_TASK_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(SEND_TASK_ID);
  }

  @Test
  void shouldGenerateShapeForServiceTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                   .startEvent(START_EVENT_ID)
                   .serviceTask(SERVICE_TASK_ID)
                   .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(SERVICE_TASK_ID);
  }

  @Test
  void shouldGenerateShapeForReceiveTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .receiveTask(TASK_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(TASK_ID);
  }

  @Test
  void shouldGenerateShapeForManualTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .manualTask(TASK_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(TASK_ID);
  }

  @Test
  void shouldGenerateShapeForBusinessRuleTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .businessRuleTask(TASK_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(TASK_ID);
  }

  @Test
  void shouldGenerateShapeForScriptTask() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .scriptTask(TASK_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertTaskShapeProperties(TASK_ID);
  }

  @Test
  void shouldGenerateShapeForCatchingIntermediateEvent() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .intermediateCatchEvent(CATCH_ID)
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertEventShapeProperties(CATCH_ID);
  }

  @Test
  void shouldGenerateShapeForBoundaryIntermediateEvent() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .userTask(USER_TASK_ID)
                  .endEvent(END_EVENT_ID)
                    .moveToActivity(USER_TASK_ID)
                      .boundaryEvent(BOUNDARY_ID)
                        .conditionalEventDefinition(CONDITION_ID)
                          .condition(TEST_CONDITION)
                        .conditionalEventDefinitionDone()
                      .endEvent()
                 .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(5, allShapes.size());

    assertEventShapeProperties(BOUNDARY_ID);
  }

  @Test
  void shouldGenerateShapeForThrowingIntermediateEvent() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .intermediateThrowEvent("inter")
                  .endEvent(END_EVENT_ID).done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertEventShapeProperties("inter");
  }

  @Test
  void shouldGenerateShapeForEndEvent() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(2, allShapes.size());

    assertEventShapeProperties(END_EVENT_ID);
  }

  @Test
  void shouldGenerateShapeForBlankSubProcess() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .subProcess(SUB_PROCESS_ID)
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    BpmnShape bpmnShapeSubProcess = findBpmnShape(SUB_PROCESS_ID);
    assertNotNull(bpmnShapeSubProcess);
    assertSubProcessSize(bpmnShapeSubProcess);
    assertTrue(bpmnShapeSubProcess.isExpanded());
  }

  @Test
  void shouldGenerateShapesForNestedFlowNodes() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .subProcess(SUB_PROCESS_ID)
                    .embeddedSubProcess()
                      .startEvent("innerStartEvent")
                      .userTask("innerUserTask")
                      .endEvent("innerEndEvent")
                    .subProcessDone()
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(6, allShapes.size());

    assertEventShapeProperties("innerStartEvent");
    assertTaskShapeProperties("innerUserTask");
    assertEventShapeProperties("innerEndEvent");

    BpmnShape bpmnShapeSubProcess = findBpmnShape(SUB_PROCESS_ID);
    assertNotNull(bpmnShapeSubProcess);
    assertTrue(bpmnShapeSubProcess.isExpanded());
  }

  @Test
  void shouldGenerateShapeForEventSubProcess() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .endEvent(END_EVENT_ID)
                  .subProcess(SUB_PROCESS_ID)
                    .triggerByEvent()
                      .embeddedSubProcess()
                        .startEvent("innerStartEvent")
                        .endEvent("innerEndEvent")
                      .subProcessDone()
                    .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(5, allShapes.size());

    assertEventShapeProperties("innerStartEvent");
    assertEventShapeProperties("innerEndEvent");

    BpmnShape bpmnShapeEventSubProcess = findBpmnShape(SUB_PROCESS_ID);
    assertNotNull(bpmnShapeEventSubProcess);
    assertTrue(bpmnShapeEventSubProcess.isExpanded());
  }

  @Test
  void shouldGenerateShapeForCallActivity() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .callActivity(CALL_ACTIVITY_ID)
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertTaskShapeProperties(CALL_ACTIVITY_ID);
  }

  @Test
  void shouldGenerateShapeForTransaction() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .transaction(TRANSACTION_ID)
                  .embeddedSubProcess()
                    .startEvent("innerStartEvent")
                    .userTask("innerUserTask")
                    .endEvent("innerEndEvent")
                  .transactionDone()
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(6, allShapes.size());

    assertEventShapeProperties("innerStartEvent");
    assertTaskShapeProperties("innerUserTask");
    assertEventShapeProperties("innerEndEvent");

    BpmnShape bpmnShapeSubProcess = findBpmnShape(TRANSACTION_ID);
    assertNotNull(bpmnShapeSubProcess);
    assertTrue(bpmnShapeSubProcess.isExpanded());
  }

  @Test
  void shouldGenerateShapeForParallelGateway() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .parallelGateway("and")
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertGatewayShapeProperties("and");
  }

  @Test
  void shouldGenerateShapeForInclusiveGateway() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .inclusiveGateway("inclusive")
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertGatewayShapeProperties("inclusive");
  }

  @Test
  void shouldGenerateShapeForEventBasedGateway() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .eventBasedGateway()
                    .id("eventBased")
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertGatewayShapeProperties("eventBased");
  }

  @Test
  void shouldGenerateShapeForExclusiveGateway() {

    // given
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess();

    // when
    instance = processBuilder
                  .startEvent(START_EVENT_ID)
                  .exclusiveGateway("or")
                  .endEvent(END_EVENT_ID)
                  .done();

    // then
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);
    assertEquals(3, allShapes.size());

    assertGatewayShapeProperties("or");
    BpmnShape bpmnShape = findBpmnShape("or");
    assertTrue(bpmnShape.isMarkerVisible());
  }

  protected void assertTaskShapeProperties(String id) {
    BpmnShape bpmnShapeTask = findBpmnShape(id);
    assertNotNull(bpmnShapeTask);
    assertActivitySize(bpmnShapeTask);
  }

  protected void assertEventShapeProperties(String id) {
    BpmnShape bpmnShapeEvent = findBpmnShape(id);
    assertNotNull(bpmnShapeEvent);
    assertEventSize(bpmnShapeEvent);
  }

  protected void assertGatewayShapeProperties(String id) {
    BpmnShape bpmnShapeGateway = findBpmnShape(id);
    assertNotNull(bpmnShapeGateway);
    assertGatewaySize(bpmnShapeGateway);
  }

  protected BpmnShape findBpmnShape(String id) {
    Collection<BpmnShape> allShapes = instance.getModelElementsByType(BpmnShape.class);

    Iterator<BpmnShape> iterator = allShapes.iterator();
    while (iterator.hasNext()) {
      BpmnShape shape = iterator.next();
      if (shape.getBpmnElement().getId().equals(id)) {
        return shape;
      }
    }
    return null;
  }

  protected void assertEventSize(BpmnShape shape) {
    assertSize(shape, 36, 36);
  }

  protected void assertGatewaySize(BpmnShape shape) {
    assertSize(shape, 50, 50);
  }

  protected void assertSubProcessSize(BpmnShape shape) {
    assertSize(shape, 200, 350);
  }

  protected void assertActivitySize(BpmnShape shape) {
    assertSize(shape, 80, 100);
  }

  protected void assertSize(BpmnShape shape, int height, int width) {
    assertThat(shape.getBounds().getHeight()).isEqualTo(height);
    assertThat(shape.getBounds().getWidth()).isEqualTo(width);
  }

}
