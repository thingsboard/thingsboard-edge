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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityGroupsTableConfigResolver } from '@home/pages/group/entity-groups-table-config.resolver';
import { EntityType } from '@shared/models/entity-type.models';

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
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'entity-group.device-groups',
      groupType: EntityType.DEVICE,
      breadcrumb: {
        label: 'entity-group.device-groups',
        icon: 'devices_other'
      }
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
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
    EntityGroupsTableConfigResolver
  ]
})
export class EntityGroupRoutingModule { }
