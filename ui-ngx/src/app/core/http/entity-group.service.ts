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
import { defaultPageLinkSearchFunction, PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { ContactBased } from '@shared/models/contact-based.model';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityGroup, EntityGroupInfo, ShortEntityView } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { map } from 'rxjs/operators';
import { BaseData, HasId, sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class EntityGroupService {

  constructor(
    private http: HttpClient
  ) {}

  public getOwners(pageLink: PageLink, config?: RequestConfig): Observable<PageData<ContactBased<EntityId>>> {
    return this.http.get<PageData<ContactBased<EntityId>>>(`/api/owners${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroup(entityGroupId: string, config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.get<EntityGroupInfo>(`/api/entityGroup/${entityGroupId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveEntityGroup(entityGroup: EntityGroup, config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.post<EntityGroupInfo>('/api/entityGroup', entityGroup, defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityGroup(entityGroupId: string, config?: RequestConfig) {
    return this.http.delete(`/api/entityGroup/${entityGroupId}`, defaultHttpOptionsFromConfig(config));
  }

  public makeEntityGroupPublic(entityGroupId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/makePublic`, null, defaultHttpOptionsFromConfig(config));
  }

  public makeEntityGroupPrivate(entityGroupId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/makePrivate`, null, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroups(groupType: EntityType, config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    return this.http.get<Array<EntityGroupInfo>>(`/api/entityGroups/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupIdsForEntityId(entityType: EntityType, entityId: string, config?: RequestConfig): Observable<Array<EntityGroupId>> {
    return this.http.get<Array<EntityGroupId>>(`/api/entityGroups/${entityType}/${entityId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupsByIds(entityGroupIds: Array<string>, config?: RequestConfig): Observable<Array<EntityGroup>> {
    return this.http.get<Array<EntityGroup>>(`/api/entityGroups?entityGroupIds=${entityGroupIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((entityGroups) => sortEntitiesByIds(entityGroups, entityGroupIds))
    );
  }

  public getEntityGroupsByOwnerId(ownerType: EntityType, ownerId: string, groupType: EntityType,
                                  config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    return this.http.get<Array<EntityGroupInfo>>(`/api/entityGroups/${ownerType}/${ownerId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupAllByOwnerId(ownerType: EntityType, ownerId: string, groupType: EntityType,
                                    config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.get<EntityGroupInfo>(`/api/entityGroup/all/${ownerType}/${ownerId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupsByPageLink(pageLink: PageLink, groupType: EntityType,
                                   config?: RequestConfig): Observable<PageData<EntityGroupInfo>> {
    return this.getEntityGroups(groupType, config).pipe(
      map((entityGroups) => pageLink.filterData(entityGroups, defaultPageLinkSearchFunction( 'name'))));
  }

  public addEntityToEntityGroup(entityGroupId: string, entityId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/addEntities`, [entityId], defaultHttpOptionsFromConfig(config));
  }

  public addEntitiesToEntityGroup(entityGroupId: string, entityIds: string[], config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/addEntities`, entityIds, defaultHttpOptionsFromConfig(config));
  }

  public changeEntityOwner(ownerId: EntityId, entityId: EntityId, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/owner/${ownerId.entityType}/${ownerId.id}/${entityId.entityType}/${entityId.id}`,
      null, defaultHttpOptionsFromConfig(config));
  }

  public removeEntityFromEntityGroup(entityGroupId: string, entityId: string, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/deleteEntities`, [entityId], defaultHttpOptionsFromConfig(config));
  }

  public removeEntitiesFromEntityGroup(entityGroupId: string, entityIds: string[], config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/deleteEntities`, entityIds, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntity(entityGroupId: string, entityId: string, config?: RequestConfig): Observable<ShortEntityView> {
    return this.http.get<ShortEntityView>(`/api/entityGroup/${entityGroupId}/${entityId}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntities<T extends BaseData<HasId> | ShortEntityView>(entityGroupId: string,
                                                                             pageLink: PageLink,
                                                                             entityType?: EntityType,
                                                                             config?: RequestConfig): Observable<PageData<T>> {
    let url = `/api/entityGroup/${entityGroupId}/`;
    if (entityType) {
      switch (entityType) {
        case EntityType.ASSET:
          url += 'assets';
          break;
        case EntityType.CUSTOMER:
          url += 'customers';
          break;
        case EntityType.DEVICE:
          url += 'devices';
          break;
        case EntityType.USER:
          url += 'users';
          break;
        case EntityType.ENTITY_VIEW:
          url += 'entityViews';
          break;
        case EntityType.DASHBOARD:
          url += 'dashboards';
          break;
        default:
          url += 'entities';
      }
    } else {
      url += 'entities';
    }
    url += pageLink.toQuery();
    return this.http.get<PageData<T>>(url, defaultHttpOptionsFromConfig(config));
  }
}
