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
package org.operaton.bpm.integrationtest.util;

import org.operaton.bpm.BpmPlatform;

/**
 * @author Thorben Lindhauer
 *
 */
public class TestConstants {

  public static final String APP_NAME = "";

  public static final String PROCESS_ENGINE_SERVICE_JNDI_NAME = BpmPlatform.PROCESS_ENGINE_SERVICE_JNDI_NAME;
  public static final String PROCESS_APPLICATION_SERVICE_JNDI_NAME = BpmPlatform.PROCESS_APPLICATION_SERVICE_JNDI_NAME;

  public static String getAppName() {
    return APP_NAME;
  }

  public String getEngineService() {
    return PROCESS_ENGINE_SERVICE_JNDI_NAME;
  }

  public String getProcessApplicationService() {
    return PROCESS_APPLICATION_SERVICE_JNDI_NAME;
  }

}
