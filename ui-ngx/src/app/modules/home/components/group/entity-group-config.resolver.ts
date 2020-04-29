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
