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
package org.operaton.bpm.qa.upgrade.variables;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.variable.serializer.jpa.EntityManagerSession;
import org.operaton.bpm.engine.impl.variable.serializer.jpa.EntityManagerSessionFactory;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.qa.upgrade.DescribesScenario;
import org.operaton.bpm.qa.upgrade.ScenarioSetup;

public class JpaVariableScenario {

  protected static FieldAccessJPAEntity simpleEntityFieldAccess;
  protected static EntityManagerFactory entityManagerFactory;

  private JpaVariableScenario() {
  }

  @Deployment
  public static String deployOneTask() {
    return "org/operaton/bpm/qa/upgrade/variables/JpaEntitiesScenario.oneTaskProcess.bpmn20.xml";
  }

  @DescribesScenario("createJpaVariable")
  public static ScenarioSetup createJpaVariable() {
    return (engine, scenarioName) -> {
      EntityManagerSessionFactory entityManagerSessionFactory = (EntityManagerSessionFactory) ((ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration())
          .getSessionFactories()
          .get(EntityManagerSession.class);
      entityManagerFactory = entityManagerSessionFactory.getEntityManagerFactory();

      setupJPAEntities();

      Map<String, Object> variables = new HashMap<>();
      variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

      //
      String id = engine.getRuntimeService().startProcessInstanceByKey("JPAVariableProcess", variables).getId();
      engine.getManagementService().setProperty("JpaEntitiesScenario.processInstanceId", id);

      entityManagerFactory.close();
      entityManagerFactory = null;
    };
  }

  public static void setupJPAEntities() {

    EntityManager manager = entityManagerFactory.createEntityManager();
    manager.getTransaction().begin();

    // Simple test data
    simpleEntityFieldAccess = new FieldAccessJPAEntity();
    simpleEntityFieldAccess.setId(1L);
    simpleEntityFieldAccess.setMyValue("value1");
    manager.persist(simpleEntityFieldAccess);

    manager.flush();
    manager.getTransaction().commit();
    manager.close();
  }
}
