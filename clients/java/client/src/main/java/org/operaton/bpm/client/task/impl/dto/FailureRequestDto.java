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
package org.operaton.bpm.client.task.impl.dto;

import java.util.Map;

import org.operaton.bpm.client.impl.RequestDto;
import org.operaton.bpm.client.variable.impl.TypedValueField;

/**
 * @author Tassilo Weidner
 */
public class FailureRequestDto extends RequestDto {

  protected String errorMessage;
  protected String errorDetails;
  protected int retries;
  protected long retryTimeout;
  protected Map<String, TypedValueField> variables;
  protected Map<String, TypedValueField> localVariables;

  public FailureRequestDto(String workerId, String errorMessage, String errorDetails, int retries, long retryTimeout, Map<String, TypedValueField> variables, Map<String, TypedValueField> localVariables) {
    super(workerId);
    this.errorMessage = errorMessage;
    this.errorDetails = errorDetails;
    this.retries = retries;
    this.retryTimeout = retryTimeout;
    this.variables = variables;
    this.localVariables = localVariables;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorDetails() {
    return errorDetails;
  }

  public int getRetries() {
    return retries;
  }

  public long getRetryTimeout() {
    return retryTimeout;
  }

  public Map<String, TypedValueField> getVariables() {
    return variables;
  }

  public Map<String, TypedValueField> getLocalVariables() {
    return localVariables;
  }

}
