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
package org.operaton.bpm.model.cmmn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.operaton.bpm.model.cmmn.CmmnModelException;
import org.operaton.bpm.model.cmmn.Query;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;

/**
 * @author Sebastian Menski
 */
public class QueryImpl<T extends ModelElementInstance> implements Query<T> {

  private final Collection<T> collection;

  public QueryImpl(Collection<T> collection) {
    this.collection = collection;
  }

  @Override
  public List<T> list() {
    return new ArrayList<>(collection);
  }

  @Override
  public int count() {
    return collection.size();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V extends ModelElementInstance> Query<V> filterByType(ModelElementType elementType) {
    Class<V> elementClass = (Class<V>) elementType.getInstanceType();
    return filterByType(elementClass);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V extends ModelElementInstance> Query<V> filterByType(Class<V> elementClass) {
    List<V> filtered = new ArrayList<>();
    for (T instance : collection) {
      if (elementClass.isAssignableFrom(instance.getClass())) {
        filtered.add((V) instance);
      }
    }
    return new QueryImpl<>(filtered);
  }

  @Override
  public T singleResult() {
    if (collection.size() == 1) {
      return collection.iterator().next();
    }
    else {
      throw new CmmnModelException("Collection expected to have <1> entry but has <" + collection.size() + ">");
    }
  }
}
