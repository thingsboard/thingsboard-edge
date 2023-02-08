///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import {
  Converter,
  ConverterDebugInput,
  TestConverterResult,
  TestDownLinkInputParams,
  TestUpLinkInputParams
} from '@shared/models/converter.models';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { ScriptLanguage } from '@shared/models/rule-node.models';

@Injectable({
  providedIn: 'root'
})
export class ConverterService {

  constructor(
    private http: HttpClient
  ) { }

  public getConverters(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Converter>> {
    return this.getConvertersByEdgeTemplate(pageLink, false, config);
  }

  public getConvertersByEdgeTemplate(pageLink: PageLink, isEdgeTemplate: boolean, config?: RequestConfig): Observable<PageData<Converter>> {
    return this.http.get<PageData<Converter>>(`/api/converters${pageLink.toQuery()}&isEdgeTemplate=${isEdgeTemplate}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getConvertersByIds(converterIds: Array<string>, config?: RequestConfig): Observable<Array<Converter>> {
    return this.http.get<Array<Converter>>(`/api/converters?converterIds=${converterIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((converters) => sortEntitiesByIds(converters, converterIds))
    );
  }

  public getConverter(converterId: string, config?: RequestConfig): Observable<Converter> {
    return this.http.get<Converter>(`/api/converter/${converterId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveConverter(converter: Converter, config?: RequestConfig): Observable<Converter> {
    return this.http.post<Converter>('/api/converter', converter, defaultHttpOptionsFromConfig(config));
  }

  public deleteConverter(converterId: string, config?: RequestConfig) {
    return this.http.delete(`/api/converter/${converterId}`, defaultHttpOptionsFromConfig(config));
  }

  public testUpLink(inputParams: TestUpLinkInputParams, scriptLang?: ScriptLanguage,
                    config?: RequestConfig): Observable<TestConverterResult> {
    let url = '/api/converter/testUpLink';
    if (scriptLang) {
      url += `?scriptLang=${scriptLang}`;
    }
    return this.http.post<TestConverterResult>(url, inputParams, defaultHttpOptionsFromConfig(config));
  }

  public testDownLink(inputParams: TestDownLinkInputParams, scriptLang?: ScriptLanguage,
                      config?: RequestConfig): Observable<TestConverterResult> {
    let url = '/api/converter/testDownLink';
    if (scriptLang) {
      url += `?scriptLang=${scriptLang}`;
    }
    return this.http.post<TestConverterResult>(url, inputParams, defaultHttpOptionsFromConfig(config));
  }

  public getLatestConverterDebugInput(converterId: string, config?: RequestConfig): Observable<ConverterDebugInput> {
    return this.http.get<ConverterDebugInput>(`/api/converter/${converterId}/debugIn`, defaultHttpOptionsFromConfig(config));
  }

}
