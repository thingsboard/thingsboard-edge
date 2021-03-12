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
export default angular.module('thingsboard.api.edge', [])
    .factory('edgeService', EdgeService)
    .name;

/*@ngInject*/
function EdgeService($http, $q, $timeout, customerService) {

    var service = {
        getEdges: getEdges,
        getEdge: getEdge,
        deleteEdge: deleteEdge,
        saveEdge: saveEdge,
        getEdgeTypes: getEdgeTypes,
        getTenantEdges: getTenantEdges,
        getCustomerEdges: getCustomerEdges,
        getUserEdges: getUserEdges,
        assignEdgeToCustomer: assignEdgeToCustomer,
        findByQuery: findByQuery,
        findByName: findByName,
        unassignEdgeFromCustomer: unassignEdgeFromCustomer,
        makeEdgePublic: makeEdgePublic,
        setRootRuleChain: setRootRuleChain,
        getEdgeEvents: getEdgeEvents,
        syncEdge: syncEdge,
        findMissingToRelatedRuleChains: findMissingToRelatedRuleChains,
        transformEdgeEventToPromise: transformEdgeEventToPromise
    };

    return service;

    function getEdges(pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/edges?limit=' + pageLink.limit;
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

    function getEdge(edgeId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveEdge(edge, entityGroupId, config, ignoreErrors) {
        var deferred = $q.defer();
        var url = '/api/edge';
        if (entityGroupId) {
            url += '?entityGroupId=' + entityGroupId;
        }
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, edge, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteEdge(edgeId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getEdgeTypes(config) {
        var deferred = $q.defer();
        var url = '/api/edge/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getTenantEdges(pageLink, applyCustomersInfo, type, config) {
        var deferred = $q.defer();
        var url = '/api/tenant/edges?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        if (angular.isDefined(type) && type.length) {
            url += '&type=' + type;
        }
        $http.get(url, config).then(function success(response) {
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomersInfo(response.data.data).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerEdges(customerId, pageLink, applyCustomersInfo, type, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/edges?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        if (angular.isDefined(type) && type.length) {
            url += '&type=' + type;
        }
        $http.get(url, config).then(function success(response) {
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomerInfo(response.data.data, customerId).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });

        return deferred.promise;
    }

    function findByQuery(query, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/edges';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, query, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByName(edgeName, config) {
        config = config || {};
        var deferred = $q.defer();
        var url = '/api/tenant/edges?edgeName=' + edgeName;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getUserEdges(pageLink, config, type) {
        var deferred = $q.defer();
        var url = '/api/user/edges?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        if (angular.isDefined(type) && type.length) {
            url += '&type=' + type;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignEdgeToCustomer(customerId, edgeId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/edge/' + edgeId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignEdgeFromCustomer(edgeId) {
        var deferred = $q.defer();
        var url = '/api/customer/edge/' + edgeId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeEdgePublic(edgeId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/edge/' + edgeId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function setRootRuleChain(edgeId, ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/' + ruleChainId + '/root';
        $http.post(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEdgeEvents(edgeId, pageLink) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/events' + '?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.startTime) && pageLink.startTime != null) {
            url += '&startTime=' + pageLink.startTime;
        }
        if (angular.isDefined(pageLink.endTime) && pageLink.endTime != null) {
            url += '&endTime=' + pageLink.endTime;
        }
        if (angular.isDefined(pageLink.idOffset) && pageLink.idOffset != null) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function syncEdge(edgeId) {
        var deferred = $q.defer();
        var url = '/api/edge/sync/' + edgeId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function findMissingToRelatedRuleChains(edgeId) {
        var deferred = $q.defer();
        var url = '/api/edge/missingToRelatedRuleChains/' + edgeId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function transformEdgeEventToPromise(data) {
        var deferred = $q.defer();
        $timeout(function() {
            if (data.body) {
                deferred.resolve(data.body);
            } else {
                deferred.reject();
            }
        }, 0);
        return deferred.promise;
    }
}
