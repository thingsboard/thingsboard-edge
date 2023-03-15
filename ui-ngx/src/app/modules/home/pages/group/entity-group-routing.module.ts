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
import {
  ActivatedRouteSnapshot,
  CanActivate,
  Route,
  Router,
  RouterModule,
  RouterStateSnapshot,
  Routes
} from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { of } from 'rxjs';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { map, switchMap } from 'rxjs/operators';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { SchedulerEventsComponent } from '@home/components/scheduler/scheduler-events.component';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import {
  ruleChainBreadcumbLabelFunction,
  RuleChainMetaDataResolver,
  RuleChainResolver,
  RuleNodeComponentsResolver,
  TooltipsterResolver
} from '@home/pages/rulechain/rulechain-routing.module';
import { RuleChainType } from '@shared/models/rule-chain.models';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { UsersTableConfigResolver } from '@home/pages/user/users-table-config.resolver';
import { isDefined } from '@core/utils';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

import _ from 'lodash';
import { EntityService } from '@core/http/entity.service';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { IntegrationsTableConfigResolver } from '@home/pages/integration/integrations-table-config.resolver';
import { dashboardsRoute } from '@home/pages/dashboard/dashboard-routing.module';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { entitiesRoute } from '@home/pages/entities/entities-routing.module';

@Injectable()
export class RedirectToEntityGroup implements CanActivate {
  constructor(private router: Router,
              private entityGroupService: EntityGroupService,
              private store: Store<AppState>,
              private entityService: EntityService) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const groupType: EntityType = route.data.groupType;
    const entityId: string = route.params.entityId;
    if (isDefined(groupType) && isDefined(entityId)) {
      const authState = getCurrentAuthState(this.store);
      if (groupType === EntityType.USER && authState.authUser.authority === Authority.SYS_ADMIN) {
        return true;
      }
      const userOwnerId = authState.userDetails.ownerId;
      return this.entityService.getEntity(groupType, entityId).pipe(
        switchMap((entity) => {
          const ownerId = entity.ownerId;
          return this.entityGroupService.getEntityGroupAllByOwnerId(ownerId.entityType as EntityType, ownerId.id, groupType).pipe(
            switchMap((groupAll) => {
              const entityGroupUrl = _.camelCase(groupType) + 'Groups';
              if (entityIdEquals(ownerId, userOwnerId)) {
                return of(`${entityGroupUrl}/${groupAll.id.id}/${entityId}`);
              } else {
               return this.entityService.getEntity(ownerId.entityType as EntityType, ownerId.id).pipe(
                  switchMap((ownerEntity) => {
                    return this.entityGroupService.getEntityGroupAllByOwnerId(ownerEntity.ownerId.entityType as EntityType,
                      ownerEntity.ownerId.id, EntityType.CUSTOMER).pipe(
                      map((customersGroupAll) => {
                        return `customerGroups/${customersGroupAll.id.id}/${ownerId.id}/${entityGroupUrl}/${groupAll.id.id}/${entityId}`;
                      })
                    );
                  })
                );
              }
            })
          );
        }),
        map((url) => {
          return this.router.parseUrl(url);
        })
      );
    }
    this.router.navigate(['/']);
    return false;
  }

}

const ENTITY_RUTE_ROUTE: Routes = [
  {
    path: 'devices/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.DEVICE
    }
  },
  {
    path: 'assets/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.ASSET
    }
  },
  {
    path: 'entityViews/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.ENTITY_VIEW
    }
  },
  {
    path: 'customers/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.CUSTOMER
    }
  },
  {
    path: 'edgeInstances/:entityId',
    pathMatch: 'full',
    children: [],
    canActivate: [RedirectToEntityGroup],
    data: {
      groupType: EntityType.EDGE
    }
  },
  {
    path: 'users/:entityId',
    pathMatch: 'full',
    component: EntityDetailsPageComponent,
    canActivate: [RedirectToEntityGroup],
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'account_circle'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'user.user',
      groupType: EntityType.USER
    },
    resolve: {
      entitiesTableConfig: UsersTableConfigResolver
    }
  }
];

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
      data: {
        breadcrumb: {
          icon: 'account_circle',
          labelFunction: groupEntitiesLabelFunction
        } as BreadCrumbConfig<GroupEntitiesTableComponent>
      },
      children: [
        {
          path: '',
          component: GroupEntitiesTableComponent,
          data: {
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'entity-group.user-group',
            groupType: EntityType.USER
          },
          resolve: {
            entityGroup: EntityGroupResolver
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
            auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
            title: 'entity-group.user-group',
            groupType: EntityType.USER
          },
          resolve: {
            entityGroup: EntityGroupResolver
          }
        }
      ]
    }
  ]
};

const redirectDashboardGroupsRoutes: Routes = [
  {
    path: 'dashboardGroups',
    pathMatch: 'full',
    redirectTo: '/dashboards/groups'
  },
  {
    path: 'dashboardGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/dashboards/groups/:entityGroupId'
  },
  {
    path: 'dashboardGroups/:entityGroupId/:dashboardId',
    redirectTo: '/dashboards/groups/:entityGroupId/:dashboardId'
  }
];

const redirectDeviceGroupsRoutes: Routes = [
  {
    path: 'deviceGroups',
    pathMatch: 'full',
    redirectTo: '/entities/devices/groups'
  },
  {
    path: 'deviceGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/entities/devices/groups/:entityGroupId'
  },
  {
    path: 'deviceGroups/:entityGroupId/:entityId',
    redirectTo: '/entities/devices/groups/:entityGroupId/:entityId'
  }
];

const redirectAssetGroupsRoutes: Routes = [
  {
    path: 'assetGroups',
    pathMatch: 'full',
    redirectTo: '/entities/assets/groups'
  },
  {
    path: 'assetGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/entities/assets/groups/:entityGroupId'
  },
  {
    path: 'assetGroups/:entityGroupId/:entityId',
    redirectTo: '/entities/assets/groups/:entityGroupId/:entityId'
  }
];

const redirectEntityViewGroupsRoutes: Routes = [
  {
    path: 'entityViewGroups',
    pathMatch: 'full',
    redirectTo: '/entities/entityViews/groups'
  },
  {
    path: 'entityViewGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/entities/entityViews/groups/:entityGroupId'
  },
  {
    path: 'entityViewGroups/:entityGroupId/:entityId',
    redirectTo: '/entities/entityViews/groups/:entityGroupId/:entityId'
  }
];

const redirectCustomerGroupsRoutes: Routes = [
  {
    path: 'customerGroups',
    pathMatch: 'full',
    redirectTo: '/customers/groups'
  },
  {
    path: 'customerGroups/:entityGroupId',
    pathMatch: 'full',
    redirectTo: '/customers/groups/:entityGroupId'
  },
  {
    path: 'customerGroups/:entityGroupId/:entityId',
    redirectTo: '/customers/groups/:entityGroupId/:entityId'
  }
];

const redirectCustomersHierarchyRoutes: Routes = [
  {
    path: 'customersHierarchy',
    pathMatch: 'full',
    redirectTo: '/customers/hierarchy'
  }
];

const EDGE_SCHEDULER_ROUTE: Route = {
  path: ':edgeId/scheduler',
  component: SchedulerEventsComponent,
  data: {
    edgeEntitiesType: EntityType.SCHEDULER_EVENT,
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    breadcrumb: {
    labelFunction: (route, translate, component, data) => {
      return data.entityGroup.edgeEntitiesTitle;
    },
      icon: 'schedule'
    }
  },
  resolve: {
      entityGroup: EntityGroupResolver
  }
};

const EDGE_RULE_CHAINS_ROUTE: Route = {
  path: ':edgeId/ruleChains',
  data: {
    edgeEntitiesType: EntityType.RULE_CHAIN,
    breadcrumb: {
      labelFunction: (route, translate, component, data) => {
        return data.entityGroup.edgeEntitiesTitle;
      },
      icon: 'settings_ethernet'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        title: 'edge.rulechains',
        auth: [Authority.TENANT_ADMIN],
        ruleChainsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: RuleChainsTableConfigResolver
      }
    },
    {
      path: ':ruleChainId',
      component: RuleChainPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: ruleChainBreadcumbLabelFunction,
          icon: 'settings_ethernet'
        } as BreadCrumbConfig<RuleChainPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'rulechain.edge-rulechain',
        import: false,
        ruleChainType: RuleChainType.EDGE,
        ruleChainsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        ruleChain: RuleChainResolver,
        ruleChainMetaData: RuleChainMetaDataResolver,
        ruleNodeComponents: RuleNodeComponentsResolver,
        tooltipster: TooltipsterResolver
      }
    }
  ]
};

const EDGE_INTEGRATIONS_ROUTE: Route = {
  path: ':edgeId/integrations',
  data: {
    edgeEntitiesType: EntityType.INTEGRATION,
    breadcrumb: {
      labelFunction: (route, translate, component, data) => {
        return data.entityGroup.edgeEntitiesTitle;
      },
      icon: 'input'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        title: 'edge.integrations',
        auth: [Authority.TENANT_ADMIN],
        integrationsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
    },
    {
      path: ':entityId',
      component: EntityDetailsPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: entityDetailsPageBreadcrumbLabelFunction,
          icon: 'input'
        } as BreadCrumbConfig<EntityDetailsPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.integration-templates',
        integrationsType: 'edge'
      },
      resolve: {
        entityGroup: EntityGroupResolver,
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
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
              groupType: EntityType.CUSTOMER
            },
            resolve: {
              entityGroup: EntityGroupResolver
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
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'entity-group.customer-group',
              groupType: EntityType.CUSTOMER
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
                data: {
                  breadcrumb: {
                    icon: 'supervisor_account',
                    labelFunction: groupEntitiesLabelFunction
                  } as BreadCrumbConfig<GroupEntitiesTableComponent>
                },
                children: [
                  {
                    path: '',
                    component: GroupEntitiesTableComponent,
                    data: {
                      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                      title: 'entity-group.customer-group',
                      groupType: EntityType.CUSTOMER
                    },
                    resolve: {
                      entityGroup: EntityGroupResolver
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
                      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                      title: 'entity-group.customer-group',
                      groupType: EntityType.CUSTOMER
                    },
                    resolve: {
                      entityGroup: EntityGroupResolver
                    }
                  }
                ]
              }
            ]
          },
          { ...entitiesRoute(), ...{
              path: ':customerId/entities',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerTitle + ': ' + translate.instant('entity.entities');
                  },
                  icon: 'category'
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
          { ...dashboardsRoute(), ...{
              path: ':customerId/dashboards',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.customerGroupsTitle;
                  },
                  icon: 'dashboards'
                }
              }
            }
          },
          {
            path: ':customerId/edgeGroups',
            data: {
              groupType: EntityType.EDGE,
              breadcrumb: {
                labelFunction: (route, translate, component, data) => {
                  return data.entityGroup.customerGroupsTitle;
                },
                icon: 'router'
              }
            },
            children: [
              {
                path: '',
                component: EntitiesTableComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'entity-group.edge-groups',
                  groupType: EntityType.EDGE
                },
                resolve: {
                  entityGroup: EntityGroupResolver,
                  entitiesTableConfig: EntityGroupsTableConfigResolver
                }
              },
              {
                path: ':entityGroupId',
                data: {
                  title: 'entity-group.edge-group',
                  groupType: EntityType.EDGE,
                  breadcrumb: {
                    labelFunction: (route, translate, component, data) => {
                      return data.entityGroup.edgeGroupName ? data.entityGroup.edgeGroupName : data.entityGroup.name;
                    },
                    icon: 'router'
                  }
                },
                children: [
                  {
                    path: '',
                    component: GroupEntitiesTableComponent,
                    data: {
                      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER]
                    },
                    resolve: {
                      entityGroup: EntityGroupResolver,
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
                      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                      title: 'entity-group.edge-group',
                      groupType: EntityType.EDGE
                    },
                    resolve: {
                      entityGroup: EntityGroupResolver
                    }
                  },
                  {...USER_GROUPS_ROUTE, ...{
                      path: ':edgeId/userGroups',
                      data: {
                        edgeEntitiesType: EntityType.USER,
                        groupType: EntityType.USER,
                        breadcrumb: {
                          labelFunction: (route, translate, component, data) => {
                            return data.entityGroup.edgeEntitiesTitle;
                          },
                          icon: 'account_circle'
                        }
                      }
                    }
                  },
                  {...entitiesRoute(), ...{
                      path: ':edgeId/entities',
                      data: {
                        edgeEntitiesType: EntityType.DEVICE,
                        groupType: EntityType.DEVICE,
                        breadcrumb: {
                          labelFunction: (route, translate, component, data) => {
                            return data.entityGroup.edgeEntitiesTitle;
                          },
                          icon: 'devices_other'
                        }
                      }
                    }
                  },
                  {...dashboardsRoute(), ...{
                      path: ':edgeId/dashboards',
                      data: {
                        edgeEntitiesType: EntityType.DASHBOARD,
                        groupType: EntityType.DASHBOARD,
                        breadcrumb: {
                          labelFunction: (route, translate, component, data) => {
                            return data.entityGroup.edgeEntitiesTitle;
                          },
                          icon: 'dashboards'
                        }
                      }
                    }
                  },
                  {...EDGE_SCHEDULER_ROUTE},
                  {...EDGE_RULE_CHAINS_ROUTE},
                  {...EDGE_INTEGRATIONS_ROUTE}
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: 'edgeGroups',
    data: {
      groupType: EntityType.EDGE,
      breadcrumb: {
        label: 'entity-group.edge-groups',
        icon: 'router'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'entity-group.edge-groups',
          groupType: EntityType.EDGE
        },
        resolve: {
          entitiesTableConfig: EntityGroupsTableConfigResolver
        }
      },
      {
        path: ':entityGroupId',
        data: {
          groupType: EntityType.EDGE,
          breadcrumb: {
            icon: 'router',
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
              title: 'entity-group.edge-group',
              groupType: EntityType.EDGE,
            },
            resolve: {
              entityGroup: EntityGroupResolver
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
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'entity-group.edge-group',
              groupType: EntityType.EDGE
            },
            resolve: {
              entityGroup: EntityGroupResolver
            }
          },
          {
            path: ':edgeId/edgeGroups',
            data: {
              groupType: EntityType.EDGE,
              breadcrumb: {
                labelFunction: (route, translate, component, data) => {
                  return data.entityGroup.edgeEntitiesTitle;
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
                  title: 'entity-group.edge-groups',
                  groupType: EntityType.EDGE
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
                  title: 'entity-group.edge-group',
                  groupType: EntityType.EDGE,
                  breadcrumb: {
                    icon: 'router',
                    labelFunction: groupEntitiesLabelFunction
                  } as BreadCrumbConfig<GroupEntitiesTableComponent>
                },
                resolve: {
                  entityGroup: EntityGroupResolver
                }
              }
            ]
          },
          { ...USER_GROUPS_ROUTE, ...{
              path: ':edgeId/userGroups',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.edgeEntitiesTitle;
                  },
                  icon: 'account_circle'
                }
              }
            }
          },
          { ...entitiesRoute(), ...{
              path: ':edgeId/entities',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.edgeEntitiesTitle;
                  },
                  icon: 'devices_other'
                }
              }
            }
          },
          { ...dashboardsRoute(), ...{
              path: ':edgeId/dashboards',
              data: {
                breadcrumb: {
                  labelFunction: (route, translate, component, data) => {
                    return data.entityGroup.edgeEntitiesTitle;
                  },
                  icon: 'dashboards'
                }
              }
            }
          },
          {...EDGE_SCHEDULER_ROUTE},
          {...EDGE_RULE_CHAINS_ROUTE},
          {...EDGE_INTEGRATIONS_ROUTE}
        ]
      }
    ]
  },
  USER_GROUPS_ROUTE,
  ...redirectDashboardGroupsRoutes,
  ...redirectDeviceGroupsRoutes,
  ...redirectAssetGroupsRoutes,
  ...redirectEntityViewGroupsRoutes,
  ...redirectCustomerGroupsRoutes,
  ...redirectCustomersHierarchyRoutes,
  ...ENTITY_RUTE_ROUTE
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EntityGroupResolver,
    RedirectToEntityGroup,
    UsersTableConfigResolver,
    {
      provide: 'emptyEntityGroupResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class EntityGroupRoutingModule { }
