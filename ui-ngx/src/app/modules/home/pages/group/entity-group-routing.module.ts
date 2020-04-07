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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityGroupsTableConfigResolver } from '@home/pages/group/entity-groups-table-config.resolver';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import { EntityGroupStateInfo } from '@home/models/group/group-entities-table-config.models';
import { EntityGroupConfigResolver } from '@home/pages/group/entity-group-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';

@Injectable()
export class EntityGroupResolver<T> implements Resolve<EntityGroupStateInfo<T>> {

  constructor(private entityGroupConfigResolver: EntityGroupConfigResolver) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityGroupStateInfo<T>> {
    return this.entityGroupConfigResolver.constructGroupConfigByStateParams(route.params);
  }
}

const routes: Routes = [
  {
    path: 'customerGroups',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.customer-groups',
      groupType: EntityType.CUSTOMER,
      breadcrumb: {
        label: 'entity-group.customer-groups',
        icon: 'supervisor_account'
      }
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: 'assetGroups',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.asset-groups',
      groupType: EntityType.ASSET,
      breadcrumb: {
        label: 'entity-group.asset-groups',
        icon: 'domain'
      }
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: 'deviceGroups',
    data: {
      breadcrumb: {
        label: 'entity-group.device-groups',
        icon: 'devices_other'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.device-groups',
          groupType: EntityType.DEVICE
        },
        resolve: {
          entitiesTableConfig: EntityGroupsTableConfigResolver
        }
      },
      {
        path: ':entityGroupId',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.device-group',
          breadcrumb: {
            icon: 'devices_other',
            labelFunction: (route, translate, component) => {
              return component ? component.entityGroup?.name : '';
            }
          } as BreadCrumbConfig<GroupEntitiesTableComponent>
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      }
    ]
  },
  {
    path: 'entityViewGroups',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.entity-view-groups',
      groupType: EntityType.ENTITY_VIEW,
      breadcrumb: {
        label: 'entity-group.entity-view-groups',
        icon: 'view_quilt'
      }
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: 'userGroups',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.user-groups',
      groupType: EntityType.USER,
      breadcrumb: {
        label: 'entity-group.user-groups',
        icon: 'account_circle'
      }
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  },
  {
    path: 'dashboardGroups',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.dashboard-groups',
      groupType: EntityType.DASHBOARD,
      breadcrumb: {
        label: 'entity-group.dashboard-groups',
        icon: 'dashboard'
      }
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EntityGroupsTableConfigResolver,
    EntityGroupResolver
  ]
})
export class EntityGroupRoutingModule { }
