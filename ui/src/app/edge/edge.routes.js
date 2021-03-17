/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
/* eslint-disable import/no-unresolved, import/default */
/* USED FOR THE PURPOSE OF MERGING CE-PE
import edgesTemplate from './edges.tpl.html';
import entityViewsTemplate from "../entity-view/entity-views.tpl.html";
import devicesTemplate from "../device/devices.tpl.html";
import assetsTemplate from "../asset/assets.tpl.html";
import dashboardsTemplate from "../dashboard/dashboards.tpl.html";
import dashboardTemplate from "../dashboard/dashboard.tpl.html";

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
/*export default function EdgeRoutes($stateProvider, types) {
    $stateProvider
        .state('home.edges', {
            url: '/edges',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: edgesTemplate,
                    controller: 'EdgeController',
                    controllerAs: 'vm'
                }
            },
            data: {
                edgesType: 'tenant',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.edge,
                pageTitle: 'edge.edge-instances'
            },
            ncyBreadcrumb: {
                label: '{"icon": "router", "label": "edge.edge-instances"}'
            }
        }).state('home.edges.entityViews', {
            url: '/:edgeId/entityViews',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: entityViewsTemplate,
                    controllerAs: 'vm',
                    controller: 'EntityViewController'
                }
            },
            data: {
                entityViewsType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.entityView,
                pageTitle: 'edge.entity-views'
            },
            ncyBreadcrumb: {
                label: '{"icon": "view_quilt", "label": "edge.entity-views"}'
            }
        }).state('home.edges.devices', {
            url: '/:edgeId/devices',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: devicesTemplate,
                    controllerAs: 'vm',
                    controller: 'DeviceController'
                }
            },
            data: {
                devicesType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.device,
                pageTitle: 'edge.devices'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "edge.devices"}'
            }
        }).state('home.edges.assets', {
            url: '/:edgeId/assets',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: assetsTemplate,
                    controllerAs: 'vm',
                    controller: 'AssetController'
                }
            },
            data: {
                assetsType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.asset,
                pageTitle: 'edge.assets'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "edge.assets"}'
            }
        }).state('home.edges.dashboards', {
            url: '/:edgeId/dashboards',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: dashboardsTemplate,
                    controllerAs: 'vm',
                    controller: 'DashboardsController'
                }
            },
            data: {
                dashboardsType: 'edge',
                searchEnabled: true,
                pageTitle: 'edge.dashboards'
            },
            ncyBreadcrumb: {
                label: '{"icon": "dashboard", "label": "edge.dashboards"}'
            }
        }).state('home.edges.dashboards.dashboard', {
            url: '/:dashboardId?state',
            reloadOnSearch: false,
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: dashboardTemplate,
                    controller: 'DashboardController',
                    controllerAs: 'vm'
                }
            },
            data: {
                widgetEditMode: false,
                searchEnabled: false,
                pageTitle: 'dashboard.dashboard',
                dashboardsType: 'edge',
            },
            ncyBreadcrumb: {
                label: '{"icon": "dashboard", "label": "{{ vm.dashboard.title }}", "translate": "false"}'
            }
        }).state('home.customers.edges', {
            url: '/:customerId/edges',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: edgesTemplate,
                    controllerAs: 'vm',
                    controller: 'EdgeController'
                }
            },
            data: {
                edgesType: 'customer',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.edge,
                pageTitle: 'customer.edges'
            },
            ncyBreadcrumb: {
                label: '{"icon": "router", "label": "{{ vm.customerEdgesTitle }}", "translate": "false"}'
            }
        });
}
*/
