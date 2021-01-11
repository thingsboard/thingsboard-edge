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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Route, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import { EntityGroupStateInfo } from '@home/models/group/group-entities-table-config.models';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { DashboardPageComponent } from '@home/pages/dashboard/dashboard-page.component';
import { Operation, Resource } from '@shared/models/security.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { map } from 'rxjs/operators';
import { dashboardBreadcumbLabelFunction } from '@home/pages/dashboard/dashboard-routing.module';
import { CustomersHierarchyComponent } from '@home/pages/group/customers-hierarchy.component';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class EntityGroupResolver<T> implements Resolve<EntityGroupStateInfo<T>> {

  constructor(private entityGroupConfigResolver: EntityGroupConfigResolver) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityGroupStateInfo<T>> | EntityGroupStateInfo<T> {
    const entityGroupParams = resolveGroupParams(route);
    return this.entityGroupConfigResolver.constructGroupConfigByStateParams(entityGroupParams);
  }
}

@Injectable()
export class DashboardResolver implements Resolve<Dashboard> {

  constructor(private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Dashboard> {
    const dashboardId = route.params.dashboardId;
    return this.dashboardService.getDashboard(dashboardId).pipe(
      map((dashboard) => this.dashboardUtils.validateAndUpdateDashboard(dashboard))
    );
  }
}

const groupEntitiesLabelFunction: BreadCrumbLabelFunction<GroupEntitiesTableComponent> =
  (route, translate, component, data) => {
    return component.entityGroup.name;
  };

const ASSET_GROUPS_ROUTE: Route =
  {
    path: 'assetGroups',
    data: {
      groupType: EntityType.ASSET,
      breadcrumb: {
        label: 'entity-group.asset-groups',
        icon: 'domain'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.asset-groups',
          groupType: EntityType.ASSET
        },
        resolve: {
          entityGroup: EntityGroupResolver,
          entitiesTableConfig: EntityGroupsTableConfigResolver
        }
      },
      {
        path: ':entityGroupId',
        component: GroupEntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.asset-group',
          groupType: EntityType.ASSET,
          breadcrumb: {
            icon: 'domain',
            labelFunction: groupEntitiesLabelFunction
          } as BreadCrumbConfig<GroupEntitiesTableComponent>
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      }
    ]
  };

const DEVICE_GROUPS_ROUTE: Route = {
  path: 'deviceGroups',
  data: {
    groupType: EntityType.DEVICE,
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
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: EntityGroupsTableConfigResolver
      }
    },
    {
      path: ':entityGroupId',
      component: GroupEntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'entity-group.device-group',
        groupType: EntityType.DEVICE,
        breadcrumb: {
          icon: 'devices_other',
          labelFunction: groupEntitiesLabelFunction
        } as BreadCrumbConfig<GroupEntitiesTableComponent>
      },
      resolve: {
        entityGroup: EntityGroupResolver
      }
    }
  ]
};

const ENTITY_VIEW_GROUPS_ROUTE: Route = {
  path: 'entityViewGroups',
  data: {
    groupType: EntityType.ENTITY_VIEW,
    breadcrumb: {
      label: 'entity-group.entity-view-groups',
      icon: 'view_quilt'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'entity-group.entity-view-groups',
        groupType: EntityType.ENTITY_VIEW
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: EntityGroupsTableConfigResolver
      }
    },
    {
      path: ':entityGroupId',
      component: GroupEntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'entity-group.entity-view-group',
        groupType: EntityType.ENTITY_VIEW,
        breadcrumb: {
          icon: 'view_quilt',
          labelFunction: groupEntitiesLabelFunction
        } as BreadCrumbConfig<GroupEntitiesTableComponent>
      },
      resolve: {
        entityGroup: EntityGroupResolver
      }
    }
  ]
};

const USER_GROUPS_ROUTE: Route = {
  path: 'userGroups',
  data: {
    groupType: EntityType.USER,
    breadcrumb: {
      label: 'entity-group.user-groups',
      icon: 'account_circle'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'entity-group.user-groups',
        groupType: EntityType.USER
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: EntityGroupsTableConfigResolver
      }
    },
    {
      path: ':entityGroupId',
      component: GroupEntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'entity-group.user-group',
        groupType: EntityType.USER,
        breadcrumb: {
          icon: 'account_circle',
          labelFunction: groupEntitiesLabelFunction
        } as BreadCrumbConfig<GroupEntitiesTableComponent>
      },
      resolve: {
        entityGroup: EntityGroupResolver
      }
    }
  ]
};

const DASHBOARD_GROUPS_ROUTE: Route =   {
  path: 'dashboardGroups',
  data: {
    groupType: EntityType.DASHBOARD,
    breadcrumb: {
      label: 'entity-group.dashboard-groups',
      icon: 'dashboard'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'entity-group.dashboard-groups',
        groupType: EntityType.DASHBOARD
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: EntityGroupsTableConfigResolver
      }
    },
    {
      path: ':entityGroupId',
      data: {
        groupType: EntityType.DASHBOARD,
        breadcrumb: {
          icon: 'dashboard',
          labelFunction: groupEntitiesLabelFunction
        } as BreadCrumbConfig<GroupEntitiesTableComponent>
      },
      children: [
        {
          path: '',
          component: GroupEntitiesTableComponent,
          data: {
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'entity-group.dashboard-group',
            groupType: EntityType.DASHBOARD
          },
          resolve: {
            entityGroup: EntityGroupResolver
          }
        },
        {
          path: ':dashboardId',
          component: DashboardPageComponent,
          data: {
            groupType: EntityType.DASHBOARD,
            breadcrumb: {
              labelFunction: dashboardBreadcumbLabelFunction,
              icon: 'dashboard'
            } as BreadCrumbConfig<DashboardPageComponent>,
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'dashboard.dashboard',
            widgetEditMode: false
          },
          resolve: {
            dashboard: DashboardResolver,
            entityGroup: EntityGroupResolver
          }
        }
      ]
    }
  ]
};

const routes: Routes = [
  {
    path: 'customerGroups',
    data: {
      groupType: EntityType.CUSTOMER,
      breadcrumb: {
        label: 'entity-group.customer-groups',
        icon: 'supervisor_account'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.customer-groups',
          groupType: EntityType.CUSTOMER
        },
        resolve: {
          entitiesTableConfig: EntityGroupsTableConfigResolver
        }
      },
      {
        path: ':entityGroupId',
        data: {
          groupType: EntityType.CUSTOMER,
          breadcrumb: {
            icon: 'supervisor_account',
            labelFunction: (route, translate, component, data) => {
              return data.entityGroup.parentEntityGroup ?
                     data.entityGroup.parentEntityGroup.name :
                (component && component.entityGroup ? component.entityGroup.name : data.entityGroup.name);
            }
          } as BreadCrumbConfig<GroupEntitiesTableComponent>
        },
        children: [
          {
            path: '',
            component: GroupEntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'entity-group.customer-group',
              groupType: EntityType.CUSTOMER,
            },
            resolve: {
              entityGroup: EntityGroupResolver
            }
          },
          {
            path: ':customerId/customerGroups',
            data: {
              groupType: EntityType.CUSTOMER,
              breadcrumb: {
                labelFunction: (route, translate, component, data) => {
                  return data.entityGroup.customerGroupsTitle;
                },
                icon: 'supervisor_account'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'entity-group.customer-groups',
                  groupType: EntityType.CUSTOMER
                },
                resolve: {
                  entityGroup: EntityGroupResolver,
                  entitiesTableConfig: EntityGroupsTableConfigResolver
                }
              },
              {
                path: ':entityGroupId',
                component: GroupEntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'entity-group.customer-group',
                  groupType: EntityType.CUSTOMER,
                  breadcrumb: {
                    icon: 'supervisor_account',
                    labelFunction: groupEntitiesLabelFunction
                  } as BreadCrumbConfig<GroupEntitiesTableComponent>
                },
                resolve: {
                  entityGroup: EntityGroupResolver
                }
              }
            ]
          },
          { ...ASSET_GROUPS_ROUTE, ...{
              path: ':customerId/assetGroups',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerGroupsTitle;
                  },
                  icon: 'domain'
                }
              }
            }
          },
          { ...DEVICE_GROUPS_ROUTE, ...{
              path: ':customerId/deviceGroups',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerGroupsTitle;
                  },
                  icon: 'devices_other'
                }
              }
            }
          },
          { ...ENTITY_VIEW_GROUPS_ROUTE, ...{
              path: ':customerId/entityViewGroups',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerGroupsTitle;
                  },
                  icon: 'view_quilt'
                }
              }
            }
          },
          { ...USER_GROUPS_ROUTE, ...{
              path: ':customerId/userGroups',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerGroupsTitle;
                  },
                  icon: 'account_circle'
                }
              }
            }
          },
          { ...DASHBOARD_GROUPS_ROUTE, ...{
              path: ':customerId/dashboardGroups',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerGroupsTitle;
                  },
                  icon: 'dashboard'
                }
              }
            }
          }
        ]
      }
    ]
  },
  ASSET_GROUPS_ROUTE,
  DEVICE_GROUPS_ROUTE,
  ENTITY_VIEW_GROUPS_ROUTE,
  USER_GROUPS_ROUTE,
  DASHBOARD_GROUPS_ROUTE,
  {
    path: 'dashboards/:dashboardId',
    component: DashboardPageComponent,
    data: {
      breadcrumb: {
        labelFunction: dashboardBreadcumbLabelFunction,
        icon: 'dashboard'
      } as BreadCrumbConfig<DashboardPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      canActivate: (userPermissionsService: UserPermissionsService): boolean => {
        return userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD) &&
          userPermissionsService.hasResourcesGenericPermission([Resource.WIDGETS_BUNDLE, Resource.WIDGET_TYPE], Operation.READ);
      },
      title: 'dashboard.dashboard',
      widgetEditMode: false
    },
    resolve: {
      dashboard: DashboardResolver,
      entityGroup: 'emptyEntityGroupResolver'
    }
  },
  {
    path: 'customersHierarchy',
    component: CustomersHierarchyComponent,
    data: {
      breadcrumb: {
        label: 'customers-hierarchy.customers-hierarchy',
        icon: 'sort'
      },
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'customers-hierarchy.customers-hierarchy'
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EntityGroupResolver,
    DashboardResolver,
    {
      provide: 'emptyEntityGroupResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class EntityGroupRoutingModule { }
