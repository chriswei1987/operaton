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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DiagramLayout;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author kristin.polenz
 */
class MultiTenancyProcessDefinitionCmdsTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String BPMN_PROCESS_MODEL = "org/operaton/bpm/engine/test/api/multitenancy/testProcess.bpmn";
  protected static final String BPMN_PROCESS_DIAGRAM = "org/operaton/bpm/engine/test/api/multitenancy/testProcess.png";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  protected String processDefinitionId;

  @BeforeEach
  void setUp() {
    testRule.deployForTenant(TENANT_ONE, BPMN_PROCESS_MODEL, BPMN_PROCESS_DIAGRAM);

    processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
  }

  @Test
  void failToGetProcessModelNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getProcessModel(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition");
  }

  @Test
  void getProcessModelWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getProcessModel(processDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getProcessModelDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getProcessModel(processDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetProcessDiagramNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getProcessDiagram(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition");
  }

  @Test
  void getProcessDiagramWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getProcessDiagram(processDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void getProcessDiagramDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getProcessDiagram(processDefinitionId);

    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetProcessDiagramLayoutNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getProcessDiagramLayout(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition");
  }

  @Test
  void getProcessDiagramLayoutWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DiagramLayout diagramLayout = repositoryService.getProcessDiagramLayout(processDefinitionId);

    assertThat(diagramLayout).isNotNull();
  }

  @Test
  void getProcessDiagramLayoutDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DiagramLayout diagramLayout = repositoryService.getProcessDiagramLayout(processDefinitionId);

    assertThat(diagramLayout).isNotNull();
  }

  @Test
  void failToGetProcessDefinitionNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getProcessDefinition(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition");
  }

  @Test
  void getProcessDefinitionWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void getProcessDefinitionDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void failToGetBpmnModelInstanceNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getBpmnModelInstance(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the process definition");
  }

  @Test
  void getBpmnModelInstanceWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);

    assertThat(modelInstance).isNotNull();
  }

  @Test
  void getBpmnModelInstanceDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);

    assertThat(modelInstance).isNotNull();
  }

  @Test
  void failToDeleteProcessDefinitionNoAuthenticatedTenant() {
    //given deployment with a process definition
    //and user with no tenant authentication
    identityService.setAuthentication("user", null, null);

    // when/then
    //deletion should end in exception, since tenant authorization is missing
    assertThatThrownBy(() -> repositoryService.deleteProcessDefinition(processDefinitionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the process definition");
  }

  @Test
  void testDeleteProcessDefinitionWithAuthenticatedTenant() {
    //given deployment with two process definitions
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/repository/twoProcesses.bpmn20.xml");
    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId());
    List<ProcessDefinition> processDefinitions = processDefinitionQuery.list();
    //and user with tenant authentication
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    //when delete process definition with authenticated user
    repositoryService.deleteProcessDefinition(processDefinitions.get(0).getId());

    //then process definition should be deleted
    identityService.clearAuthentication();
    assertThat(processDefinitionQuery.count()).isEqualTo(1L);
    assertThat(processDefinitionQuery.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteCascadeProcessDefinitionWithAuthenticatedTenant() {
    //given deployment with a process definition and process instance
    BpmnModelInstance bpmnModel = Bpmn.createExecutableProcess("process").startEvent().userTask().endEvent().done();
    testRule.deployForTenant(TENANT_ONE, bpmnModel);
    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    ProcessDefinition processDefinition = processDefinitionQuery.processDefinitionKey("process").singleResult();
    engineRule.getRuntimeService().createProcessInstanceByKey("process").executeWithVariablesInReturn();

    //and user with tenant authentication
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    //when the corresponding process definition is cascading deleted from the deployment
    repositoryService.deleteProcessDefinition(processDefinition.getId(), true);

    //then exist no process instance and one definition
    identityService.clearAuthentication();
    assertThat(engineRule.getRuntimeService().createProcessInstanceQuery().count()).isZero();
    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {
      assertThat(engineRule.getHistoryService().createHistoricActivityInstanceQuery().count()).isZero();
    }
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteProcessDefinitionDisabledTenantCheck() {
    //given deployment with two process definitions
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/repository/twoProcesses.bpmn20.xml");
    //tenant check disabled
    processEngineConfiguration.setTenantCheckEnabled(false);
    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId());
    List<ProcessDefinition> processDefinitions = processDefinitionQuery.list();
    //user with no authentication
    identityService.setAuthentication("user", null, null);

    //when process definition should be deleted without tenant check
    repositoryService.deleteProcessDefinition(processDefinitions.get(0).getId());

    //then process definition is deleted
    identityService.clearAuthentication();
    assertThat(processDefinitionQuery.count()).isEqualTo(1L);
    assertThat(processDefinitionQuery.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteCascadeProcessDefinitionDisabledTenantCheck() {
    //given deployment with a process definition and process instances
    BpmnModelInstance bpmnModel = Bpmn.createExecutableProcess("process").startEvent().userTask().endEvent().done();
    testRule.deployForTenant(TENANT_ONE, bpmnModel);
    //tenant check disabled
    processEngineConfiguration.setTenantCheckEnabled(false);
    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    ProcessDefinition processDefinition = processDefinitionQuery.processDefinitionKey("process").singleResult();
    engineRule.getRuntimeService().createProcessInstanceByKey("process").executeWithVariablesInReturn();
    //user with no authentication
    identityService.setAuthentication("user", null, null);

    //when the corresponding process definition is cascading deleted from the deployment
    repositoryService.deleteProcessDefinition(processDefinition.getId(), true);

    //then exist no process instance and one definition, because test case deployes per default one definition
    identityService.clearAuthentication();
    assertThat(engineRule.getRuntimeService().createProcessInstanceQuery().count()).isZero();
    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_ACTIVITY.getId()) {
      assertThat(engineRule.getHistoryService().createHistoricActivityInstanceQuery().count()).isZero();
    }
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void failToDeleteProcessDefinitionsByKeyNoAuthenticatedTenant() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    identityService.setAuthentication("user", null, null);
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byKey("process")
      .withoutTenantId();

    // when/then
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No process definition found");
  }

  @Test
  void testDeleteProcessDefinitionsByKeyForAllTenants() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
      deployProcessDefinitionWithoutTenant();
    }

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("process")
      .delete();

    // then
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteProcessDefinitionsByKeyWithAuthenticatedTenant() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("process")
      .withTenantId(TENANT_ONE)
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteCascadeProcessDefinitionsByKeyWithAuthenticatedTenant() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    runtimeService.startProcessInstanceByKey("process");

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("process")
      .withTenantId(TENANT_ONE)
      .cascade()
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteProcessDefinitionsByKeyDisabledTenantCheck() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("process")
      .withTenantId(TENANT_ONE)
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteCascadeProcessDefinitionsByKeyDisabledTenantCheck() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);
    runtimeService.startProcessInstanceByKey("process");

    // when
    repositoryService.deleteProcessDefinitions()
      .byKey("process")
      .withTenantId(TENANT_ONE)
      .cascade()
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void failToDeleteProcessDefinitionsByIdsNoAuthenticatedTenant() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey("process");

    identityService.setAuthentication("user", null, null);
    var deleteProcessDefinitionsBuilder = repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds);

    // when/then
    assertThatThrownBy(deleteProcessDefinitionsBuilder::delete)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the process definition");

  }

  @Test
  void testDeleteProcessDefinitionsByIdsWithAuthenticatedTenant() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey("process");

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteCascadeProcessDefinitionsByIdsWithAuthenticatedTenant() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey("process");

    runtimeService.startProcessInstanceByKey("process");

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .cascade()
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteProcessDefinitionsByIdsDisabledTenantCheck() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey("process");

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void testDeleteCascadeProcessDefinitionsByIdsDisabledTenantCheck() {
    // given
    for (int i = 0; i < 3; i++) {
      deployProcessDefinitionWithTenant();
    }

    String[] processDefinitionIds = findProcessDefinitionIdsByKey("process");

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);
    runtimeService.startProcessInstanceByKey("process");

    // when
    repositoryService.deleteProcessDefinitions()
      .byIds(processDefinitionIds)
      .cascade()
      .delete();

    // then
    identityService.clearAuthentication();
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1L);
    assertThat(repositoryService.createProcessDefinitionQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void updateHistoryTimeToLiveWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitionId, 6);

    ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  void updateHistoryTimeToLiveDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitionId, 6);

    ProcessDefinition definition = repositoryService.getProcessDefinition(processDefinitionId);

    assertThat(definition.getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(definition.getHistoryTimeToLive()).isEqualTo(6);
  }

  @Test
  void updateHistoryTimeToLiveNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.updateProcessDefinitionHistoryTimeToLive(processDefinitionId, 6))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition");

  }

  private String[] findProcessDefinitionIdsByKey(String processDefinitionKey) {
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey(processDefinitionKey).list();
    List<String> processDefinitionIds = new ArrayList<>();
    for (ProcessDefinition processDefinition: processDefinitions) {
      processDefinitionIds.add(processDefinition.getId());
    }

    return processDefinitionIds.toArray(new String[0]);
  }

  private void deployProcessDefinitionWithTenant() {
    testRule.deployForTenant(TENANT_ONE,
      Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask()
        .endEvent()
        .done());
  }

  private void deployProcessDefinitionWithoutTenant() {
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask()
        .endEvent()
        .done());
  }

}
