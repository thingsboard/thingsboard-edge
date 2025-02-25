///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Converter, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class ConverterLibraryService {

  private readonly baseUrl = '/api/converter/library';

  constructor(
    private http: HttpClient
  ) {
  }

  getVendors(integrationType: IntegrationType, config?: RequestConfig): Observable<Vendor[]> {
    return this.http.get(`${this.baseUrl}/${integrationType}/vendors`, defaultHttpOptionsFromConfig(config)) as Observable<Vendor[]>;
  }

  getModels(
    integrationType: IntegrationType,
    vendorName: string,
    converterType: ConverterType,
    config?: RequestConfig
  ): Observable<Model[]> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${vendorName}/models?converterType=${converterType.toLowerCase()}`,
      defaultHttpOptionsFromConfig(config)
    ) as Observable<Model[]>;
  }

  getConverter(
    integrationType: IntegrationType,
    vendorName: string, modelName: string,
    converterType: ConverterType,
    config?: RequestConfig
  ): Observable<Converter> {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${vendorName}/${modelName}/${converterType.toLowerCase()}`,
      defaultHttpOptionsFromConfig(config)
    ) as Observable<Converter>;
  }

  getConverterMetaData(
    integrationType: IntegrationType,
    vendorName: string,
    modelName: string,
    converterType: ConverterType,
    config?: RequestConfig
  ) {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${vendorName}/${modelName}/${converterType.toLowerCase()}/metadata`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getConverterPayload(
    integrationType: IntegrationType,
    vendorName: string,
    modelName: string,
    converterType: ConverterType,
    config?: RequestConfig
  ) {
    return this.http.get(
      `${this.baseUrl}/${integrationType}/${vendorName}/${modelName}/${converterType.toLowerCase()}/payload`,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
