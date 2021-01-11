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

import { Injectable, Injector } from '@angular/core';
import { EntityGroupService } from '@core/http/entity-group.service';
import { CustomerService } from '@core/http/customer.service';
import { EntityGroupInfo, EntityGroupParams, entityGroupsTitle } from '@shared/models/entity-group.models';
import { Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  groupConfigFactoryTokenMap
} from '@home/models/group/group-entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class EntityGroupConfigResolver {

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private translate: TranslateService,
              private injector: Injector) {
  }

  public constructGroupConfigByStateParams<T>(params: EntityGroupParams): Observable<EntityGroupStateInfo<T>> {
    const entityGroupId: string = params.childEntityGroupId || params.entityGroupId;
    if (entityGroupId) {
      return this.entityGroupService.getEntityGroup(entityGroupId).pipe(
        mergeMap((entityGroup) => {
            return this.constructGroupConfig<T>(params, entityGroup);
          }
        ));
    } else {
      return of(null);
    }
  }

  public constructGroupConfig<T>(params: EntityGroupParams,
                                 entityGroup: EntityGroupInfo): Observable<EntityGroupStateInfo<T>> {
    const entityGroupStateInfo: EntityGroupStateInfo<T> = entityGroup;
    // entityGroupStateInfo.origEntityGroup = deepClone(entityGroup);
    return this.resolveParentGroupInfo(params, entityGroupStateInfo).pipe(
      mergeMap((resolvedEntityGroup) => {
          const token = groupConfigFactoryTokenMap.get(resolvedEntityGroup.type);
          const factory = this.injector.get(token) as EntityGroupStateConfigFactory<T>;
          return factory.createConfig(params, resolvedEntityGroup).pipe(
            map(entityGroupConfig => {
              resolvedEntityGroup.entityGroupConfig = entityGroupConfig;
              return resolvedEntityGroup;
            })
          );
        }
      ));
  }

  private resolveParentGroupInfo<T>(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<T>): Observable<EntityGroupStateInfo<T>> {
    if (params.customerId) {
      const groupType: EntityType = params.childGroupType || params.groupType;
      return this.customerService.getShortCustomerInfo(params.customerId).pipe(
        mergeMap((info) => {
            entityGroup.customerGroupsTitle = info.title + ': ' + this.translate.instant(entityGroupsTitle(groupType));
            if (params.childEntityGroupId) {
              return this.entityGroupService.getEntityGroup(params.entityGroupId).pipe(
                map(parentEntityGroup => {
                  entityGroup.parentEntityGroup = parentEntityGroup;
                  return entityGroup;
                })
              );
            } else {
              return of(entityGroup);
            }
          }
        ));
    } else {
      return of(entityGroup);
    }
  }

}
