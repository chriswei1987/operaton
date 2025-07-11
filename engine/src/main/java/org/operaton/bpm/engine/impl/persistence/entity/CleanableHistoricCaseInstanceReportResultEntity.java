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
package org.operaton.bpm.engine.impl.persistence.entity;

import org.operaton.bpm.engine.history.CleanableHistoricCaseInstanceReportResult;

public class CleanableHistoricCaseInstanceReportResultEntity implements CleanableHistoricCaseInstanceReportResult {

  protected String caseDefinitionId;
  protected String caseDefinitionKey;
  protected String caseDefinitionName;
  protected int caseDefinitionVersion;
  protected Integer historyTimeToLive;
  protected long finishedCaseInstanceCount;
  protected long cleanableCaseInstanceCount;
  protected String tenantId;

  @Override
  public String getCaseDefinitionId() {
    return caseDefinitionId;
  }

  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @Override
  public String getCaseDefinitionKey() {
    return caseDefinitionKey;
  }

  public void setCaseDefinitionKey(String caseDefinitionKey) {
    this.caseDefinitionKey = caseDefinitionKey;
  }

  @Override
  public String getCaseDefinitionName() {
    return caseDefinitionName;
  }

  public void setCaseDefinitionName(String caseDefinitionName) {
    this.caseDefinitionName = caseDefinitionName;
  }

  @Override
  public int getCaseDefinitionVersion() {
    return caseDefinitionVersion;
  }

  public void setCaseDefinitionVersion(int caseDefinitionVersion) {
    this.caseDefinitionVersion = caseDefinitionVersion;
  }

  @Override
  public Integer getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  public void setHistoryTimeToLive(Integer historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }

  @Override
  public long getFinishedCaseInstanceCount() {
    return finishedCaseInstanceCount;
  }

  public void setFinishedCaseInstanceCount(Long finishedCaseInstanceCount) {
    this.finishedCaseInstanceCount = finishedCaseInstanceCount;
  }

  @Override
  public long getCleanableCaseInstanceCount() {
    return cleanableCaseInstanceCount;
  }

  public void setCleanableCaseInstanceCount(Long cleanableCaseInstanceCount) {
    this.cleanableCaseInstanceCount = cleanableCaseInstanceCount;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "[caseDefinitionId = " + caseDefinitionId
        + ", caseDefinitionKey = " + caseDefinitionKey
        + ", caseDefinitionName = " + caseDefinitionName
        + ", caseDefinitionVersion = " + caseDefinitionVersion
        + ", historyTimeToLive = " + historyTimeToLive
        + ", finishedCaseInstanceCount = " + finishedCaseInstanceCount
        + ", cleanableCaseInstanceCount = " + cleanableCaseInstanceCount
        + ", tenantId = " + tenantId
        + "]";
  }
}
