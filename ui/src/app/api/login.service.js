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
export default angular.module('thingsboard.api.login', [])
    .factory('loginService', LoginService)
    .name;

/*@ngInject*/
function LoginService($http, $q) {

    var service = {
        activate: activate,
        changePassword: changePassword,
        hasUser: hasUser,
        login: login,
        publicLogin: publicLogin,
        resetPassword: resetPassword,
        sendResetPasswordLink: sendResetPasswordLink,
        activateByEmailCode: activateByEmailCode,
        resendEmailActivation: resendEmailActivation
    }

    return service;

    function hasUser() {
        return true;
    }

    function login(user) {
        var deferred = $q.defer();
        var loginRequest = {
            username: user.name,
            password: user.password
        };
        $http.post('/api/auth/login', loginRequest).then(function success(response) {
            deferred.resolve(response);
        }, function fail(response) {
            deferred.reject(response);
        });
        return deferred.promise;
    }

    function publicLogin(publicId) {
        var deferred = $q.defer();
        var pubilcLoginRequest = {
            publicId: publicId
        };
        $http.post('/api/auth/login/public', pubilcLoginRequest).then(function success(response) {
            deferred.resolve(response);
        }, function fail(response) {
            deferred.reject(response);
        });
        return deferred.promise;
    }

    function sendResetPasswordLink(email) {
        var deferred = $q.defer();
        var url = '/api/noauth/resetPasswordByEmail';
        $http.post(url, {email: email}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function resetPassword(resetToken, password) {
        var deferred = $q.defer();
        var url = '/api/noauth/resetPassword';
        $http.post(url, {resetToken: resetToken, password: password}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function activate(activateToken, password) {
        var deferred = $q.defer();
        var url = '/api/noauth/activate';
        $http.post(url, {activateToken: activateToken, password: password}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function changePassword(currentPassword, newPassword) {
        var deferred = $q.defer();
        var url = '/api/auth/changePassword';
        $http.post(url, {currentPassword: currentPassword, newPassword: newPassword}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function activateByEmailCode(emailCode) {
        var deferred = $q.defer();
        var url = '/api/noauth/activateByEmailCode?emailCode=' + emailCode;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function resendEmailActivation(email) {
        var deferred = $q.defer();
        var url = '/api/noauth/resendEmailActivation?email=' + email;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
