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
package org.operaton.bpm.engine.history;


/**
 * @author Roman Smirnov
 *
 */
public interface JobState {

  JobState CREATED = new JobStateImpl(0, "created");
  JobState FAILED = new JobStateImpl(1, "failed");
  JobState SUCCESSFUL = new JobStateImpl(2, "successful");
  JobState DELETED = new JobStateImpl(3, "deleted");

  int getStateCode();

  ///////////////////////////////////////////////////// default implementation

  class JobStateImpl implements JobState {

    public final int stateCode;
    protected final String name;

    public JobStateImpl(int stateCode, String string) {
      this.stateCode = stateCode;
      this.name = string;
    }

    @Override
    public int getStateCode() {
      return stateCode;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + stateCode;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      JobStateImpl other = (JobStateImpl) obj;
      if (stateCode != other.stateCode)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
