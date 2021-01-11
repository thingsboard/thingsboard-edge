///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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

@Injectable({
  providedIn: 'root'
})
export class ConverterService {

  constructor(
    private http: HttpClient
  ) { }

  public getConverters(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Converter>> {
    return this.http.get<PageData<Converter>>(`/api/converters${pageLink.toQuery()}`,
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

  public testUpLink(inputParams: TestUpLinkInputParams, config?: RequestConfig): Observable<TestConverterResult> {
    return this.http.post<TestConverterResult>('/api/converter/testUpLink', inputParams, defaultHttpOptionsFromConfig(config));
  }

  public testDownLink(inputParams: TestDownLinkInputParams, config?: RequestConfig): Observable<TestConverterResult> {
    return this.http.post<TestConverterResult>('/api/converter/testDownLink', inputParams, defaultHttpOptionsFromConfig(config));
  }

  public getLatestConverterDebugInput(converterId: string, config?: RequestConfig): Observable<ConverterDebugInput> {
    return this.http.get<ConverterDebugInput>(`/api/converter/${converterId}/debugIn`, defaultHttpOptionsFromConfig(config));
  }

}
