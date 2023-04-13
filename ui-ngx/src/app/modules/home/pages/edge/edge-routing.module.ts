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
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { RuleChainType } from '@shared/models/rule-chain.models';
import {
  importRuleChainBreadcumbLabelFunction,
  ruleChainBreadcumbLabelFunction,
  RuleChainImportGuard,
  RuleChainMetaDataResolver,
  RuleChainResolver,
  RuleNodeComponentsResolver,
  TooltipsterResolver
} from '@home/pages/rulechain/rulechain-routing.module';
import { ConvertersTableConfigResolver } from '@home/pages/converter/converters-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { IntegrationsTableConfigResolver } from '@home/pages/integration/integrations-table-config.resolver';
import { EntityType } from '@shared/models/entity-type.models';
import { dashboardGroupsRoute } from '@home/pages/dashboard/dashboard-routing.module';
import { userGroupsRoute } from '@home/pages/user/user-routing.module';
import { assetGroupsRoute } from '@home/pages/asset/asset-routing.module';
import { deviceGroupsRoute } from '@home/pages/device/device-routing.module';
import { entityViewGroupsRoute } from '@home/pages/entity-view/entity-view-routing.module';
import { edgeEntitiesTitle, entityGroupsTitle, resolveGroupParams } from '@shared/models/entity-group.models';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupResolver, groupEntitiesLabelFunction } from '@home/pages/group/entity-group.shared';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { SchedulerEventsComponent } from '@home/components/scheduler/scheduler-events.component';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { EdgeService } from '@core/http/edge.service';

@Injectable()
export class EdgeTitleResolver implements Resolve<string> {

  constructor(private edgeService: EdgeService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<string> {
    const params = resolveGroupParams(route);
    if (params.edgeId) {
      return this.edgeService.getEdge(params.edgeId).pipe(
        map((edge) => edge.name)
      );
    } else {
      return of(null);
    }
  }
}

const edgeRoute = (entityGroup: any, entitiesTableConfig: any): Route =>
  ({
    path: ':entityId',
    component: EntityDetailsPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      groupType: EntityType.EDGE,
      breadcrumb: {
        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
        icon: 'router'
      } as BreadCrumbConfig<EntityDetailsPageComponent>,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'edge.edge',
      hideTabs: true
    },
    resolve: {
      entityGroup,
      entitiesTableConfig
    }
  });

const edgeSchedulerRoute: Route = {
  path: ':edgeId/scheduler',
  component: SchedulerEventsComponent,
  data: {
    edgeEntitiesType: EntityType.SCHEDULER_EVENT,
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    breadcrumb: {
      labelFunction: (route, translate, component, data) =>
        route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.SCHEDULER_EVENT)),
      icon: 'schedule'
    },
    backNavigationCommands: ['../..'],
    hideTabs: true
  },
  resolve: {
    entityGroup: EntityGroupResolver,
    edgeTitle: EdgeTitleResolver
  }
};

const edgeRuleChainsRoute: Route = {
  path: ':edgeId/ruleChains',
  data: {
    edgeEntitiesType: EntityType.RULE_CHAIN,
    breadcrumb: {
      labelFunction: (route, translate, component, data) =>
        route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.RULE_CHAIN)),
      icon: 'settings_ethernet'
    },
    backNavigationCommands: ['../..'],
    hideTabs: true
  },
  resolve: {
    edgeTitle: EdgeTitleResolver
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

const edgeIntegrationsRoute: Route = {
  path: ':edgeId/integrations',
  data: {
    edgeEntitiesType: EntityType.INTEGRATION,
    breadcrumb: {
      labelFunction: (route, translate, component, data) =>
        route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.INTEGRATION)),
      icon: 'input'
    },
    backNavigationCommands: ['../..'],
    hideTabs: true
  },
  resolve: {
    edgeTitle: EdgeTitleResolver
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

const edgeChildrenRoutes = (): Routes =>
  ([
    { ...userGroupsRoute, ...{
        path: ':edgeId/userGroups',
        data: {
          edgeEntitiesType: EntityType.USER,
          groupType: EntityType.USER,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.USER)),
            icon: 'account_circle'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...assetGroupsRoute, ...{
        path: ':edgeId/assetGroups',
        data: {
          edgeEntitiesType: EntityType.ASSET,
          groupType: EntityType.ASSET,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.ASSET)),
            icon: 'domain'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...deviceGroupsRoute, ...{
        path: ':edgeId/deviceGroups',
        data: {
          edgeEntitiesType: EntityType.DEVICE,
          groupType: EntityType.DEVICE,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.DEVICE)),
            icon: 'devices_other'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...entityViewGroupsRoute, ...{
        path: ':edgeId/entityViewGroups',
        data: {
          edgeEntitiesType: EntityType.ENTITY_VIEW,
          groupType: EntityType.ENTITY_VIEW,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.ENTITY_VIEW)),
            icon: 'view_quilt'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    { ...dashboardGroupsRoute, ...{
        path: ':edgeId/dashboardGroups',
        data: {
          edgeEntitiesType: EntityType.DASHBOARD,
          groupType: EntityType.DASHBOARD,
          breadcrumb: {
            labelFunction: (route, translate, component, data) =>
              route.data.edgeTitle + ': ' + translate.instant(edgeEntitiesTitle(EntityType.DASHBOARD)),
            icon: 'dashboard'
          },
          backNavigationCommands: ['../..'],
          hideTabs: true
        },
        resolve: {
          edgeTitle: EdgeTitleResolver
        }
      }
    },
    edgeSchedulerRoute,
    edgeRuleChainsRoute,
    edgeIntegrationsRoute
  ]);

const edgeGroupsChildrenRoutesTemplate = (root: boolean, shared: boolean): Routes => {
  const routes: Routes = [];
  const groupsRoute: Route = {
    path: '',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: entityGroupsTitle(EntityType.EDGE, shared),
      groupType: EntityType.EDGE
    },
    resolve: {
      entitiesTableConfig: EntityGroupsTableConfigResolver
    }
  };
  if (!root) {
    groupsRoute.resolve.entityGroup = EntityGroupResolver;
  }
  routes.push(groupsRoute);

  const edgeEntitiesRoute: Route = {
    path: ':entityGroupId',
    data: {
      groupType: EntityType.EDGE,
      breadcrumb: {
        labelFunction: root ?
          (route, translate, component, data) =>
            data.entityGroup.parentEntityGroup ? data.entityGroup.parentEntityGroup.name :
            (component && component.entityGroup ? component.entityGroup.name : data.entityGroup.name) :
          (route, translate, component, data) =>
            data.entityGroup.edgeGroupName ? data.entityGroup.edgeGroupName : data.entityGroup.name,
        icon: 'router'
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
          backNavigationCommands: ['../']
        },
        resolve: {
          entityGroup: EntityGroupResolver
        }
      },
      ...edgeChildrenRoutes(),
      edgeRoute(EntityGroupResolver, 'emptyEdgeTableConfigResolver')
    ]
  };
  routes.push(edgeEntitiesRoute);
  return routes;
};

const edgeGroupsRoute = (root: boolean): Route => ({
  path: 'groups',
  data: {
    groupType: EntityType.EDGE,
    breadcrumb: {
      label: 'edge.groups',
      icon: 'router'
    }
  },
  children: edgeGroupsChildrenRoutesTemplate(root, false)
});

const edgeSharedGroupsRoute = (root: boolean): Route => ({
  path: 'shared',
  data: {
    groupType: EntityType.EDGE,
    shared: true,
    breadcrumb: {
      label: 'edge.shared',
      icon: 'router'
    }
  },
  children: edgeGroupsChildrenRoutesTemplate(root, true)
});

const edgeRuleChainTemplatesRoute: Route = {
  path: 'ruleChains',
  data: {
    breadcrumb: {
      label: 'edge.rulechain-templates',
      icon: 'settings_ethernet'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.rulechain-templates',
        ruleChainsType: 'edges'
      },
      resolve: {
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
        ruleChainType: RuleChainType.EDGE
      },
      resolve: {
        ruleChain: RuleChainResolver,
        ruleChainMetaData: RuleChainMetaDataResolver,
        ruleNodeComponents: RuleNodeComponentsResolver,
        tooltipster: TooltipsterResolver
      }
    },
    {
      path: 'ruleChain/import',
      component: RuleChainPageComponent,
      canActivate: [RuleChainImportGuard],
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: importRuleChainBreadcumbLabelFunction,
          icon: 'settings_ethernet'
        } as BreadCrumbConfig<RuleChainPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'rulechain.edge-rulechain',
        import: true,
        ruleChainType: RuleChainType.EDGE
      },
      resolve: {
        ruleNodeComponents: RuleNodeComponentsResolver,
        tooltipster: TooltipsterResolver
      }
    }
  ]
};

const edgeConverterTemplatesRoute: Route = {
  path: 'converters',
  data: {
    breadcrumb: {
      label: 'edge.converter-templates',
      icon: 'transform'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.converter-templates',
        convertersType: 'edges'
      },
      resolve: {
        entitiesTableConfig: ConvertersTableConfigResolver
      }
    },
    {
      path: ':entityId',
      component: EntityDetailsPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: entityDetailsPageBreadcrumbLabelFunction,
          icon: 'transform'
        } as BreadCrumbConfig<EntityDetailsPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.converter-templates'
      },
      resolve: {
        entitiesTableConfig: ConvertersTableConfigResolver
      }
    }
  ]
};

const edgeIntegrationTemplatesRoute: Route = {
  path: 'integrations',
  data: {
    breadcrumb: {
      label: 'edge.integration-templates',
      icon: 'input'
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'edge.integration-templates',
        integrationsType: 'edges'
      },
      resolve: {
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
        integrationsType: 'edges'
      },
      resolve: {
        entitiesTableConfig: IntegrationsTableConfigResolver
      }
    }
  ]
};

export const edgesRoute = (root = false): Route => {
  const routeConfig: Route = {
    path: 'edgeManagement',
    data: {
      breadcrumb: root ? {
        label: 'edge.management',
        icon: 'settings_input_antenna'
      } : { skip: true }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: 'instances'
        }
      }
    ]
  };
  const edgeInstancesRoute: Route = {
    path: 'instances',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        labelFunction: (route, translate) =>
          (route.data.customerTitle ? (route.data.customerTitle + ': ') : '') +
          translate.instant(root ? 'edge.instances' : 'edge.edge-instances'),
        icon: 'router'
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
  const allEdgesRoute: Route = {
    path: 'all',
    data: {
      groupType: EntityType.EDGE,
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'edge.all',
        icon: 'router'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'edge.edge-instances'
        },
        resolve: {
          entitiesTableConfig: EdgesTableConfigResolver,
          entityGroup: EntityGroupResolver
        }
      },
      edgeRoute(EntityGroupResolver, EdgesTableConfigResolver),
      ...edgeChildrenRoutes()
    ]
  };
  routeConfig.children.push(edgeInstancesRoute);
  edgeInstancesRoute.children.push(allEdgesRoute);
  edgeInstancesRoute.children.push(edgeGroupsRoute(root));
  if (root) {
    edgeInstancesRoute.children.push(edgeSharedGroupsRoute(root));
    routeConfig.children.push(edgeRuleChainTemplatesRoute);
    routeConfig.children.push(edgeIntegrationTemplatesRoute);
    routeConfig.children.push(edgeConverterTemplatesRoute);
  }
  return routeConfig;
};


@NgModule({
  imports: [RouterModule.forChild([edgesRoute(true)])],
  exports: [RouterModule],
  providers: [
    EdgesTableConfigResolver,
    EdgeTitleResolver,
    {
      provide: 'emptyEdgeTableConfigResolver',
      useValue: (route: ActivatedRouteSnapshot) => null
    }
  ]
})
export class EdgeRoutingModule {
}
