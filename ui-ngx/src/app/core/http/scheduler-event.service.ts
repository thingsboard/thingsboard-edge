///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import {
  SchedulerEvent,
  SchedulerEventInfo,
  SchedulerEventWithCustomerInfo
} from '@shared/models/scheduler-event.models';
import { isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class SchedulerEventService {

  constructor(
    private http: HttpClient,
  ) {
  }

  public getSchedulerEvents(type: string = '', config?: RequestConfig): Observable<Array<SchedulerEventWithCustomerInfo>> {
    let url = '/api/schedulerEvents';
    if (isDefinedAndNotNull(type)) {
      url += `?type=${type}`;
    }
    return this.http.get<Array<SchedulerEventWithCustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getSchedulerEventsByIds(schedulerEventIds: Array<string>, config?: RequestConfig): Observable<Array<SchedulerEventInfo>> {
    return this.http.get<Array<SchedulerEventInfo>>(`/api/schedulerEvents?schedulerEventIds=${schedulerEventIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((schedulerEvents) => sortEntitiesByIds(schedulerEvents, schedulerEventIds))
    );
  }

  public getSchedulerEventInfo(schedulerEventId: string, config?: RequestConfig): Observable<SchedulerEventWithCustomerInfo> {
    return this.http.get<SchedulerEventWithCustomerInfo>(`/api/schedulerEvent/info/${schedulerEventId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getSchedulerEvent(schedulerEventId: string, config?: RequestConfig): Observable<SchedulerEvent> {
    return this.http.get<SchedulerEvent>(`/api/schedulerEvent/${schedulerEventId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveSchedulerEvent(schedulerEvent: SchedulerEvent, config?: RequestConfig): Observable<SchedulerEvent> {
    return this.http.post<SchedulerEvent>('/api/schedulerEvent', schedulerEvent, defaultHttpOptionsFromConfig(config));
  }

  public deleteSchedulerEvent(schedulerEventId: string, config?: RequestConfig) {
    return this.http.delete(`/api/schedulerEvent/${schedulerEventId}`, defaultHttpOptionsFromConfig(config));
  }

}
