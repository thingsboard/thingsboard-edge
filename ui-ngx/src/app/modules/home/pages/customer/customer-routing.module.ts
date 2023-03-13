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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { UsersTableConfigResolver } from '../user/users-table-config.resolver';
import { CustomersTableConfigResolver } from './customers-table-config.resolver';
import { DevicesTableConfigResolver } from '@modules/home/pages/device/devices-table-config.resolver';
import { AssetsTableConfigResolver } from '../asset/assets-table-config.resolver';
import { DashboardsTableConfigResolver } from '@modules/home/pages/dashboard/dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { dashboardBreadcumbLabelFunction, DashboardResolver } from '@home/pages/dashboard/dashboard-routing.module';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';

const routes: Routes = [
  {
    path: 'customers',
    data: {
      breadcrumb: {
        label: 'customer.customers',
        icon: 'supervisor_account'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'supervisor_account'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver
        }
      },
      {
        path: ':customerId/users',
        data: {
          breadcrumb: {
            label: 'user.customer-users',
            icon: 'account_circle'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'user.customer-users'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'account_circle'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'user.customer-users'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/devices',
        data: {
          breadcrumb: {
            label: 'customer.devices',
            icon: 'devices_other'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.devices',
              devicesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'devices_other'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.devices',
              devicesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/assets',
        data: {
          breadcrumb: {
            label: 'customer.assets',
            icon: 'domain'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.assets',
              assetsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'domain'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.assets',
              assetsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/edgeInstances',
        data: {
          breadcrumb: {
            label: 'customer.edges',
            icon: 'router'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.edges',
              edgesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'router'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.edges',
              edgesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/dashboards',
        data: {
          breadcrumb: {
            label: 'customer.dashboards',
            icon: 'dashboard'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.dashboards',
              dashboardsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DashboardsTableConfigResolver
            }
          },
          {
            path: ':dashboardId',
            component: DashboardPageComponent,
            data: {
              breadcrumb: {
                labelFunction: dashboardBreadcumbLabelFunction,
                icon: 'dashboard'
              } as BreadCrumbConfig<DashboardPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'customer.dashboard',
              widgetEditMode: false
            },
            resolve: {
              dashboard: DashboardResolver
            }
          }
        ]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    CustomersTableConfigResolver
  ]
})
export class CustomerRoutingModule { }
