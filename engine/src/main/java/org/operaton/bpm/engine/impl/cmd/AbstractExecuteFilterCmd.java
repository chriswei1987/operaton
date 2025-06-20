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
package org.operaton.bpm.engine.impl.cmd;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;

import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.task.TaskQuery;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractExecuteFilterCmd implements Serializable {

  private static final long serialVersionUID = 1L;

  protected String filterId;
  protected Query<?, ?> extendingQuery;

  protected AbstractExecuteFilterCmd(String filterId) {
    this.filterId = filterId;
  }

  protected AbstractExecuteFilterCmd(String filterId, Query<?, ?> extendingQuery) {
    this.filterId = filterId;
    this.extendingQuery = extendingQuery;
  }

  protected Filter getFilter(CommandContext commandContext) {
    ensureNotNull("No filter id given to execute", "filterId", filterId);
    FilterEntity filter = commandContext
      .getFilterManager()
      .findFilterById(filterId);

    ensureNotNull("No filter found for id '" + filterId + "'", "filter", filter);

    if (extendingQuery != null) {
      ((AbstractQuery<?, ?>) extendingQuery).validate();
      filter = (FilterEntity) filter.extend(extendingQuery);
    }

    return filter;
  }

  protected Query<?, ?> getFilterQuery(CommandContext commandContext) {
    Filter filter = getFilter(commandContext);
    Query<?, ?> query = filter.getQuery();
    if (query instanceof TaskQuery taskQuery) {
      taskQuery.initializeFormKeys();
    }
    return query;
  }
}
