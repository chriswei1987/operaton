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
package org.operaton.bpm.model.xml.impl.type.reference;

import org.operaton.bpm.model.xml.ModelException;
import org.operaton.bpm.model.xml.ModelReferenceException;
import org.operaton.bpm.model.xml.impl.ModelInstanceImpl;
import org.operaton.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.operaton.bpm.model.xml.instance.ModelElementInstance;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.reference.ElementReference;

/**
 * @author Sebastian Menski
 */
public class ElementReferenceImpl<Target extends ModelElementInstance, Source extends ModelElementInstance>  extends ElementReferenceCollectionImpl<Target,Source> implements ElementReference<Target, Source> {


  public ElementReferenceImpl(ChildElement<Source> referenceSourceCollection) {
    super(referenceSourceCollection);
  }

  private ChildElement<Source> getReferenceSourceChild() {
    return (ChildElement<Source>) getReferenceSourceCollection();
  }

  @Override
  public Source getReferenceSource(ModelElementInstance referenceSourceParent) {
    return getReferenceSourceChild().getChild(referenceSourceParent);
  }

  private void setReferenceSource(ModelElementInstance referenceSourceParent, Source referenceSource) {
    getReferenceSourceChild().setChild(referenceSourceParent, referenceSource);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Target getReferenceTargetElement(ModelElementInstanceImpl referenceSourceParentElement) {
    Source referenceSource = getReferenceSource(referenceSourceParentElement);
    if (referenceSource != null) {
      String identifier = getReferenceIdentifier(referenceSource);
      ModelElementInstance referenceTargetElement = referenceSourceParentElement.getModelInstance().getModelElementById(identifier);
      if (referenceTargetElement != null) {
        return (Target) referenceTargetElement;
      }
      else {
        throw new ModelException("Unable to find a model element instance for id " + identifier);
      }
    }
    else {
      return null;
    }
  }

  @Override
  public void setReferenceTargetElement(ModelElementInstanceImpl referenceSourceParentElement, Target referenceTargetElement) {
    ModelInstanceImpl modelInstance = referenceSourceParentElement.getModelInstance();
    String identifier = referenceTargetAttribute.getValue(referenceTargetElement);
    ModelElementInstance existingElement = modelInstance.getModelElementById(identifier);

    if (existingElement == null || !existingElement.equals(referenceTargetElement)) {
      throw new ModelReferenceException("Cannot create reference to model element " + referenceTargetElement
        +": element is not part of model. Please connect element to the model first.");
    }
    else {
      Source referenceSourceElement = modelInstance.newInstance(getReferenceSourceElementType());
      setReferenceSource(referenceSourceParentElement, referenceSourceElement);
      setReferenceIdentifier(referenceSourceElement, identifier);
    }
  }

  @Override
  public void clearReferenceTargetElement(ModelElementInstanceImpl referenceSourceParentElement) {
    getReferenceSourceChild().removeChild(referenceSourceParentElement);
  }
}
