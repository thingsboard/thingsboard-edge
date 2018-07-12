/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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

import usersTemplate from '../user/users.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function UserRoutes($stateProvider) {

    $stateProvider
        .state('home.tenants.users', {
            url: '/:tenantId/users',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: usersTemplate,
                    controllerAs: 'vm',
                    controller: 'UserController'
                }
            },
            data: {
                usersType: 'tenant',
                searchEnabled: true,
                pageTitle: 'user.tenant-admins'
            },
            ncyBreadcrumb: {
                label: '{"icon": "account_circle", "label": "user.tenant-admins"}'
            }
        })
        .state('home.customers.users', {
            url: '/:customerId/users',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: usersTemplate,
                    controllerAs: 'vm',
                    controller: 'UserController'
                }
            },
            data: {
                usersType: 'customer',
                searchEnabled: true,
                pageTitle: 'user.customer-users'
            },
            ncyBreadcrumb: {
                label: '{"icon": "account_circle", "label": "user.customer-users"}'
            }
        })
        .state('home.customerGroups.customerGroup.users', {
            url: '/:customerId/users',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: usersTemplate,
                    controllerAs: 'vm',
                    controller:
                    /*@ngInject*/
                        function($scope, $stateParams, $controller, entityGroup) {
                            var ctrl = $controller('UserController as vm',{$scope: $scope, $stateParams: $stateParams});
                            ctrl.entityGroup = entityGroup;
                            return ctrl;
                        }
                }
            },
            data: {
                usersType: 'customer',
                searchEnabled: true,
                pageTitle: 'user.customer-users'
            },
            ncyBreadcrumb: {
                label: '{"icon": "account_circle", "label": "user.customer-users"}'
            }
        });

}
