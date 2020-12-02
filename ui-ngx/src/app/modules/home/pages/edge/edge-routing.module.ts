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
import { RouterModule, Routes } from "@angular/router";
import { EntitiesTableComponent } from "@home/components/entity/entities-table.component";
import { Authority } from "@shared/models/authority.enum";
import { EdgesTableConfigResolver } from "@home/pages/edge/edges-table-config.resolver"
import { AssetsTableConfigResolver } from "@home/pages/asset/assets-table-config.resolver";
import { DevicesTableConfigResolver } from "@home/pages/device/devices-table-config.resolver";
import { EntityViewsTableConfigResolver } from "@home/pages/entity-view/entity-views-table-config.resolver";
import { DashboardsTableConfigResolver } from "@home/pages/dashboard/dashboards-table-config.resolver";
import { RuleChainsTableConfigResolver } from "@home/pages/rulechain/rulechains-table-config.resolver";
import { UsersTableConfigResolver } from "@home/pages/user/users-table-config.resolver";

const routes: Routes = [
  {
    path: 'edges',
    data: {
      breadcrumb: {
        label: 'edge.edges',
        icon: 'router'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          edgesType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: EdgesTableConfigResolver
        }
      },
      {
        path: ':edgeId/ruleChains',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          ruleChainsType: 'edge',
          breadcrumb: {
            label: 'edge.rulechains',
            icon: 'settings_ethernet'
          },
        },
        resolve: {
          entitiesTableConfig: RuleChainsTableConfigResolver
        }
      },
      {
        path: ':edgeId/users',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            label: 'edge.users',
            icon: 'account_circle'
          }
        },
        resolve: {
          entitiesTableConfig: UsersTableConfigResolver
        }
      },
      {
        path: ':edgeId/assets',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          assetsType: 'edge',
          breadcrumb: {
            label: 'edge.assets',
            icon: 'domain'
          }
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      },
      {
        path: ':edgeId/devices',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          devicesType: 'edge',
          breadcrumb: {
            label: 'edge.devices',
            icon: 'devices_other'
          }
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      },
      {
        path: ':edgeId/entityViews',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          entityViewsType: 'edge',
          breadcrumb: {
            label: 'edge.entity-views',
            icon: 'view_quilt'
          },
        },
        resolve: {
          entitiesTableConfig: EntityViewsTableConfigResolver
        }
      },
      {
        path: ':edgeId/dashboards',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          dashboardsType: 'edge',
          breadcrumb: {
            label: 'edge.dashboards',
            icon: 'dashboard'
          }
        },
        resolve: {
          entitiesTableConfig: DashboardsTableConfigResolver
        }
      }]
  }]

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EdgesTableConfigResolver
  ]
})
export class EdgeRoutingModule { }
