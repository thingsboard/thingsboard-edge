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
import { CustomTranslationEditData, TranslationInfo } from '@shared/models/custom-translation.model';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { ResourcesService } from '@core/services/resources.service';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class CustomTranslationService {

  constructor(
    private http: HttpClient,
    private resourcesService: ResourcesService
  ) {}

  public getAvailableLocales(config?: RequestConfig): Observable<{[k: string]: string}> {
    return this.http.get<{[k: string]: string}>('/api/translation/availableLocales', defaultHttpOptionsFromConfig(config))
  }

  public getAvailableJavaLocales(config?: RequestConfig): Observable<{[k: string]: string}> {
    return this.http.get<{[k: string]: string}>('/api/translation/availableJavaLocales', defaultHttpOptionsFromConfig(config))
  }

  public getTranslationInfos(config?: RequestConfig): Observable<Array<TranslationInfo>> {
    return this.http.get<Array<TranslationInfo>>('/api/translation/info', defaultHttpOptionsFromConfig(config))
  }

  public getTranslationForBasicEdit(localeCode: string, config?: RequestConfig): Observable<CustomTranslationEditData> {
    return this.http.get<CustomTranslationEditData>(`/api/translation/edit/basic/${localeCode}`, defaultHttpOptionsFromConfig(config))
  }

  public getFullTranslation(localeCode: string, config?: RequestConfig): Observable<object> {
    return this.http.get<object>(`/api/translation/full/${localeCode}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadFullTranslation(localeCode: string, config?: RequestConfig): Observable<object> {
    return this.resourcesService.downloadResource(`/api/translation/full/${localeCode}/download`, config);
  }

  public deleteCustomTranslation(localeCode: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/translation/custom/${localeCode}`, defaultHttpOptionsFromConfig(config))
  }

  public deleteCustomTranslationKey(localesCode: string, key: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/translation/custom/${localesCode}/${key}`, defaultHttpOptionsFromConfig(config))
  }

  public saveCustomTranslation(localeCode: string, customTranslationValue: object, config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/translation/custom/${localeCode}`, customTranslationValue, defaultHttpOptionsFromConfig(config))
  }

  public getCustomTranslation(localeCode: string, config?: RequestConfig): Observable<object> {
    return this.http.get<object>(`/api/translation/custom/${localeCode}`, defaultHttpOptionsFromConfig(config))
  }

  public patchCustomTranslation(localeCode: string, newCustomTranslation: object, config?: RequestConfig): Observable<void> {
    return this.http.patch<void>(`/api/translation/custom/${localeCode}`, newCustomTranslation, defaultHttpOptionsFromConfig(config))
  }
}
