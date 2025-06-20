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
package org.operaton.bpm.engine.test.api.authorization.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.junit.runner.Description;

/**
 * @author Thorben Lindhauer
 *
 */
public class AuthorizationTestRule extends AuthorizationTestBaseRule {

  protected AuthorizationExceptionInterceptor interceptor;
  protected CommandExecutor replacedCommandExecutor;

  protected AuthorizationScenarioInstance scenarioInstance;

  public AuthorizationTestRule(ProcessEngineRule engineRule) {
    super(engineRule);
    this.interceptor = new AuthorizationExceptionInterceptor();
  }

  public void start(AuthorizationScenario scenario) {
    start(scenario, null, new HashMap<>());
  }

  public void start(AuthorizationScenario scenario, String userId, Map<String, String> resourceBindings) {
    assertThat(interceptor.getLastException()).isNull();
    scenarioInstance = new AuthorizationScenarioInstance(scenario, engineRule.getAuthorizationService(), resourceBindings);
    enableAuthorization(userId);
    interceptor.activate();
  }

  /**
   * Assert the scenario conditions. If no exception or the expected one was thrown.
   *
   * @param scenario the scenario to assert on
   * @return true if no exception was thrown, false otherwise
   */
  public boolean assertScenario(AuthorizationScenario scenario) {

    interceptor.deactivate();
    disableAuthorization();
    scenarioInstance.tearDown(engineRule.getAuthorizationService());
    scenarioInstance.assertAuthorizationException(interceptor.getLastException());
    scenarioInstance = null;

    return scenarioSucceeded();
  }

  /**
   * No exception was expected and none was thrown
   */
  public boolean scenarioSucceeded() {
    return interceptor.getLastException() == null;
  }

  public boolean scenarioFailed() {
    return interceptor.getLastException() != null;
  }

  @Override
  protected void starting(Description description) {
    ProcessEngineConfigurationImpl engineConfiguration =
        (ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration();

    interceptor.reset();
    engineConfiguration.getCommandInterceptorsTxRequired().get(0).setNext(interceptor);
    interceptor.setNext(engineConfiguration.getCommandInterceptorsTxRequired().get(1));

    super.starting(description);
  }

  @Override
  protected void finished(Description description) {
    super.finished(description);

    ProcessEngineConfigurationImpl engineConfiguration =
        (ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration();

    engineConfiguration.getCommandInterceptorsTxRequired().get(0).setNext(interceptor.getNext());
    interceptor.setNext(null);
  }

  public static Collection<AuthorizationScenario[]> asParameters(AuthorizationScenario... scenarios) {
    List<AuthorizationScenario[]> scenarioList = new ArrayList<>();
    for (AuthorizationScenario scenario : scenarios) {
      scenarioList.add(new AuthorizationScenario[]{ scenario });
    }

    return scenarioList;
  }

  public AuthorizationScenarioInstanceBuilder init(AuthorizationScenario scenario) {
    AuthorizationScenarioInstanceBuilder builder = new AuthorizationScenarioInstanceBuilder();
    builder.scenario = scenario;
    builder.rule = this;
    return builder;
  }

  public static class AuthorizationScenarioInstanceBuilder {
    protected AuthorizationScenario scenario;
    protected AuthorizationTestRule rule;
    protected String userId;
    protected Map<String, String> resourceBindings = new HashMap<>();

    public AuthorizationScenarioInstanceBuilder withUser(String userId) {
      this.userId = userId;
      return this;
    }

    public AuthorizationScenarioInstanceBuilder bindResource(String key, String value) {
      resourceBindings.put(key, value);
      return this;
    }

    public void start() {
      rule.start(scenario, userId, resourceBindings);
    }
  }

}
