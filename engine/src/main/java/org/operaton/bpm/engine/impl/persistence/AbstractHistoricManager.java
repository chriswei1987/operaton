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
package org.operaton.bpm.engine.impl.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.entitymanager.operation.DbOperation;
import org.operaton.bpm.engine.impl.history.HistoryLevel;


/**
 * @author Tom Baeyens
 */
public class AbstractHistoricManager extends AbstractManager {

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  protected HistoryLevel historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();

  protected boolean isHistoryEnabled = !historyLevel.equals(HistoryLevel.HISTORY_LEVEL_NONE);
  protected boolean isHistoryLevelFullEnabled = historyLevel.equals(HistoryLevel.HISTORY_LEVEL_FULL);

  protected void checkHistoryEnabled() {
    if (!isHistoryEnabled) {
      throw LOG.disabledHistoryException();
    }
  }

  public boolean isHistoryEnabled() {
    return isHistoryEnabled;
  }

  public boolean isHistoryLevelFullEnabled() {
    return isHistoryLevelFullEnabled;
  }

  protected static boolean isPerformUpdate(Set<String> entities, Class<?> entityClass) {
    return entities == null || entities.isEmpty() || entities.contains(entityClass.getName());
  }

  protected static boolean isPerformUpdateOnly(Set<String> entities, Class<?> entityClass) {
    return entities != null && entities.size() == 1 && entities.contains(entityClass.getName());
  }

  protected static void addOperation(DbOperation operation, Map<Class<? extends DbEntity>, DbOperation> operations) {
    operations.put(operation.getEntityType(), operation);
  }

  protected static void addOperation(Collection<DbOperation> newOperations, Map<Class<? extends DbEntity>, DbOperation> operations) {
    newOperations.forEach(operation -> operations.put(operation.getEntityType(), operation));
  }
}
