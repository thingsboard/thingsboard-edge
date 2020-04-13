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

import loginTemplate from './login.tpl.html';
import resetPasswordTemplate from './reset-password.tpl.html';
import resetPasswordRequestTemplate from './reset-password-request.tpl.html';
import createPasswordTemplate from './create-password.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function LoginRoutes($stateProvider) {
    $stateProvider.state('login', {
        url: '/login?username&password',
        module: 'public',
        views: {
            "@": {
                controller: 'LoginController',
                controllerAs: 'vm',
                templateUrl: loginTemplate
            }
        },
        data: {
            pageTitle: 'login.login'
        }
    }).state('login.resetPasswordRequest', {
        url: '/resetPasswordRequest',
        module: 'public',
        views: {
            "@": {
                controller: 'ResetPasswordRequestController',
                controllerAs: 'vm',
                templateUrl: resetPasswordRequestTemplate
            }
        },
        data: {
            pageTitle: 'login.request-password-reset'
        }
    }).state('login.resetPassword', {
        url: '/resetPassword?resetToken',
        module: 'public',
        views: {
            "@": {
                controller: 'ResetPasswordController',
                controllerAs: 'vm',
                templateUrl: resetPasswordTemplate
            }
        },
        data: {
            pageTitle: 'login.reset-password'
        }
    }).state('login.resetExpiredPassword', {
        url: '/resetExpiredPassword?resetToken',
        module: 'public',
        views: {
            "@": {
                controller: 'ResetPasswordController',
                controllerAs: 'vm',
                templateUrl: resetPasswordTemplate
            }
        },
        data: {
            expiredPassword: true,
            pageTitle: 'login.reset-password'
        }
    }).state('login.createPassword', {
        url: '/createPassword?activateToken',
        module: 'public',
        views: {
            "@": {
                controller: 'CreatePasswordController',
                controllerAs: 'vm',
                templateUrl: createPasswordTemplate
            }
        },
        data: {
            pageTitle: 'login.create-password'
        }
    });
}
