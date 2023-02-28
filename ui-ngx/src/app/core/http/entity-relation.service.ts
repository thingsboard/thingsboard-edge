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
import { EntityRelation, EntityRelationInfo, EntityRelationsQuery } from '@shared/models/relation.models';
import { EntityId } from '@app/shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class EntityRelationService {

  constructor(
    private http: HttpClient
  ) { }

  public saveRelation(relation: EntityRelation, config?: RequestConfig): Observable<EntityRelation> {
    return this.http.post<EntityRelation>('/api/relation', relation, defaultHttpOptionsFromConfig(config));
  }

  public deleteRelation(fromId: EntityId, relationType: string, toId: EntityId,
                        config?: RequestConfig) {
    return this.http.delete(`/api/relation?fromId=${fromId.id}&fromType=${fromId.entityType}` +
      `&relationType=${relationType}&toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public deleteRelations(entityId: EntityId,
                         config?: RequestConfig) {
    return this.http.delete(`/api/relations?entityId=${entityId.id}&entityType=${entityId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRelation(fromId: EntityId, relationType: string, toId: EntityId,
                     config?: RequestConfig): Observable<EntityRelation> {
    return this.http.get<EntityRelation>(`/api/relation?fromId=${fromId.id}&fromType=${fromId.entityType}` +
      `&relationType=${relationType}&toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByFrom(fromId: EntityId,
                    config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?fromId=${fromId.id}&fromType=${fromId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findInfoByFrom(fromId: EntityId,
                        config?: RequestConfig): Observable<Array<EntityRelationInfo>> {
    return this.http.get<Array<EntityRelationInfo>>(
      `/api/relations/info?fromId=${fromId.id}&fromType=${fromId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByFromAndType(fromId: EntityId, relationType: string,
                           config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?fromId=${fromId.id}&fromType=${fromId.entityType}&relationType=${relationType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByTo(toId: EntityId,
                  config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findInfoByTo(toId: EntityId,
                      config?: RequestConfig): Observable<Array<EntityRelationInfo>> {
    return this.http.get<Array<EntityRelationInfo>>(
      `/api/relations/info?toId=${toId.id}&toType=${toId.entityType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByToAndType(toId: EntityId, relationType: string,
                         config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.get<Array<EntityRelation>>(
      `/api/relations?toId=${toId.id}&toType=${toId.entityType}&relationType=${relationType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public findByQuery(query: EntityRelationsQuery,
                     config?: RequestConfig): Observable<Array<EntityRelation>> {
    return this.http.post<Array<EntityRelation>>(
      '/api/relations', query,
      defaultHttpOptionsFromConfig(config));
  }

  public findInfoByQuery(query: EntityRelationsQuery,
                         config?: RequestConfig): Observable<Array<EntityRelationInfo>> {
    return this.http.post<Array<EntityRelationInfo>>(
      '/api/relations/info', query,
      defaultHttpOptionsFromConfig(config));
  }

}
