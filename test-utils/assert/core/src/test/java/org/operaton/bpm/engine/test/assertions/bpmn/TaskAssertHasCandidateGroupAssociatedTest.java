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
package org.operaton.bpm.engine.test.assertions.bpmn;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.junit.jupiter.api.Test;

class TaskAssertHasCandidateGroupAssociatedTest extends ProcessAssertTestCase {

  private static final String CANDIDATE_GROUP = "candidateGroup";
  private static final String ASSIGNEE = "assignee";

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedPreDefinedSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // Then
    assertThat(processInstance).task().hasCandidateGroupAssociated("candidateGroup");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedPreDefinedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("candidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedPredefinedRemovedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), "candidateGroup");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("candidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedPreDefinedOtherFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), "candidateGroup");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("otherCandidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedExplicitlySetSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    assertThat(processInstance).task().hasCandidateGroupAssociated("explicitCandidateGroupId");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedExplicitlySetFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("candidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedExplicitlySetRemovedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // When
    taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("explicitCandidateGroupId"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedExplicitlySetOtherFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // When
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("otherCandidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedMoreThanOneSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    assertThat(processInstance).task().hasCandidateGroupAssociated("candidateGroup");
    // And
    assertThat(processInstance).task().hasCandidateGroupAssociated("explicitCandidateGroupId");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedMoreThanOneFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated("otherCandidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedNullFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroupAssociated(null));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedNonExistingTaskFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    final Task task = taskQuery().singleResult();
    complete(task);
    // Then
    expect(() -> assertThat(task).hasCandidateGroupAssociated("candidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedAssignedSuccess() {
    // Given
    final ProcessInstance pi = runtimeService().startProcessInstanceByKey(
        "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    claim(task(pi), ASSIGNEE);
    // Then
    assertThat(task(pi)).hasCandidateGroupAssociated(CANDIDATE_GROUP);
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroupAssociated.bpmn"
  })
  void hasCandidateGroupAssociatedAssignedFailure() {
    // Given
    final ProcessInstance pi = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroupAssociated"
    );
    // When
    taskService().deleteCandidateGroup(task(pi).getId(), CANDIDATE_GROUP);
    // And
    claim(task(pi), ASSIGNEE);
    // Then
    expect(() -> assertThat(task(pi)).hasCandidateGroupAssociated(CANDIDATE_GROUP));
  }

}
