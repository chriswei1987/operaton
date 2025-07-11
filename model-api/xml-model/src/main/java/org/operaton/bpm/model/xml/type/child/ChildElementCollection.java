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
package org.operaton.bpm.model.xml.type.child;

import org.operaton.bpm.model.xml.Model;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.ModelElementType;

import java.util.Collection;

/**
 * <p>A collection containing all or a subset of the child
 * elements of a given {@link ModelElementInstance}.</p>
 *
 * @author Daniel Meyer
 *
 * @param <T> The type of the model elements in the collection
 */
public interface ChildElementCollection<T extends ModelElementInstance> {

  /**
   * Indicates whether the collection is immutable.
   *
   * If the collection is immutable, all state-altering operations such
   * as {@link Collection#add(Object)} or {@link Collection#remove(Object)}
   * will throw an {@link UnsupportedOperationException}.
   *
   * @return true if the collection is mutable, false otherwise.
   */
  boolean isImmutable();

  /**
   * Indicates the minimal element count of a collection. Returns a positive integer or '0'.
   * @return the minimal element count of the collection.
   */
  int getMinOccurs();

  /**
   * Indicates the max element count of a collection. In  a negative value is returned (like '-1'),
   * the collection is unbounded.
   *
   * @return the max element count for this collection.
   */
  int getMaxOccurs();

  /**
   * Get the model element type of the elements contained in this collection.
   *
   * @param model  the model of the element
   * @return the containing {@link ModelElementType}
   */
  ModelElementType getChildElementType(Model model);

  /**
   * Get the class of the elements contained in this collection.
   *
   * @return the class of contained types
   */
  Class<T> getChildElementTypeClass();

  /**
   * Get the model element type of the element owns the collection
   *
   * @return the parent element of the collection
   */
  ModelElementType getParentElementType();

  /** returns a {@link Collection} containing all or a subset of the child elements of
   * a  {@link ModelElementInstance}.  */
  Collection<T> get(ModelElementInstance element);

}
