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
// import thingsboardApiUser  from './user.service';

export default angular.module('thingsboard.api.self-registration', [])
    .factory('selfRegistrationService', selfRegistrationService)
    .name;

/*@ngInject*/
function selfRegistrationService($http, $q, roleService, entityGroupService, tenantService, $rootScope, $state) {

    var service = {
        getRegistrationLink: getRegistrationLink,
        isAvailablePage: isAvailablePage,
        loadSelfRegistrationParams: loadSelfRegistrationParams,
        loadPrivacyPolicy: loadPrivacyPolicy,
        saveSelfRegistrationParams: saveSelfRegistrationParams,
        getSelfRegistrationParams: getSelfRegistrationParams,
        getPermissionUser: getPermissionUser
    };

    return service;

    function getRegistrationLink(domainName) {
        return domainName + "/signup";
    }

    function loadSelfRegistrationParams() {
        var deferred = $q.defer();
        var url = '/api/noauth/selfRegistration/signUpSelfRegistrationParams';
        $http.get(url, null).then(function success(response) {
            $rootScope.signUpParams = response.data;
            $rootScope.signUpParams.activate = $rootScope.signUpParams.captchaSiteKey !== null;
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function loadPrivacyPolicy() {
        var deferred = $q.defer();
        var url = '/api/noauth/selfRegistration/privacyPolicy';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function isAvailablePage(){
        var deferred = $q.defer();
        if($rootScope.signUpParams){
            $rootScope.signUpParams.activate ? deferred.resolve():$state.go('login');
        } else {
            loadSelfRegistrationParams().then(function success(){
                $rootScope.signUpParams.activate ? deferred.resolve():$state.go('login');
            });
        }
        return deferred.promise;
    }

    function saveSelfRegistrationParams(selfRegistrationParams) {
        var deferred = $q.defer();
        var url = '/api/selfRegistration/selfRegistrationParams';
        $http.post(url, selfRegistrationParams).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getSelfRegistrationParams() {
        var deferred = $q.defer();
        var url = '/api/selfRegistration/selfRegistrationParams';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getPermissionUser (permissionId) {
        var deferred = $q.defer();
        let tasks = [];
        if(angular.isDefined(permissionId.roleId.id)) {
            tasks.push(roleService.getRole(permissionId.roleId.id, true, {}));
        }
        if(permissionId.entityGroupId && permissionId.entityGroupId.id) {
            tasks.push(entityGroupService.getEntityGroup(permissionId.entityGroupId.id));
        }
        $q.all(tasks).then(function success (data) {
            if(angular.isDefined(permissionId.roleId.id)) {
                permissionId.role = data.shift();
            }
            if(permissionId.entityGroupId && permissionId.entityGroupId.id) {
                permissionId.entity = data.shift();
                if(angular.isUndefined(permissionId.entityGroupOwnerId)) {
                    permissionId.entityGroupOwnerId = permissionId.entity.ownerId;
                }
            }
            deferred.resolve(permissionId);
        });
        return deferred.promise;
    }
}