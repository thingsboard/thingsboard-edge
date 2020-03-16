///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import {
  SchedulerEvent,
  SchedulerEventInfo,
  SchedulerEventWithCustomerInfo
} from '@shared/models/scheduler-event.models';

@Injectable({
  providedIn: 'root'
})
export class SchedulerEventService {

  constructor(
    private http: HttpClient,
  ) {
  }

  public getSchedulerEvents(type: string = '', config?: RequestConfig): Observable<Array<SchedulerEventWithCustomerInfo>> {
    return this.http.get<Array<SchedulerEventWithCustomerInfo>>(`/api/schedulerEvents&type=${type}`,
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
    return this.http.post<SchedulerEvent>('api/schedulerEvent', schedulerEvent, defaultHttpOptionsFromConfig(config));
  }

  public deleteSchedulerEvent(schedulerEventId: string, config?: RequestConfig) {
    return this.http.delete(`/api/schedulerEvent/${schedulerEventId}`, defaultHttpOptionsFromConfig(config));
  }

}
