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
package org.operaton.bpm.engine.impl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.pvm.runtime.ActivityInstanceState;
import org.operaton.bpm.engine.impl.util.CompareUtil;

/**
 * @author Tom Baeyens
 */
public class HistoricActivityInstanceQueryImpl extends AbstractQuery<HistoricActivityInstanceQuery, HistoricActivityInstance>
    implements HistoricActivityInstanceQuery {

  private static final long serialVersionUID = 1L;
  protected String activityInstanceId;
  protected String processInstanceId;
  protected String executionId;
  protected String processDefinitionId;
  protected String activityId;
  protected String activityName;
  protected String activityNameLike;
  protected String activityType;
  protected String assignee;
  protected boolean finished;
  protected boolean unfinished;
  protected Date startedBefore;
  protected Date startedAfter;
  protected Date finishedBefore;
  protected Date finishedAfter;
  protected ActivityInstanceState activityInstanceState;
  protected String[] tenantIds;
  protected boolean isTenantIdSet;

  public HistoricActivityInstanceQueryImpl() {
  }

  public HistoricActivityInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getHistoricActivityInstanceManager()
      .findHistoricActivityInstanceCountByQueryCriteria(this);
  }

  @Override
  public List<HistoricActivityInstance> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getHistoricActivityInstanceManager()
      .findHistoricActivityInstancesByQueryCriteria(this, page);
  }

  @Override
  public HistoricActivityInstanceQueryImpl processInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl executionId(String executionId) {
    this.executionId = executionId;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl processDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl activityId(String activityId) {
    this.activityId = activityId;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl activityName(String activityName) {
    this.activityName = activityName;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl activityNameLike(String activityNameLike) {
    this.activityNameLike = activityNameLike;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl activityType(String activityType) {
    this.activityType = activityType;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl taskAssignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl finished() {
    this.finished = true;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl unfinished() {
    this.unfinished = true;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl completeScope() {
    if (activityInstanceState != null) {
      throw new ProcessEngineException("Already querying for activity instance state <" + activityInstanceState + ">");
    }

    this.activityInstanceState = ActivityInstanceState.SCOPE_COMPLETE;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl canceled() {
    if (activityInstanceState != null) {
      throw new ProcessEngineException("Already querying for activity instance state <" + activityInstanceState + ">");
    }
    this.activityInstanceState = ActivityInstanceState.CANCELED;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl startedAfter(Date date) {
    startedAfter = date;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl startedBefore(Date date) {
    startedBefore = date;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl finishedAfter(Date date) {
    finishedAfter = date;
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl finishedBefore(Date date) {
    finishedBefore = date;
    return this;
  }

  @Override
  public HistoricActivityInstanceQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public HistoricActivityInstanceQuery withoutTenantId() {
    this.tenantIds = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions()
      || CompareUtil.areNotInAscendingOrder(startedAfter, startedBefore)
      || CompareUtil.areNotInAscendingOrder(finishedAfter, finishedBefore);
  }

  // ordering /////////////////////////////////////////////////////////////////

  @Override
  public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceDuration() {
    orderBy(HistoricActivityInstanceQueryProperty.DURATION);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceEndTime() {
    orderBy(HistoricActivityInstanceQueryProperty.END);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByExecutionId() {
    orderBy(HistoricActivityInstanceQueryProperty.EXECUTION_ID);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceId() {
    orderBy(HistoricActivityInstanceQueryProperty.HISTORIC_ACTIVITY_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByProcessDefinitionId() {
    orderBy(HistoricActivityInstanceQueryProperty.PROCESS_DEFINITION_ID);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByProcessInstanceId() {
    orderBy(HistoricActivityInstanceQueryProperty.PROCESS_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByHistoricActivityInstanceStartTime() {
    orderBy(HistoricActivityInstanceQueryProperty.START);
    return this;
  }

  @Override
  public HistoricActivityInstanceQuery orderByActivityId() {
    orderBy(HistoricActivityInstanceQueryProperty.ACTIVITY_ID);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByActivityName() {
    orderBy(HistoricActivityInstanceQueryProperty.ACTIVITY_NAME);
    return this;
  }

  @Override
  public HistoricActivityInstanceQueryImpl orderByActivityType() {
    orderBy(HistoricActivityInstanceQueryProperty.ACTIVITY_TYPE);
    return this;
  }

  @Override
  public HistoricActivityInstanceQuery orderPartiallyByOccurrence() {
    orderBy(HistoricActivityInstanceQueryProperty.SEQUENCE_COUNTER);
    return this;
  }

  @Override
  public HistoricActivityInstanceQuery orderByTenantId() {
    return orderBy(HistoricActivityInstanceQueryProperty.TENANT_ID);
  }

  @Override
  public HistoricActivityInstanceQueryImpl activityInstanceId(String activityInstanceId) {
    this.activityInstanceId = activityInstanceId;
    return this;
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getProcessInstanceId() {
    return processInstanceId;
  }
  public String getExecutionId() {
    return executionId;
  }
  public String getProcessDefinitionId() {
    return processDefinitionId;
  }
  public String getActivityId() {
    return activityId;
  }
  public String getActivityName() {
    return activityName;
  }
  public String getActivityType() {
    return activityType;
  }
  public String getAssignee() {
    return assignee;
  }
  public boolean isFinished() {
    return finished;
  }
  public boolean isUnfinished() {
    return unfinished;
  }
  public String getActivityInstanceId() {
    return activityInstanceId;
  }
  public Date getStartedAfter() {
    return startedAfter;
  }
  public Date getStartedBefore() {
    return startedBefore;
  }
  public Date getFinishedAfter() {
    return finishedAfter;
  }
  public Date getFinishedBefore() {
    return finishedBefore;
  }
  public ActivityInstanceState getActivityInstanceState() {
    return activityInstanceState;
  }
  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }
}
