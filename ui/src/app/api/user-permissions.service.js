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


export default angular.module('thingsboard.api.userPermissions', [])
    .factory('userPermissionsService', UserPermissionsService)
    .name;

/*@ngInject*/
function UserPermissionsService($http, $q, securityTypes) {

    var operationsByResource;
    var allowedGroupRoleOperations;
    var allowedResources;
    var userPermissions;

    var service = {
        loadPermissionsInfo: loadPermissionsInfo,
        getOperationsByResource: getOperationsByResource,
        getAllowedGroupRoleOperations: getAllowedGroupRoleOperations,
        getAllowedResources: getAllowedResources,
        hasReadGroupsPermission: hasReadGroupsPermission,
        hasReadGenericPermission: hasReadGenericPermission,
        hasGenericPermission: hasGenericPermission
    };

    function loadPermissionsInfo() {
        var deferred = $q.defer();
        var url = '/api/permissions/allowedPermissions';
        $http.get(url).then(function success(response) {
            operationsByResource = response.data.operationsByResource;
            allowedGroupRoleOperations = response.data.allowedForGroupRoleOperations;
            allowedResources = response.data.allowedResources;
            userPermissions = response.data.userPermissions;
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });

        return deferred.promise;
    }

    function getOperationsByResource(resource) {
        if (resource && operationsByResource && operationsByResource[resource]) {
            return operationsByResource[resource];
        } else {
            return [];
        }
    }

    function getAllowedGroupRoleOperations() {
        if (allowedGroupRoleOperations) {
            return allowedGroupRoleOperations;
        } else {
            return [];
        }
    }

    function getAllowedResources() {
        if (allowedResources) {
            return allowedResources;
        } else {
            return [];
        }
    }

    function hasReadGroupsPermission(entityType) {
        if (userPermissions) {
            var readGroupPermissions = userPermissions.readGroupPermissions;
            var groupTypePermissionInfo = readGroupPermissions[entityType];
            return groupTypePermissionInfo.hasGenericRead || groupTypePermissionInfo.entityGroupIds.length;
        } else {
            return false;
        }
    }

    function hasReadGenericPermission(resource) {
        if (userPermissions) {
            return hasGenericPermission(resource, securityTypes.operation.read);
        } else {
            return false;
        }
    }

    function hasGenericPermission(resource, operation) {
        if (userPermissions) {
            return hasGenericResourcePermission(resource, operation) || hasGenericAllPermission(operation);
        } else {
            return false;
        }
    }

    function hasGenericAllPermission(operation) {
        var operations = userPermissions.genericPermissions[securityTypes.resource.all];
        if (operations) {
            return checkOperation(operations, operation);
        } else {
            return false;
        }
    }

    function hasGenericResourcePermission(resource, operation) {
        var operations = userPermissions.genericPermissions[resource];
        if (operations) {
            return checkOperation(operations, operation);
        } else {
            return false;
        }
    }

    function checkOperation(operations, operation) {
        return operations.indexOf(securityTypes.operation.all) > -1 || operations.indexOf(operation) > -1;
    }

    return service;
}