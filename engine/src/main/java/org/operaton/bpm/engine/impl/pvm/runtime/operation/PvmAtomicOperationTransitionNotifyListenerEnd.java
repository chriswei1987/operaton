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
package org.operaton.bpm.engine.impl.pvm.runtime.operation;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;


/**
 * @author Tom Baeyens
 */
public class PvmAtomicOperationTransitionNotifyListenerEnd extends PvmAtomicOperationActivityInstanceEnd {

  @Override
  protected ScopeImpl getScope(PvmExecutionImpl execution) {
    return execution.getActivity();
  }

  protected String getEventName() {
    return ExecutionListener.EVENTNAME_END;
  }

  @Override
  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {

    if (execution.isProcessInstanceStarting()) {
      // only call this method if we are currently in the starting phase;
      // if not, this may make an unnecessary request to fetch the process
      // instance from the database
      execution.setProcessInstanceStarting(false);
    }

    execution.dispatchDelayedEventsAndPerformOperation(execution1 -> {
      execution1.leaveActivityInstance();
      execution1.performOperation(TRANSITION_DESTROY_SCOPE);
      return null;
    });
  }

  @Override
  public String getCanonicalName() {
    return "transition-notify-listener-end";
  }

  @Override
  public boolean shouldHandleFailureAsBpmnError() {
    return true;
  }
}
