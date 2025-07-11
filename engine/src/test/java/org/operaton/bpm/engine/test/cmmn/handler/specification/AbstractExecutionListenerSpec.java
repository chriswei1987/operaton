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
package org.operaton.bpm.engine.test.cmmn.handler.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.delegate.BaseDelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateListener;
import org.operaton.bpm.engine.impl.cmmn.model.CmmnActivity;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.CmmnModelElementInstance;
import org.operaton.bpm.model.cmmn.instance.ExtensionElements;
import org.operaton.bpm.model.cmmn.instance.operaton.OperatonCaseExecutionListener;

public abstract class AbstractExecutionListenerSpec {

  public static final String ANY_EVENT = "any";

  protected String eventNameToRegisterOn;
  protected Set<String> expectedRegisteredEvents;

  protected List<FieldSpec> fieldSpecs;

  protected AbstractExecutionListenerSpec(String eventName) {
    this.eventNameToRegisterOn = eventName;
    this.expectedRegisteredEvents = new HashSet<>();
    this.expectedRegisteredEvents.add(eventName);

    this.fieldSpecs = new ArrayList<>();
  }

  public void addListenerToElement(CmmnModelInstance modelInstance, CmmnModelElementInstance modelElement) {
    ExtensionElements extensionElements = SpecUtil.createElement(modelInstance, modelElement, null, ExtensionElements.class);
    OperatonCaseExecutionListener caseExecutionListener = SpecUtil.createElement(modelInstance, extensionElements, null, OperatonCaseExecutionListener.class);

    if (!ANY_EVENT.equals(eventNameToRegisterOn)) {
      caseExecutionListener.setOperatonEvent(eventNameToRegisterOn);
    }

    configureCaseExecutionListener(modelInstance, caseExecutionListener);

    for (FieldSpec fieldSpec : fieldSpecs) {
      fieldSpec.addFieldToListenerElement(modelInstance, caseExecutionListener);
    }
  }

  protected abstract void configureCaseExecutionListener(CmmnModelInstance modelInstance, OperatonCaseExecutionListener listener);

  public void verify(CmmnActivity activity) {

    assertThat(activity.getListeners()).hasSize(expectedRegisteredEvents.size());

    for (String expectedRegisteredEvent : expectedRegisteredEvents) {
      List<DelegateListener<? extends BaseDelegateExecution>> listeners = activity.getListeners(expectedRegisteredEvent);
      assertThat(listeners).hasSize(1);
      verifyListener(listeners.get(0));
    }
  }

  protected abstract void verifyListener(DelegateListener<? extends BaseDelegateExecution> listener);

  public AbstractExecutionListenerSpec expectRegistrationFor(List<String> events) {
    expectedRegisteredEvents = new HashSet<>(events);
    return this;
  }

  public AbstractExecutionListenerSpec withFieldExpression(String fieldName, String expression) {
    fieldSpecs.add(new FieldSpec(fieldName, expression, null, null, null));
    return this;
  }

  public AbstractExecutionListenerSpec withFieldChildExpression(String fieldName, String expression) {
    fieldSpecs.add(new FieldSpec(fieldName, null, expression, null, null));
    return this;
  }

  public AbstractExecutionListenerSpec withFieldStringValue(String fieldName, String value) {
    fieldSpecs.add(new FieldSpec(fieldName, null, null, value, null));
    return this;
  }

  public AbstractExecutionListenerSpec withFieldChildStringValue(String fieldName, String value) {
    fieldSpecs.add(new FieldSpec(fieldName, null, null, null, value));
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("{type=");
    sb.append(this.getClass().getSimpleName());
    sb.append(", event=");
    sb.append(eventNameToRegisterOn);
    sb.append("}");

    return sb.toString();
  }
}
