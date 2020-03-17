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

import signupTemplate from './signup.tpl.html';
import emailVerificationTemplate from './email-verification.tpl.html';
import emailVerifiedTemplate from './email-verified.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function SignUpRoutes($stateProvider) {

    $stateProvider.state('signup', {
        url: '/signup',
        module: 'public',
        views: {
            "@": {
                controller: 'SignUpController',
                controllerAs: 'vm',
                templateUrl: signupTemplate
            }
        },
        data: {
            pageTitle: 'signup.signup'
        },
        resolve: {
            /*@ngInject*/
            signupParams: function(selfRegistrationService) {
                return selfRegistrationService.isAvailablePage()
            }
        }
    }).state('signup.emailVerification', {
        url: '/emailVerification?email',
        module: 'public',
        views: {
            "@": {
                controller: 'EmailVerificationController',
                controllerAs: 'vm',
                templateUrl: emailVerificationTemplate
            }
        },
        data: {
            pageTitle: 'signup.email-verification'
        }
    }).state('signup.emailVerified', {
        url: '/emailVerified?emailCode',
        module: 'public',
        views: {
            "@": {
                controller: 'EmailVerifiedController',
                controllerAs: 'vm',
                templateUrl: emailVerifiedTemplate
            }
        },
        data: {
            pageTitle: 'signup.account-activation-title'
        }
    });
}
