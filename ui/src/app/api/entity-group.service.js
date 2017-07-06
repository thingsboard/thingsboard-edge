/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export default angular.module('thingsboard.api.entityGroup', [])
    .factory('entityGroupService', EntityGroupService)
    .name;

/*@ngInject*/
function EntityGroupService($http, $q) {

    var service = {
        getEntityGroup: getEntityGroup,
        saveEntityGroup: saveEntityGroup,
        deleteEntityGroup: deleteEntityGroup,
        getTenantEntityGroups: getTenantEntityGroups,
        addEntityToEntityGroup: addEntityToEntityGroup,
        addEntitiesToEntityGroup: addEntitiesToEntityGroup,
        removeEntityFromEntityGroup: removeEntityFromEntityGroup,
        removeEntitiesFromEntityGroup: removeEntitiesFromEntityGroup,
        getEntityGroupEntities: getEntityGroupEntities
    }

    return service;

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

    function getTenantEntityGroups(groupType, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/tenant/entityGroups/' + groupType;
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

    function addEntityToEntityGroup(entityGroupId, entityType, entityId) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/' + entityType + '/' + entityId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function addEntitiesToEntityGroup(entityGroupId, entityType, entityIds) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/' + entityType + '/' + entityIds.join();
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeEntityFromEntityGroup(entityGroupId, entityType, entityId) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/' + entityType + '/' + entityId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeEntitiesFromEntityGroup(entityGroupId, entityType, entityIds) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/' + entityType + '/' + entityIds.join();
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityGroupEntities(entityGroupId, entityType, pageLink, ascOrder, config) {
        var deferred = $q.defer();
        var url = '/api/entityGroup/' + entityGroupId + '/' + entityType + '?limit=' + pageLink.limit;

        if (angular.isDefined(pageLink.startTime)) {
            url += '&startTime=' + pageLink.startTime;
        }
        if (angular.isDefined(pageLink.endTime)) {
            url += '&endTime=' + pageLink.endTime;
        }
        if (angular.isDefined(pageLink.idOffset)) {
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
