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
package org.operaton.bpm.integrationtest.functional.spin;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.spin.Spin;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class FeelEngineIT extends AbstractFoxPlatformIntegrationTest {

  protected static final String PATH = "org/operaton/bpm/integrationtest/functional/spin/feel/";

  protected static final String DMN_JSON = "feel-spin-json-decision.dmn";
  protected static final String DMN_XML = "feel-spin-xml-decision.dmn";
  protected static final String PROCESS_JSON = "feel-spin-json-process.bpmn";
  protected static final String PROCESS_XML = "feel-spin-xml-process.bpmn";

  protected static final List<String> TEST_LIST = Arrays.asList("\"foo\"", "\"bar\"");

  @Deployment(name="feel-deployment")
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
        .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
        .addAsResource(PATH + DMN_JSON, DMN_JSON)
        .addAsResource(PATH + DMN_XML, DMN_XML)
        .addAsResource(PATH + PROCESS_JSON, PROCESS_JSON)
        .addAsResource(PATH + PROCESS_XML, PROCESS_XML)
        .addClass(JsonListSerializable.class)
        .addClass(XmlListSerializable.class)
        .addClass(AbstractFoxPlatformIntegrationTest.class);
  }

  @Test
  public void shouldExecuteProcessWithJSONVariableCorrectly() {
    // given
    VariableMap varMap = createSpinParsedVariableInMap("JSON");

    // when
    runtimeService.startProcessInstanceByKey("feel-spin-json-process", varMap);

    // then
    HistoricDecisionInstance hdi = historyService.createHistoricDecisionInstanceQuery()
        .decisionDefinitionKey("feel-spin-json-decision")
        .includeOutputs()
        .singleResult();

    assertThat(hdi.getOutputs()).hasSize(1);
    assertThat(hdi.getOutputs().get(0).getValue()).isEqualTo(Boolean.TRUE);
  }

  @Test
  public void shouldExecuteProcessWithXMLVariableCorrectly() {
    // given
    VariableMap varMap = createSpinParsedVariableInMap("XML");

    // when
    runtimeService.startProcessInstanceByKey("feel-spin-xml-process", varMap);

    // then
    HistoricDecisionInstance hdi = historyService.createHistoricDecisionInstanceQuery()
        .decisionDefinitionKey("feel-spin-xml-decision")
        .includeOutputs()
        .singleResult();

    assertThat(hdi.getOutputs()).hasSize(1);
    assertThat(hdi.getOutputs().get(0).getValue()).isEqualTo(Boolean.TRUE);
  }

  // HELPER

  protected VariableMap createSpinParsedVariableInMap(String serializationType) {
    if ("json".equalsIgnoreCase(serializationType)) {
      JsonListSerializable<String> jsonList = new JsonListSerializable<>();
      jsonList.setListProperty(TEST_LIST);
      return Variables.createVariables()
                      .putValue("inputVar", Spin.JSON(jsonList.toExpectedJsonString()));

    } else if ("xml".equalsIgnoreCase(serializationType)) {
      XmlListSerializable<String> xmlList = new XmlListSerializable<>();
      xmlList.setListProperty(TEST_LIST);
      return Variables.createVariables()
                      .putValue("inputVar", Spin.XML(xmlList.toExpectedXmlString()));

    } else {
      return Variables.createVariables();

    }
  }
}
