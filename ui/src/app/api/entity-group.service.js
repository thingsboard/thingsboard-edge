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
export default angular.module('thingsboard.api.entityGroup', [])
    .factory('entityGroupService', EntityGroupService)
    .name;

/*@ngInject*/
function EntityGroupService($http, $q, utils) {

    var service = {
        getOwners: getOwners,
        getEntityGroup: getEntityGroup,
        saveEntityGroup: saveEntityGroup,
        deleteEntityGroup: deleteEntityGroup,
        getEntityGroups: getEntityGroups,
        getEntityGroupsByIds: getEntityGroupsByIds,
        getEntityGroupsByOwnerId: getEntityGroupsByOwnerId,
        getEntityGroupsByPageLink: getEntityGroupsByPageLink,
        addEntityToEntityGroup: addEntityToEntityGroup,
        addEntitiesToEntityGroup: addEntitiesToEntityGroup,
        removeEntityFromEntityGroup: removeEntityFromEntityGroup,
        removeEntitiesFromEntityGroup: removeEntitiesFromEntityGroup,
        getEntityGroupEntity: getEntityGroupEntity,
        getEntityGroupEntities: getEntityGroupEntities
    }

    return service;

    function getOwners(pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/owners?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroup(entityGroupId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveEntityGroup(entityGroup, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroup';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, entityGroup, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteEntityGroup(entityGroupId) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroups(groupType, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroups/' + groupType;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroupsByIds(entityGroupIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<entityGroupIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += entityGroupIds[i];
        }
        var url = '/api/entityGroups?entityGroupIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var entities = response.data;
            entities.sort(function (entity1, entity2) {
                var id1 =  entity1.id.id;
                var id2 =  entity2.id.id;
                var index1 = entityGroupIds.indexOf(id1);
                var index2 = entityGroupIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(entities);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroupsByOwnerId(ownerType, ownerId, groupType, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroups/' + ownerType + '/' + ownerId + '/' + groupType;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroupsByPageLink(pageLink, groupType, ignoreErrors, config) {
        var deferred = $q.defer();
        getEntityGroups(groupType, ignoreErrors, config).then(
            function success(entityGroups) {
                utils.filterSearchTextEntities(entityGroups, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function addEntityToEntityGroup(entityGroupId, entityId) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/addEntities';
        $http.post(url, [entityId]).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function addEntitiesToEntityGroup(entityGroupId, entityIds) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/addEntities';
        $http.post(url, entityIds).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeEntityFromEntityGroup(entityGroupId, entityId) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/deleteEntities';
        $http.post(url, [entityId]).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeEntitiesFromEntityGroup(entityGroupId, entityIds) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/deleteEntities';
        $http.post(url, entityIds).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroupEntity(entityGroupId, entityId, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/' + entityId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroupEntities(entityGroupId, pageLink, ascOrder, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/entities?limit=' + pageLink.limit;

        if (angular.isDefined(pageLink.startTime) && pageLink.startTime != null) {
            url += '&startTime=' + pageLink.startTime;
        }
        if (angular.isDefined(pageLink.endTime) && pageLink.endTime != null) {
            url += '&endTime=' + pageLink.endTime;
        }
        if (angular.isDefined(pageLink.idOffset) && pageLink.idOffset != null) {
            url += '&offset=' + pageLink.idOffset;
        }
        if (angular.isDefined(ascOrder) && ascOrder != null) {
            url += '&ascOrder=' + (ascOrder ? 'true' : 'false');
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}
