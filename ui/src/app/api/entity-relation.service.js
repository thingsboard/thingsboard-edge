/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
export default angular.module('thingsboard.api.entityRelation', [])
    .factory('entityRelationService', EntityRelationService)
    .name;

/*@ngInject*/
function EntityRelationService($http, $q) {

    var service = {
        saveRelation: saveRelation,
        deleteRelation: deleteRelation,
        deleteRelations: deleteRelations,
        getRelation: getRelation,
        findByFrom: findByFrom,
        findInfoByFrom: findInfoByFrom,
        findByFromAndType: findByFromAndType,
        findByTo: findByTo,
        findInfoByTo: findInfoByTo,
        findByToAndType: findByToAndType,
        findByQuery: findByQuery,
        findInfoByQuery: findInfoByQuery
    }

    return service;

    function saveRelation(relation) {
        var deferred = $q.defer();
        var url = '/api/relation';
        $http.post(url, relation).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteRelation(fromId, fromType, relationType, toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relation?fromId=' + fromId;
        url += '&fromType=' + fromType;
        url += '&relationType=' + relationType;
        url += '&toId=' + toId;
        url += '&toType=' + toType;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteRelations(entityId, entityType) {
        var deferred = $q.defer();
        var url = '/api/relations?entityId=' + entityId;
        url += '&entityType=' + entityType;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRelation(fromId, fromType, relationType, toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relation?fromId=' + fromId;
        url += '&fromType=' + fromType;
        url += '&relationType=' + relationType;
        url += '&toId=' + toId;
        url += '&toType=' + toType;
        $http.get(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByFrom(fromId, fromType) {
        var deferred = $q.defer();
        var url = '/api/relations?fromId=' + fromId;
        url += '&fromType=' + fromType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findInfoByFrom(fromId, fromType) {
        var deferred = $q.defer();
        var url = '/api/relations/info?fromId=' + fromId;
        url += '&fromType=' + fromType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByFromAndType(fromId, fromType, relationType) {
        var deferred = $q.defer();
        var url = '/api/relations?fromId=' + fromId;
        url += '&fromType=' + fromType;
        url += '&relationType=' + relationType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByTo(toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relations?toId=' + toId;
        url += '&toType=' + toType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findInfoByTo(toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relations/info?toId=' + toId;
        url += '&toType=' + toType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByToAndType(toId, toType, relationType) {
        var deferred = $q.defer();
        var url = '/api/relations?toId=' + toId;
        url += '&toType=' + toType;
        url += '&relationType=' + relationType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByQuery(query) {
        var deferred = $q.defer();
        var url = '/api/relations';
        $http.post(url, query).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findInfoByQuery(query) {
        var deferred = $q.defer();
        var url = '/api/relations/info';
        $http.post(url, query).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}
