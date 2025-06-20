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
package org.operaton.bpm.model.bpmn.impl.instance;

import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.DataInput;
import org.operaton.bpm.model.bpmn.instance.InputSet;
import org.operaton.bpm.model.bpmn.instance.OutputSet;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_INPUT_SET;

/**
 * The BPMN inputSet element
 *
 * @author Sebastian Menski
 */
public class InputSetImpl extends BaseElementImpl implements InputSet {

  protected static Attribute<String> nameAttribute;
  protected static ElementReferenceCollection<DataInput, DataInputRefs> dataInputDataInputRefsCollection;
  protected static ElementReferenceCollection<DataInput, OptionalInputRefs> optionalInputRefsCollection;
  protected static ElementReferenceCollection<DataInput, WhileExecutingInputRefs> whileExecutingInputRefsCollection;
  protected static ElementReferenceCollection<OutputSet, OutputSetRefs> outputSetOutputSetRefsCollection;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(InputSet.class, BPMN_ELEMENT_INPUT_SET)
      .namespaceUri(BPMN20_NS)
      .extendsType(BaseElement.class)
      .instanceProvider(InputSetImpl::new);

    nameAttribute = typeBuilder.stringAttribute("name")
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataInputDataInputRefsCollection = sequenceBuilder.elementCollection(DataInputRefs.class)
      .idElementReferenceCollection(DataInput.class)
      .build();

    optionalInputRefsCollection = sequenceBuilder.elementCollection(OptionalInputRefs.class)
      .idElementReferenceCollection(DataInput.class)
      .build();

    whileExecutingInputRefsCollection = sequenceBuilder.elementCollection(WhileExecutingInputRefs.class)
      .idElementReferenceCollection(DataInput.class)
      .build();

    outputSetOutputSetRefsCollection = sequenceBuilder.elementCollection(OutputSetRefs.class)
      .idElementReferenceCollection(OutputSet.class)
      .build();

    typeBuilder.build();
  }

  public InputSetImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public Collection<DataInput> getDataInputs() {
    return dataInputDataInputRefsCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<DataInput> getOptionalInputs() {
    return optionalInputRefsCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<DataInput> getWhileExecutingInput() {
    return whileExecutingInputRefsCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<OutputSet> getOutputSets() {
    return outputSetOutputSetRefsCollection.getReferenceTargetElements(this);
  }
}
