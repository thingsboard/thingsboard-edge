/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* eslint-disable import/no-unresolved, import/default */

import outgoingMailSettingsTemplate from '../admin/outgoing-mail-settings.tpl.html';
import whiteLabelingTemplate from './white-labeling.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AdminRoutes($stateProvider) {
    $stateProvider
        .state('home.settings', {
            url: '/settings',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER'],
            redirectTo: {
                'SYS_ADMIN': 'home.settings.outgoing-mail',
                'TENANT_ADMIN': 'home.settings.whiteLabel',
                'CUSTOMER_USER': 'home.settings.whiteLabel'},
            ncyBreadcrumb: {
                label: '{"icon": "settings", "label": "admin.system-settings"}'
            }
        })
        .state('home.settings.outgoing-mail', {
            url: '/outgoing-mail',
            module: 'private',
            auth: ['SYS_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: outgoingMailSettingsTemplate,
                    controllerAs: 'vm',
                    controller: 'AdminController'
                }
            },
            data: {
                key: 'mail',
                pageTitle: 'admin.outgoing-mail-settings'
            },
            ncyBreadcrumb: {
                label: '{"icon": "mail", "label": "admin.outgoing-mail"}'
            }
        })
        .state('home.settings.whiteLabel', {
            url: '/whiteLabel',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: whiteLabelingTemplate,
                    controllerAs: 'vm',
                    controller: 'WhiteLabelingController'
                }
            },
            data: {
                pageTitle: 'white-labeling.white-labeling'
            },
            ncyBreadcrumb: {
                label: '{"icon": "format_paint", "label": "white-labeling.white-labeling"}'
            }
        });
}
