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
package org.operaton.bpm.engine.impl.form.validator;

import org.operaton.bpm.engine.impl.form.FormException;

/**
 * Runtime exception for use within a {@linkplain FormFieldValidator}.
 * Optionally contains a detail which uniquely identifies the problem.
 *
 * @author Thomas Skjolberg
 */
public class FormFieldValidationException extends FormException {

  private static final long serialVersionUID = 1L;

  /** optional object for detailing the nature of the validation error */
  protected final Object detail;

  public FormFieldValidationException() {
    super();

    this.detail = null;
  }

  public FormFieldValidationException(Object detail) {
    super();

    this.detail = detail;
  }

  public FormFieldValidationException(Object detail, String message, Throwable cause) {
    super(message, cause);

    this.detail = detail;
  }

  public FormFieldValidationException(Object detail, String message) {
    super(message);

    this.detail = detail;
  }

  public FormFieldValidationException(Object detail, Throwable cause) {
    super(cause);

    this.detail = detail;
  }

  @SuppressWarnings("unchecked")
  public <T> T getDetail() {
    return (T) detail;
  }
}
