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

import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;

/**
 * @deprecated Command is empty
 * The sending telemetry data feature is removed.
 * Please any remove usages of the command.
 */
public class TelemetryConfigureCmd implements Command<Void> {

  protected boolean telemetryEnabled;

  public TelemetryConfigureCmd(boolean telemetryEnabled) {
    this.telemetryEnabled = telemetryEnabled;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    return null;
  }

}
