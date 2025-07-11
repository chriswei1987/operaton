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
package org.operaton.bpm.engine.test.jobexecutor;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandInvocationContext;
import org.operaton.bpm.engine.test.concurrency.ConcurrencyTestHelper;

public class ControllableCommandContext extends CommandContext {

  protected ConcurrencyTestHelper.ThreadControl executionThread;
  protected boolean skipFlushControl;

  public ControllableCommandContext(ProcessEngineConfigurationImpl processEngineConfiguration,
      ConcurrencyTestHelper.ThreadControl executionThread, boolean skipFlushControl) {
    super(processEngineConfiguration);
    this.executionThread = executionThread;
    this.skipFlushControl = skipFlushControl;
  }

  @Override
  public void close(CommandInvocationContext commandInvocationContext) {
    super.close(commandInvocationContext);
    if (!skipFlushControl) {
      executionThread.sync();
    }
  }

}
