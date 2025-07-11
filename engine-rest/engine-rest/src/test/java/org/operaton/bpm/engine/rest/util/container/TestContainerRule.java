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
package org.operaton.bpm.engine.rest.util.container;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Thorben Lindhauer
 *
 */
public class TestContainerRule implements TestRule {

  private static final Logger LOGGER = Logger.getLogger(TestContainerRule.class.getSimpleName());

  protected ContainerSpecifics containerSpecifics;

  @Override
  public Statement apply(Statement base, Description description) {

    lookUpContainerSpecifics();
    TestRule containerSpecificRule = containerSpecifics.getTestRule(description.getTestClass());
    return containerSpecificRule.apply(base, description);
  }

  protected void lookUpContainerSpecifics() {

    if (this.containerSpecifics == null) {
      ServiceLoader<ContainerSpecifics> serviceLoader = ServiceLoader.load(ContainerSpecifics.class);
      Iterator<ContainerSpecifics> it = serviceLoader.iterator();

      if (it.hasNext()) {
        this.containerSpecifics = it.next();

        if (it.hasNext()) {
          LOGGER.warning("There is more than one test runtime container implementation present on the classpath. "
              + "Using " + this.containerSpecifics.getClass().getName());
        }
      }
      else {
        throw new RuntimeException("Could not find container provider SPI that implements " + ContainerSpecifics.class.getName());
      }
    }

  }
}

