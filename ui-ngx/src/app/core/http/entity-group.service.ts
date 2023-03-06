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
import { defaultPageLinkSearchFunction, PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { forkJoin, Observable, of, throwError } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { ContactBased } from '@shared/models/contact-based.model';
import { EntityId } from '@shared/models/id/entity-id';
import {
  EntityGroup,
  EntityGroupInfo,
  prepareEntityGroupConfiguration,
  ShareGroupRequest,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { map, mergeMap } from 'rxjs/operators';
import { BaseData, HasId, sortEntitiesByIds } from '@shared/models/base-data';
import { deepClone, isDefinedAndNotNull } from '@core/utils';
import { OtaPackageService } from '@core/http/ota-package.service';
import { DeviceGroupOtaPackage, OtaUpdateType } from '@shared/models/ota-package.models';
import { OtaPackageId } from '@shared/models/id/ota-package-id';

@Injectable({
  providedIn: 'root'
})
export class EntityGroupService {

  constructor(
    private http: HttpClient,
    private otaPackageService: OtaPackageService
  ) {}

  public getOwners(pageLink: PageLink, config?: RequestConfig): Observable<PageData<ContactBased<EntityId>>> {
    return this.http.get<PageData<ContactBased<EntityId>>>(`/api/owners${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroup(entityGroupId: string, config?: RequestConfig): Observable<EntityGroupInfo> {
    return this.http.get<EntityGroupInfo>(`/api/entityGroup/${entityGroupId}`,
      defaultHttpOptionsFromConfig(config)).pipe(
        map(group => {
          group.configuration = prepareEntityGroupConfiguration(group.type, group.configuration);
          return group;
        }),
        mergeMap(group => this.fetchOtaPackageGroupInfo(group, config))
    );
  }

  public fetchOtaPackageGroupInfo(group: EntityGroupInfo, config?: RequestConfig): Observable<EntityGroupInfo> {
    if (isDefinedAndNotNull(group) && group.type === EntityType.DEVICE && !group.groupAll) {
      const tasks = [];
      tasks.push(this.otaPackageService.getOtaPackageInfoByDeviceGroupId(group.id.id, OtaUpdateType.FIRMWARE, config));
      tasks.push(this.otaPackageService.getOtaPackageInfoByDeviceGroupId(group.id.id, OtaUpdateType.SOFTWARE, config));
      return forkJoin(tasks).pipe(
        map(([firmware, software]: DeviceGroupOtaPackage[]) => {
          if (isDefinedAndNotNull(firmware)) {
            group.firmwareGroup = firmware;
            group.firmwareId = deepClone(firmware.otaPackageId);
          }
          if (isDefinedAndNotNull(software)) {
            group.softwareGroup = software;
            group.softwareId = deepClone(software.otaPackageId);
          }
          return group;
        })
      );
    }
    return of(group);
  }

  public updateDeviceGroupOtaPackage(deviceGroupOtaPackage: DeviceGroupOtaPackage, otaPackageId: OtaPackageId | null,
                                     groupId: EntityGroupId, otaPackageType: OtaUpdateType,
                                     config?: RequestConfig): Observable<DeviceGroupOtaPackage> {
    if (isDefinedAndNotNull(deviceGroupOtaPackage)) {
      if (otaPackageId === null) {
        return this.deleteDeviceGroupOtaPackage(deviceGroupOtaPackage.id, config);
      } else if (otaPackageId.id !== deviceGroupOtaPackage.otaPackageId.id) {
        deviceGroupOtaPackage.otaPackageId = otaPackageId;
        return this.saveDeviceGroupOtaPackage(deviceGroupOtaPackage, config);
      } else {
        return of(deviceGroupOtaPackage);
      }
    } else if (isDefinedAndNotNull(otaPackageId)) {
      const groupOtaPackage: DeviceGroupOtaPackage = {
        otaPackageId,
        otaPackageType,
        groupId
      };
      return this.saveDeviceGroupOtaPackage(groupOtaPackage, config);
    }
    return of(null);
  }

  public saveDeviceGroupOtaPackage(deviceGroupOtaPackage: DeviceGroupOtaPackage,
                                   config?: RequestConfig ): Observable<DeviceGroupOtaPackage> {
    return this.http.post<DeviceGroupOtaPackage>('/api/deviceGroupOtaPackage', deviceGroupOtaPackage, defaultHttpOptionsFromConfig(config));
  }

  public saveDeviceEntityGroup(entityGroup: EntityGroupInfo, originalEntityGroup: EntityGroupInfo,
                               config?: RequestConfig): Observable<EntityGroupInfo> {
    if (isDefinedAndNotNull(entityGroup.id)) {
      return this.otaPackageService.confirmDialogUpdatePackage(entityGroup, originalEntityGroup).pipe(
        mergeMap((update) => {
          if (update) {
            const firmwareId = entityGroup.firmwareId;
            const firmwareGroup = entityGroup.firmwareGroup;
            const softwareId = entityGroup.softwareId;
            const softwareGroup = entityGroup.softwareGroup;
            delete entityGroup.firmwareId;
            delete entityGroup.firmwareGroup;
            delete entityGroup.softwareId;
            delete entityGroup.softwareGroup;
            const tasks = [this.updateDeviceGroupOtaPackage(firmwareGroup, firmwareId, entityGroup.id, OtaUpdateType.FIRMWARE, config),
                           this.updateDeviceGroupOtaPackage(softwareGroup, softwareId, entityGroup.id, OtaUpdateType.SOFTWARE, config),
                           this.saveEntityGroup(entityGroup, config)];
            return forkJoin(tasks).pipe(
              map(([firmware, software, savedEntityGroup]: [DeviceGroupOtaPackage, DeviceGroupOtaPackage, EntityGroupInfo]) =>
                Object.assign(savedEntityGroup, {
                    firmwareId: deepClone(firmware?.otaPackageId),
                    firmwareGroup: firmware,
                    softwareId: deepClone(software?.otaPackageId),
                    softwareGroup: software
                  })
              ));
          }
          return throwError('Canceled saving device group');
        })
      );
    }
    return this.saveEntityGroup(entityGroup, config);
  }

  public deleteDeviceGroupOtaPackage(otaPackageId: string, config?: RequestConfig ) {
    return this.http.delete<null>(`/api/deviceGroupOtaPackage/${otaPackageId}`, defaultHttpOptionsFromConfig(config));
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

  public shareEntityGroup(entityGroupId: string, shareGroupRequest: ShareGroupRequest, config?: RequestConfig): Observable<any> {
    return this.http.post(`/api/entityGroup/${entityGroupId}/share`, shareGroupRequest, defaultHttpOptionsFromConfig(config));
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

  public getEntityGroupsByOwnerIdAndPageLink(ownerType: EntityType, ownerId: string, groupType: EntityType,
                                             pageLink: PageLink, config?: RequestConfig): Observable<PageData<EntityGroup>> {
    return this.http.get<PageData<EntityGroup>>(`/api/entityGroups/${ownerType}/${ownerId}/${groupType}${pageLink.toQuery()}`,
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
        case EntityType.EDGE:
          url += 'edges';
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

  public getEdgeEntityGroups(edgeId: string, groupType: EntityType, config?: RequestConfig): Observable<Array<EntityGroupInfo>> {
    return this.http.get<Array<EntityGroupInfo>>(`/api/allEntityGroups/edge/${edgeId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignEntityGroupToEdge(edgeId: string, entityGroupId: string,
                                 groupType: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>(`/api/edge/${edgeId}/entityGroup/${entityGroupId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignEntityGroupFromEdge(edgeId: string, entityGroupId: string,
                                     groupType: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.delete<EntityGroup>(`/api/edge/${edgeId}/entityGroup/${entityGroupId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }
}
