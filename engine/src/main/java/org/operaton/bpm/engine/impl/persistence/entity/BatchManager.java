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

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.batch.BatchEntity;
import org.operaton.bpm.engine.impl.batch.BatchQueryImpl;
import org.operaton.bpm.engine.impl.db.ListQueryParameterObject;
import org.operaton.bpm.engine.impl.persistence.AbstractManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchManager extends AbstractManager {

  public void insertBatch(BatchEntity batch) {
    batch.setCreateUserId(getCommandContext().getAuthenticatedUserId());
    getDbEntityManager().insert(batch);
  }

  public BatchEntity findBatchById(String id) {
    return getDbEntityManager().selectById(BatchEntity.class, id);
  }

  public long findBatchCountByQueryCriteria(BatchQueryImpl batchQuery) {
    configureQuery(batchQuery);
    return (Long) getDbEntityManager().selectOne("selectBatchCountByQueryCriteria", batchQuery);
  }

  @SuppressWarnings("unchecked")
  public List<Batch> findBatchesByQueryCriteria(BatchQueryImpl batchQuery, Page page) {
    configureQuery(batchQuery);
    return getDbEntityManager().selectList("selectBatchesByQueryCriteria", batchQuery, page);
  }

  public void updateBatchSuspensionStateById(String batchId, SuspensionState suspensionState) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(BATCH_ID, batchId);
    parameters.put(SUSPENSION_STATE, suspensionState.getStateCode());

    ListQueryParameterObject queryParameter = new ListQueryParameterObject();
    queryParameter.setParameter(parameters);

    getDbEntityManager().update(BatchEntity.class, "updateBatchSuspensionStateByParameters", queryParameter);
  }

  protected void configureQuery(BatchQueryImpl batchQuery) {
    getAuthorizationManager().configureBatchQuery(batchQuery);
    getTenantManager().configureQuery(batchQuery);
  }

}
