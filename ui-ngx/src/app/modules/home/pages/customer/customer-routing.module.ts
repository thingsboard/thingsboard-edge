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

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { CustomersTableConfigResolver } from './customers-table-config.resolver';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { entitiesRoute } from '@home/pages/entities/entities-routing.module';
import { dashboardsRoute } from '@home/pages/dashboard/dashboard-routing.module';
import { Observable, of } from 'rxjs';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { CustomerService } from '@core/http/customer.service';
import { map } from 'rxjs/operators';
import { CustomersHierarchyComponent } from '@home/pages/customer/customers-hierarchy.component';

@Injectable()
export class CustomerTitleResolver implements Resolve<string> {

  constructor(private customerService: CustomerService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<string> {
    const params = resolveGroupParams(route);
    if (params.customerId) {
      return this.customerService.getShortCustomerInfo(params.customerId).pipe(
        map((info) => info.title)
      );
    } else {
      return of(null);
    }
  }
}

const customerRoute = (entityGroup: any, entitiesTableConfig: any): Route =>
  ({
    path: ':entityId',
    component: EntityDetailsPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.CUSTOMER,
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'supervisor_account'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'customer.customer',
      hideTabs: true
    },
    resolve: {
      entityGroup,
      entitiesTableConfig
    }
  });

const customerChildrenRoutes = (): Routes =>
  ([
    { ...customersRoute(), ...{
        path: ':customerId/customers',
        data: {
          breadcrumb: {
            labelFunction: (route, translate) =>
              route.data.customerTitle + ': ' + translate.instant('customer.customers'),
            icon: 'supervisor_account'
          },
          backNavigationCommands: ['../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    },
    { ...entitiesRoute(), ...{
        path: ':customerId/entities',
        data: {
          breadcrumb: {
            labelFunction: (route, translate) =>
              route.data.customerTitle + ': ' + translate.instant('entity.entities'),
            icon: 'category'
          },
          backNavigationCommands: ['../../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    },
    { ...dashboardsRoute(), ...{
        path: ':customerId/dashboards',
        data: {
          breadcrumb: {
            labelFunction: (route, translate) =>
              route.data.customerTitle + ': ' + translate.instant('dashboard.dashboards'),
            icon: 'dashboards'
          },
          backNavigationCommands: ['../../..']
        },
        resolve: {
          customerTitle: CustomerTitleResolver
        }
      }
    }
]);

const customerGroupsChildrenRoutesTemplate = (root: boolean): Routes => {
  const routes: Routes = [];
  const groupsRoute: Route = {
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
  };
  if (!root) {
    groupsRoute.resolve.entityGroup = EntityGroupResolver;
  }
  routes.push(groupsRoute);

  const customerEntitiesRoute: Route = {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.CUSTOMER,
      breadcrumb: {
        icon: 'supervisor_account'
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
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      customerRoute(EntityGroupResolver, 'emptyCustomerTableConfigResolver')
    ]
  };
  if (root) {
    customerEntitiesRoute.data.breadcrumb.labelFunction = (route, translate, component, data) => data.entityGroup.parentEntityGroup ?
        data.entityGroup.parentEntityGroup.name :
        (component && component.entityGroup ? component.entityGroup.name : data.entityGroup.name);
  } else {
    customerEntitiesRoute.data.breadcrumb.labelFunction = groupEntitiesLabelFunction;
  }
  if (root) {
    customerEntitiesRoute.children.push(
      ...customerChildrenRoutes()
    );
  }
  routes.push(customerEntitiesRoute);
  return routes;
};

const customerGroupsRoute = (root: boolean): Route => ({
  path: 'groups',
  data: {
    groupType: EntityType.CUSTOMER,
    breadcrumb: {
      label: 'customer.groups',
      icon: 'supervisor_account'
    }
  },
  children: customerGroupsChildrenRoutesTemplate(root)
});

const customerSharedGroupsRoute = (root: boolean): Route => ({
  path: 'shared',
  data: {
    groupType: EntityType.CUSTOMER,
    shared: true,
    breadcrumb: {
      label: 'customer.shared',
      icon: 'supervisor_account'
    }
  },
  children: customerGroupsChildrenRoutesTemplate(root)
});

const customersHierarchyRoute: Route = {
  path: 'hierarchy',
  component: CustomersHierarchyComponent,
  data: {
    breadcrumb: {
      label: 'customer.hierarchy',
      icon: 'sort'
    },
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    title: 'customers-hierarchy.customers-hierarchy'
  }
};

export const customersRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'customers',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'customer.customers',
        icon: 'supervisor_account'
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
      }
    ]
  };
  const allCustomersRoute: Route = {
    path: 'all',
    data: {
      groupType: EntityType.CUSTOMER,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'customer.all',
        icon: 'supervisor_account'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver,
          entityGroup: EntityGroupResolver
        }
      },
      customerRoute(EntityGroupResolver, CustomersTableConfigResolver)
    ]
  };
  if (root) {
    allCustomersRoute.children.push(
      ...customerChildrenRoutes()
    );
  }
  routeConfig.children.push(allCustomersRoute);
  routeConfig.children.push(customerGroupsRoute(root));
  if (root) {
    routeConfig.children.push(customerSharedGroupsRoute(root));
    routeConfig.children.push(customersHierarchyRoute);
    routeConfig.children.push(customerRoute(EntityGroupResolver, CustomersTableConfigResolver));
  }
  return routeConfig;
};

@NgModule({
  imports: [RouterModule.forChild([customersRoute(true)])],
  exports: [RouterModule],
  providers: [
    CustomersTableConfigResolver,
    {
      provide: 'emptyCustomerTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    },
    CustomerTitleResolver
  ]
})
export class CustomerRoutingModule { }
