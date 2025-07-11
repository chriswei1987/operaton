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
package org.operaton.bpm.model.bpmn.impl.instance.bpmndi;

import org.operaton.bpm.model.bpmn.impl.instance.di.LabeledEdgeImpl;
import org.operaton.bpm.model.bpmn.instance.BaseElement;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.operaton.bpm.model.bpmn.instance.bpmndi.BpmnLabel;
import org.operaton.bpm.model.bpmn.instance.bpmndi.MessageVisibleKind;
import org.operaton.bpm.model.bpmn.instance.di.DiagramElement;
import org.operaton.bpm.model.bpmn.instance.di.LabeledEdge;
import org.operaton.bpm.model.xml.ModelBuilder;
import org.operaton.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.operaton.bpm.model.xml.type.ModelElementTypeBuilder;
import org.operaton.bpm.model.xml.type.attribute.Attribute;
import org.operaton.bpm.model.xml.type.child.ChildElement;
import org.operaton.bpm.model.xml.type.child.SequenceBuilder;
import org.operaton.bpm.model.xml.type.reference.AttributeReference;

import static org.operaton.bpm.model.bpmn.impl.BpmnModelConstants.*;

/**
 * The BPMNDI BPMNEdge element
 *
 * @author Sebastian Menski
 */
public class BpmnEdgeImpl extends LabeledEdgeImpl implements BpmnEdge {

  protected static AttributeReference<BaseElement> bpmnElementAttribute;
  protected static AttributeReference<DiagramElement> sourceElementAttribute;
  protected static AttributeReference<DiagramElement> targetElementAttribute;
  protected static Attribute<MessageVisibleKind> messageVisibleKindAttribute;
  protected static ChildElement<BpmnLabel> bpmnLabelChild;

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(BpmnEdge.class, BPMNDI_ELEMENT_BPMN_EDGE)
      .namespaceUri(BPMNDI_NS)
      .extendsType(LabeledEdge.class)
      .instanceProvider(BpmnEdgeImpl::new);

    bpmnElementAttribute = typeBuilder.stringAttribute(BPMNDI_ATTRIBUTE_BPMN_ELEMENT)
      .qNameAttributeReference(BaseElement.class)
      .build();

    sourceElementAttribute = typeBuilder.stringAttribute(BPMNDI_ATTRIBUTE_SOURCE_ELEMENT)
      .qNameAttributeReference(DiagramElement.class)
      .build();

    targetElementAttribute = typeBuilder.stringAttribute(BPMNDI_ATTRIBUTE_TARGET_ELEMENT)
      .qNameAttributeReference(DiagramElement.class)
      .build();

    messageVisibleKindAttribute = typeBuilder.enumAttribute(BPMNDI_ATTRIBUTE_MESSAGE_VISIBLE_KIND, MessageVisibleKind.class)
      .build();

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    bpmnLabelChild = sequenceBuilder.element(BpmnLabel.class)
      .build();

    typeBuilder.build();
  }

  public BpmnEdgeImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public BaseElement getBpmnElement() {
    return bpmnElementAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setBpmnElement(BaseElement bpmnElement) {
    bpmnElementAttribute.setReferenceTargetElement(this, bpmnElement);
  }

  @Override
  public DiagramElement getSourceElement() {
    return sourceElementAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSourceElement(DiagramElement sourceElement) {
    sourceElementAttribute.setReferenceTargetElement(this, sourceElement);
  }

  @Override
  public DiagramElement getTargetElement() {
    return targetElementAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setTargetElement(DiagramElement targetElement) {
    targetElementAttribute.setReferenceTargetElement(this, targetElement);
  }

  @Override
  public MessageVisibleKind getMessageVisibleKind() {
    return messageVisibleKindAttribute.getValue(this);
  }

  @Override
  public void setMessageVisibleKind(MessageVisibleKind messageVisibleKind) {
    messageVisibleKindAttribute.setValue(this, messageVisibleKind);
  }

  @Override
  public BpmnLabel getBpmnLabel() {
    return bpmnLabelChild.getChild(this);
  }

  @Override
  public void setBpmnLabel(BpmnLabel bpmnLabel) {
    bpmnLabelChild.setChild(this, bpmnLabel);
  }
}
