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
package org.operaton.bpm.engine.rest.dto.telemetry;

import org.operaton.bpm.engine.telemetry.TelemetryData;

public class TelemetryDataDto {

  protected String installation;
  protected ProductDto product;

  public TelemetryDataDto(String installation, ProductDto product) {
    this.installation = installation;
    this.product = product;
  }

  public String getInstallation() {
    return installation;
  }

  public void setInstallation(String installation) {
    this.installation = installation;
  }

  public ProductDto getProduct() {
    return product;
  }

  public void setProduct(ProductDto product) {
    this.product = product;
  }

  public static TelemetryDataDto fromEngineDto(TelemetryData other) {
    return new TelemetryDataDto(
        other.getInstallation(),
        ProductDto.fromEngineDto(other.getProduct()));
  }
}
