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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Route, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { DashboardsTableConfigResolver } from './dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import {
  EntityGroupResolver,
  groupEntitiesLabelFunction
} from '@home/pages/group/entity-group.shared';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityType } from '@shared/models/entity-type.models';
import { Operation, Resource } from '@shared/models/security.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';

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

const dashboardBreadcumbLabelFunction: BreadCrumbLabelFunction<DashboardPageComponent> =
  ((route, translate, component, data, utils) =>
      utils ? utils.customTranslation(component.dashboard.title, component.dashboard.title) : component.dashboard.title
  );

const dashboardRoute = (entityGroup: any, singlePageMode = false): Route =>
  ({
    path: ':dashboardId',
    component: DashboardPageComponent,
    data: {
      groupType: EntityType.DASHBOARD,
      breadcrumb: {
        labelFunction: dashboardBreadcumbLabelFunction,
        icon: 'dashboard'
      } as BreadCrumbConfig<DashboardPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      canActivate: (userPermissionsService: UserPermissionsService): boolean =>
        userPermissionsService.hasReadGroupsPermission(EntityType.DASHBOARD) &&
        userPermissionsService.hasResourcesGenericPermission([Resource.WIDGETS_BUNDLE, Resource.WIDGET_TYPE], Operation.READ),
      title: 'dashboard.dashboard',
      hideTabs: true,
      widgetEditMode: false,
      singlePageMode
    },
    resolve: {
      dashboard: DashboardResolver,
      entityGroup
    }
  });

const dashboardGroupsChildrenRoutes: Route[] = [
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
          groupType: EntityType.DASHBOARD,
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      dashboardRoute(EntityGroupResolver, false)
    ]
  }
];

const dashboardGroupsRoute: Route = {
  path: 'groups',
  data: {
    groupType: EntityType.DASHBOARD,
    breadcrumb: {
      label: 'dashboard.groups',
      icon: 'dashboard'
    }
  },
  children: dashboardGroupsChildrenRoutes
};

const dashboardSharedGroupsRoute: Route = {
  path: 'shared',
  data: {
    groupType: EntityType.DASHBOARD,
    shared: true,
    breadcrumb: {
      label: 'dashboard.shared',
      icon: 'dashboard'
    }
  },
  children: dashboardGroupsChildrenRoutes
};

export const dashboardsRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'dashboards',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'dashboard.dashboards',
        icon: 'dashboards'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: 'all'
        }
      },
      {
        path: 'all',
        data: {
          groupType: EntityType.DASHBOARD,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          breadcrumb: {
            label: 'dashboard.all',
            icon: 'dashboards'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'dashboard.dashboards'
            },
            resolve: {
              entitiesTableConfig: DashboardsTableConfigResolver,
              entityGroup: EntityGroupResolver
            }
          },
          dashboardRoute(EntityGroupResolver, false)
        ]
      },
      dashboardGroupsRoute
    ]
  };
  if (root) {
    routeConfig.children.push(dashboardSharedGroupsRoute);
  }
  routeConfig.children.push(dashboardRoute('emptyEntityGroupResolver', true));
  return routeConfig;
};

// @dynamic
@NgModule({
  imports: [RouterModule.forChild([dashboardsRoute(true)])],
  exports: [RouterModule],
  providers: [
    DashboardsTableConfigResolver,
    DashboardResolver
  ]
})
export class DashboardRoutingModule { }
