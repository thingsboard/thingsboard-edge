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
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { RuleChainPageComponent } from '@home/pages/rulechain/rulechain-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { RuleChainType } from '@shared/models/rule-chain.models';
import {
  importRuleChainBreadcumbLabelFunction,
  RuleChainMetaDataResolver,
  ruleChainBreadcumbLabelFunction,
  RuleChainImportGuard,
  RuleChainResolver,
  RuleNodeComponentsResolver,
  TooltipsterResolver
} from '@home/pages/rulechain/rulechain-routing.module';
import { ConvertersTableConfigResolver } from '@home/pages/converter/converters-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { IntegrationsTableConfigResolver } from '@home/pages/integration/integrations-table-config.resolver';

const routes: Routes = [
  /* {
    path: 'edgeInstances',
    data: {
      breadcrumb: {
        label: 'edge.edge-instances',
        icon: 'router'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'edge.edge-instances',
          edgesType: 'tenant'
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
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'edge.edge-instances',
          edgesType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: EdgesTableConfigResolver
        }
      },
      {
        path: ':edgeId/assets',
        data: {
          breadcrumb: {
            label: 'edge.assets',
            icon: 'domain'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.assets',
              assetsType: 'edge'
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
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.assets',
              assetsType: 'edge'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':edgeId/devices',
        data: {
          breadcrumb: {
            label: 'edge.devices',
            icon: 'devices_other'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.devices',
              devicesType: 'edge'
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
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.devices',
              devicesType: 'edge'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':edgeId/entityViews',
        data: {
          breadcrumb: {
            label: 'edge.entity-views',
            icon: 'view_quilt'
          },
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.entity-views',
              entityViewsType: 'edge'
            },
            resolve: {
              entitiesTableConfig: EntityViewsTableConfigResolver
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
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'edge.entity-views',
              entityViewsType: 'edge'
            },
            resolve: {
              entitiesTableConfig: EntityViewsTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':edgeId/dashboards',
        data: {
          breadcrumb: {
            label: 'edge.dashboards',
            icon: 'dashboard'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              dashboardsType: 'edge'
            },
            resolve: {
              entitiesTableConfig: DashboardsTableConfigResolver
            },
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
              title: 'edge.dashboard',
              widgetEditMode: false
            },
            resolve: {
              dashboard: DashboardResolver
            }
          }
        ]
      },
      {
        path: ':edgeId/ruleChains',
        data: {
          breadcrumb: {
            label: 'edge.edge-rulechains',
            icon: 'settings_ethernet'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'edge.rulechains',
              ruleChainsType: 'edge'
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
          }
        ]
      }
    ]
  }, */
  {
    path: 'edgeManagement',
    data: {
      breadcrumb: {
        label: 'edge.management',
        icon: 'settings_input_antenna'
      }
    },
    children: [
      {
        path: '',
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/edgeManagement/ruleChains'
        }
      },
      {
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
      },
      {
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
      },
      {
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
      }
    ]
  }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    /* EdgesTableConfigResolver */
  ]
})
export class EdgeRoutingModule {
}
