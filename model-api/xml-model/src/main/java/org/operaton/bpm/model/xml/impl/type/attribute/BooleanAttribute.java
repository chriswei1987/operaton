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
package org.operaton.bpm.model.xml.impl.type.attribute;

import org.operaton.bpm.model.xml.impl.util.ModelUtil;
import org.operaton.bpm.model.xml.type.ModelElementType;

/**
 * <p>class for providing Boolean value attributes. Takes care of type conversion and
 * the interaction with the underlying Xml model model.</p>
 *
 * @author Daniel Meyer
 *
 */
public class BooleanAttribute extends AttributeImpl<Boolean> {

  public BooleanAttribute(ModelElementType owningElementType) {
    super(owningElementType);
  }

  @Override
  protected Boolean convertXmlValueToModelValue(String rawValue) {
    return ModelUtil.valueAsBoolean(rawValue);
  }

  @Override
  protected String convertModelValueToXmlValue(Boolean modelValue) {
    return ModelUtil.valueAsString(modelValue);
  }

}
