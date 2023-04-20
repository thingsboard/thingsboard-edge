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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { DebugEventType, Event, EventType, FilterEventBody } from '@shared/models/event.models';

@Injectable({
  providedIn: 'root'
})
export class EventService {

  constructor(
    private http: HttpClient
  ) { }

  public getEvents(entityId: EntityId, eventType: EventType | DebugEventType, tenantId: string, pageLink: TimePageLink,
                   config?: RequestConfig): Observable<PageData<Event>> {
    return this.http.get<PageData<Event>>(`/api/events/${entityId.entityType}/${entityId.id}/${eventType}` +
              `${pageLink.toQuery()}&tenantId=${tenantId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getFilterEvents(entityId: EntityId, eventType: EventType | DebugEventType, tenantId: string,
                         filters: FilterEventBody, pageLink: TimePageLink, config?: RequestConfig): Observable<PageData<Event>> {
    return this.http.post<PageData<Event>>(`/api/events/${entityId.entityType}/${entityId.id}` +
      `${pageLink.toQuery()}&tenantId=${tenantId}`, {...filters, eventType}, defaultHttpOptionsFromConfig(config));
  }

  public clearEvents(entityId: EntityId, eventType: EventType | DebugEventType, filters: FilterEventBody, tenantId: string,
                     pageLink: TimePageLink, config?: RequestConfig) {
    return this.http.post(`/api/events/${entityId.entityType}/${entityId.id}/clear?tenantId=${tenantId}` +
      (pageLink.startTime ? `&startTime=${pageLink.startTime}` : ``) +
      (pageLink.endTime ? `&endTime=${pageLink.endTime}` : ``), {...filters, eventType},
      defaultHttpOptionsFromConfig(config));
  }
}
