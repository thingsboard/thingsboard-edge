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
export default angular.module('thingsboard.api.signup', [])
    .factory('signUpService', SignUpService)
    .name;

/*@ngInject*/
function SignUpService($http, $q, userService) {

    var service = {
        signup: signup,
        acceptPrivacyPolicy: acceptPrivacyPolicy,
        deleteTenantAccount: deleteTenantAccount,
        isDisplayWelcome: isDisplayWelcome,
        setNotDisplayWelcome: setNotDisplayWelcome
    };

    return service;

    function signup(signupRequest) {
        var deferred = $q.defer();
        $http.post('/api/noauth/signup', signupRequest).then(function success(response) {
            deferred.resolve(response);
        }, function fail(response) {
            deferred.reject(response);
        });
        return deferred.promise;
    }

    function acceptPrivacyPolicy() {
        var deferred = $q.defer();
        $http.post('/api/signup/acceptPrivacyPolicy').then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteTenantAccount() {
        var deferred = $q.defer();
        $http.delete('/api/signup/tenantAccount').then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function isDisplayWelcome() {
        var deferred = $q.defer();
        if (userService.getAuthority() === 'TENANT_ADMIN') {
            $http.get('/api/signup/displayWelcome').then(function success(response) {
                deferred.resolve(response.data);
            }, function fail() {
                deferred.resolve(false);
            });
        } else {
            deferred.resolve(false);
        }
        return deferred.promise;
    }

    function setNotDisplayWelcome() {
        var deferred = $q.defer();
        $http.post('/api/signup/notDisplayWelcome').then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
