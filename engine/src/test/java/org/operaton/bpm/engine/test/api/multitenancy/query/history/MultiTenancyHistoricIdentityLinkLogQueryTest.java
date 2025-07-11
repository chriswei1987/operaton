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
package org.operaton.bpm.engine.test.api.multitenancy.query.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLogQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
*
* @author Deivarayan Azhagappan
*
*/
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class MultiTenancyHistoricIdentityLinkLogQueryTest {

  private static final String GROUP_1 = "Group1";
  private static final String USER_1 = "User1";

  private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected TaskService taskService;

  protected static final String A_USER_ID = "aUserId";

  protected static final String TENANT_NULL = null;
  protected static final String TENANT_1 = "tenant1";
  protected static final String TENANT_2 = "tenant2";
  protected static final String TENANT_3 = "tenant3";

  @BeforeEach
  void init() {
    // create sample identity link
    BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess("testProcess")
    .startEvent()
    .userTask("task").operatonCandidateUsers(A_USER_ID)
    .endEvent()
    .done();

    // deploy tenants
    testRule.deployForTenant(TENANT_NULL, oneTaskProcess);
    testRule.deployForTenant(TENANT_1, oneTaskProcess);
    testRule.deployForTenant(TENANT_2, oneTaskProcess);
    testRule.deployForTenant(TENANT_3, oneTaskProcess);
  }

  @Test
  void shouldAddAndDeleteHistoricIdentityLinkForSingleTenant() {
    // given
    startProcessInstanceForTenant(TENANT_1);
    HistoricIdentityLinkLogQuery query = historyService
        .createHistoricIdentityLinkLogQuery();

    // when
    HistoricIdentityLinkLog historicIdentityLink = query.singleResult();
    taskService.deleteCandidateUser(historicIdentityLink.getTaskId(), A_USER_ID);

    // then
    assertThat(query.tenantIdIn(TENANT_1).count()).isEqualTo(2L);
  }

  @Test
  void shouldQueryWithoutTenantId() {
    // given
    startProcessInstanceForTenant(TENANT_NULL);
    startProcessInstanceForTenant(TENANT_1);

    // when
    HistoricIdentityLinkLogQuery query = historyService
        .createHistoricIdentityLinkLogQuery()
        .withoutTenantId();

    // then
    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  void shouldAddHistoricIdentityLinkForMultipleTenants() {
    // given
    startProcessInstanceForTenant(TENANT_1);
    startProcessInstanceForTenant(TENANT_2);

    // when
    HistoricIdentityLinkLogQuery query = historyService
        .createHistoricIdentityLinkLogQuery();

    // then
    assertThat(query.list()).hasSize(2);
    assertThat(query.tenantIdIn(TENANT_1).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_2).count()).isEqualTo(1L);
  }

  @Test
  void shouldAddAndRemoveHistoricIdentityLinksForProcessDefinitionWithTenantId() {
    // given
    String resourceName = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";
    testRule.deployForTenant(TENANT_1, resourceName);
    testRule.deployForTenant(TENANT_2, resourceName);

    ProcessDefinition processDefinition1 = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey(PROCESS_DEFINITION_KEY)
      .list()
      .get(0);
    ProcessDefinition processDefinition2 = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionKey(PROCESS_DEFINITION_KEY)
      .list()
      .get(1);

    // assume
    assertThat(processDefinition1).isNotNull();
    assertThat(processDefinition2).isNotNull();

    // when
    createIdentityLinks(processDefinition1.getId());
    createIdentityLinks(processDefinition2.getId());

    // then
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.count()).isEqualTo(8);
    assertThat(query.tenantIdIn(TENANT_1).count()).isEqualTo(4L);
    assertThat(query.tenantIdIn(TENANT_2).count()).isEqualTo(4L);
  }

  @SuppressWarnings("deprecation")
  @Test
  void shouldAddIdentityLinksForProcessDefinitionWithTenantId() {
    // given
    String resourceName = "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";
    testRule.deployForTenant(TENANT_1, resourceName);
    testRule.deployForTenant(TENANT_2, resourceName);

    ProcessDefinition processDefinition1 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .list()
        .get(0);

    ProcessDefinition processDefinition2 = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY)
        .list()
        .get(1);

    assertThat(processDefinition1).isNotNull();
    assertThat(processDefinition2).isNotNull();

    // when
    addIdentityLinks(processDefinition1.getId());
    addIdentityLinks(processDefinition2.getId());

    // then
    // Identity link test
    List<IdentityLink> identityLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinition1.getId());
    assertThat(identityLinks).hasSize(2);
    assertThat(identityLinks.get(0).getTenantId()).isEqualTo(TENANT_1);
    assertThat(identityLinks.get(1).getTenantId()).isEqualTo(TENANT_1);

    identityLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinition2.getId());
    assertThat(identityLinks).hasSize(2);
    assertThat(identityLinks.get(0).getTenantId()).isEqualTo(TENANT_2);
    assertThat(identityLinks.get(1).getTenantId()).isEqualTo(TENANT_2);
  }

  @Test
  void shouldUseSingleQueryForMultipleTenants() {
    // when
    startProcessInstanceForTenant(TENANT_NULL);
    startProcessInstanceForTenant(TENANT_1);
    startProcessInstanceForTenant(TENANT_2);
    startProcessInstanceForTenant(TENANT_3);

    // then
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.withoutTenantId().count()).isEqualTo(1);
    assertThat(query.tenantIdIn(TENANT_1, TENANT_2).count()).isEqualTo(2);
    assertThat(query.tenantIdIn(TENANT_2, TENANT_3).count()).isEqualTo(2);
    assertThat(query.tenantIdIn(TENANT_1, TENANT_2, TENANT_3).count()).isEqualTo(3);
  }

  protected void createIdentityLinks(String processDefinitionId) {
    addIdentityLinks(processDefinitionId);
    deleteIdentityLinks(processDefinitionId);
  }

  @SuppressWarnings("deprecation")
  protected void addIdentityLinks(String processDefinitionId) {
    repositoryService.addCandidateStarterGroup(processDefinitionId, GROUP_1);
    repositoryService.addCandidateStarterUser(processDefinitionId, USER_1);
  }

  @SuppressWarnings("deprecation")
  protected void deleteIdentityLinks(String processDefinitionId) {
    repositoryService.deleteCandidateStarterGroup(processDefinitionId, GROUP_1);
    repositoryService.deleteCandidateStarterUser(processDefinitionId, USER_1);
  }

  protected ProcessInstance startProcessInstanceForTenant(String tenant) {
    return runtimeService.createProcessInstanceByKey("testProcess")
        .processDefinitionTenantId(tenant)
        .execute();
  }
}
