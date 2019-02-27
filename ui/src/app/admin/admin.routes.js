/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import outgoingMailSettingsTemplate from '../admin/outgoing-mail-settings.tpl.html';
import mailTemplateSettingsTemplate from '../admin/mail-template-settings.tpl.html';
import whiteLabelingTemplate from './white-labeling.tpl.html';
import customTranslationTemplate from './custom-translation.tpl.html';

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
                'TENANT_ADMIN': 'home.settings.outgoing-mail',
                'CUSTOMER_USER': 'home.settings.customTranslation'},
            ncyBreadcrumb: {
                label: '{"icon": "settings", "label": "admin.system-settings"}'
            }
        })
        .state('home.settings.outgoing-mail', {
            url: '/outgoing-mail',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
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
        .state('home.settings.mail-template', {
            url: '/mail-template',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: mailTemplateSettingsTemplate,
                    controllerAs: 'vm',
                    controller: 'AdminController'
                }
            },
            data: {
                key: 'mailTemplates',
                pageTitle: 'admin.mail-template-settings'
            },
            ncyBreadcrumb: {
                label: '{"icon": "format_shapes", "label": "admin.mail-templates"}'
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
                pageTitle: 'white-labeling.white-labeling',
                isLoginWl: false
            },
            ncyBreadcrumb: {
                label: '{"icon": "format_paint", "label": "white-labeling.white-labeling"}'
            }
        })
        .state('home.settings.loginWhiteLabel', {
            url: '/loginWhiteLabel',
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
                pageTitle: 'white-labeling.login-white-labeling',
                isLoginWl: true
            },
            ncyBreadcrumb: {
                label: '{"icon": "format_paint", "label": "white-labeling.login-white-labeling"}'
            }
        })
        .state('home.settings.customTranslation', {
            url: '/customTranslation',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: customTranslationTemplate,
                    controllerAs: 'vm',
                    controller: 'CustomTranslationController'
                }
            },
            data: {
                pageTitle: 'custom-translation.custom-translation'
            },
            ncyBreadcrumb: {
                label: '{"icon": "language", "label": "custom-translation.custom-translation"}'
            }
        });
}
