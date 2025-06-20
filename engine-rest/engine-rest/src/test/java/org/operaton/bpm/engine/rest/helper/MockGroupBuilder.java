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
package org.operaton.bpm.engine.rest.helper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.operaton.bpm.engine.identity.Group;

public class MockGroupBuilder {

  protected String id;
  protected String name;
  protected String type;

  public MockGroupBuilder id(String id) {
    this.id = id;
    return this;
  }

  public MockGroupBuilder name(String name) {
    this.name = name;
    return this;
  }

  public MockGroupBuilder type(String type) {
    this.type = type;
    return this;
  }

  @SuppressWarnings("unchecked")
  public Group build() {
    Group group = mock(Group.class);
    when(group.getId()).thenReturn(id);
    when(group.getName()).thenReturn(name);
    when(group.getType()).thenReturn(type);
    return group;
  }

}
