/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import widgetLibraryTemplate from './widget-library.tpl.html';
import widgetEditorTemplate from './widget-editor.tpl.html';
import dashboardTemplate from '../dashboard/dashboard.tpl.html';
import widgetsBundlesTemplate from './widgets-bundles.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function WidgetLibraryRoutes($stateProvider) {
    $stateProvider
        .state('home.widgets-bundles', {
            url: '/widgets-bundles',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: widgetsBundlesTemplate,
                    controller: 'WidgetsBundleController',
                    controllerAs: 'vm'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'widgets-bundle.widgets-bundles'
            },
            ncyBreadcrumb: {
                label: '{"icon": "now_widgets", "label": "widgets-bundle.widgets-bundles"}'
            }
        })
        .state('home.widgets-bundles.widget-types', {
            url: '/:widgetsBundleId/widgetTypes',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: widgetLibraryTemplate,
                    controller: 'WidgetLibraryController',
                    controllerAs: 'vm'
                }
            },
            data: {
                searchEnabled: false,
                pageTitle: 'widget.widget-library'
            },
            ncyBreadcrumb: {
                label: '{"icon": "now_widgets", "label": "{{ vm.widgetsBundle.title }}", "translate": "false"}'
            }
        })
        .state('home.widgets-bundles.widget-types.widget-type', {
            url: '/:widgetTypeId',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: widgetEditorTemplate,
                    controller: 'WidgetEditorController',
                    controllerAs: 'vm'
                }
            },
            params: {
                widgetType: null
            },
            data: {
                searchEnabled: false,
                pageTitle: 'widget.editor'
            },
            ncyBreadcrumb: {
                label: '{"icon": "insert_chart", "label": "{{ vm.widget.widgetName }}", "translate": "false"}'
            }
        })
        .state('widgetEditor', {
            url: '/widget-editor',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "@": {
                    templateUrl: dashboardTemplate,
                    controller: 'DashboardController',
                    controllerAs: 'vm'
                }
            },
            data: {
                widgetEditMode: true,
                searchEnabled: false,
                pageTitle: 'widget.editor'
            }
        })
}
