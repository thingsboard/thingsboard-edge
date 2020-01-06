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
export default angular.module('thingsboard.api.userPermissions', [])
    .factory('userPermissionsService', UserPermissionsService)
    .name;

/*@ngInject*/
function UserPermissionsService($http, $q, types, securityTypes) {

    var operationsByResource;
    var allowedGroupRoleOperations;
    var allowedGroupOwnerOnlyOperations;
    var allowedGroupOwnerOnlyGroupOperations;
    var allowedResources;
    var userPermissions;
    var userOwnerId;

    var service = {
        loadPermissionsInfo: loadPermissionsInfo,
        getOperationsByResource: getOperationsByResource,
        getAllowedGroupRoleOperations: getAllowedGroupRoleOperations,
        getAllowedResources: getAllowedResources,
        hasReadGroupsPermission: hasReadGroupsPermission,
        hasReadGenericPermission: hasReadGenericPermission,
        hasGenericPermission: hasGenericPermission,
        hasGenericEntityGroupTypePermission: hasGenericEntityGroupTypePermission,
        hasGenericEntityGroupPermission: hasGenericEntityGroupPermission,
        hasEntityGroupPermission: hasEntityGroupPermission,
        hasGroupEntityPermission: hasGroupEntityPermission,
        isDirectlyOwnedGroup: isDirectlyOwnedGroup,
        isOwnedGroup: isOwnedGroup,
        getUserOwnerId: getUserOwnerId
    };

    function loadPermissionsInfo() {
        var deferred = $q.defer();
        var url = '/api/permissions/allowedPermissions';
        $http.get(url).then(function success(response) {
            operationsByResource = response.data.operationsByResource;
            allowedGroupRoleOperations = response.data.allowedForGroupRoleOperations;
            allowedGroupOwnerOnlyOperations = response.data.allowedForGroupOwnerOnlyOperations;
            allowedGroupOwnerOnlyGroupOperations = response.data.allowedForGroupOwnerOnlyGroupOperations;
            allowedResources = response.data.allowedResources;
            userPermissions = response.data.userPermissions;
            userOwnerId = response.data.userOwnerId;
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

    function hasGenericEntityGroupTypePermission(operation, groupType) {
        if (!groupType) {
            return false;
        }
        var resource = securityTypes.groupResourceByGroupType[groupType];
        return hasGenericPermission(resource, operation);
    }

    function hasGenericEntityGroupPermission(operation, entityGroup) {
        if (!entityGroup) {
            return false;
        }
        return hasGenericEntityGroupTypePermission(operation, entityGroup.type);
    }

    function hasEntityGroupPermission(operation, entityGroup) {
        return checkEntityGroupPermission(operation, entityGroup, true);
    }

    function hasGroupEntityPermission(operation, entityGroup) {
        return checkEntityGroupPermission(operation, entityGroup, false);
    }

    function isDirectlyOwnedGroup(entityGroup) {
        if (userOwnerId && entityGroup && entityGroup.ownerId) {
            return idsEqual(userOwnerId, entityGroup.ownerId);
        } else {
            return false;
        }
    }

    function isOwnedGroup(entityGroup) {
        if (!entityGroup) {
            return false;
        }
        return isCurrentUserOwner(entityGroup);
    }

    function getUserOwnerId() {
        return userOwnerId;
    }

    function checkEntityGroupPermission(operation, entityGroup, isGroup) {
        if (!entityGroup) {
            return false;
        }
        var resource;
        if (isGroup) {
            resource = securityTypes.groupResourceByGroupType[entityGroup.type];
        } else {
            resource = securityTypes.resourceByEntityType[entityGroup.type];
        }
        if (isCurrentUserOwner(entityGroup)) {
            if (hasGenericPermission(resource, operation)) {
                return true;
            }
        }
        return hasGroupPermissions(entityGroup, operation, isGroup);
    }

    function hasGroupPermissions(entityGroup, operation, isGroup) {
        if (!allowedGroupRoleOperations || allowedGroupRoleOperations.indexOf(operation) == -1) {
            return false;
        }
        if (isGroup) {
            if (allowedGroupOwnerOnlyGroupOperations && allowedGroupOwnerOnlyGroupOperations.indexOf(operation) > -1) {
                return false;
            }
        } else {
            if (allowedGroupOwnerOnlyOperations && allowedGroupOwnerOnlyOperations.indexOf(operation) > -1) {
                if (!isCurrentUserOwner(entityGroup)) {
                    return false;
                }
            }
        }
        if (userPermissions && userPermissions.groupPermissions) {
            var permissionInfo = userPermissions.groupPermissions[entityGroup.id.id];
            return permissionInfo && checkOperation(permissionInfo.operations, operation);
        }
        return false;
    }

    function isCurrentUserOwner(entityGroup) {
        var groupOwnerIds = entityGroup.ownerIds;
        if (userOwnerId && groupOwnerIds) {
            return containsId(groupOwnerIds, userOwnerId);
        } else {
            return false;
        }
    }

    function containsId(idsArray, id) {
        for (var i=0;i<idsArray.length;i++) {
            if (idsEqual(idsArray[i], id)) {
                return true;
            }
        }
        return false;
    }

    function idsEqual(id1, id2) {
        return id1.id === id2.id && id1.entityType === id2.entityType;
    }

    return service;
}