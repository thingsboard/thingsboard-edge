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
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.entityView', [thingsboardTypes])
    .factory('entityViewService', EntityViewService)
    .name;

/*@ngInject*/
function EntityViewService($http, $q, $window, userService, attributeService, customerService, types) {

    var service = {
        assignEntityViewToCustomer: assignEntityViewToCustomer,
        deleteEntityView: deleteEntityView,
        getCustomerEntityViews: getCustomerEntityViews,
        getEntityView: getEntityView,
        getEntityViews: getEntityViews,
        getTenantEntityViews: getTenantEntityViews,
        saveEntityView: saveEntityView,
        unassignEntityViewFromCustomer: unassignEntityViewFromCustomer,
        makeEntityViewPublic: makeEntityViewPublic,
        getEntityViewAttributes: getEntityViewAttributes,
        subscribeForEntityViewAttributes: subscribeForEntityViewAttributes,
        unsubscribeForEntityViewAttributes: unsubscribeForEntityViewAttributes,
        findByQuery: findByQuery,
        getEntityViewTypes: getEntityViewTypes
    }

    return service;

    function getTenantEntityViews(pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/tenant/entityViews?limit=' + pageLink.limit;
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

    function getCustomerEntityViews(customerId, pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/entityViews?limit=' + pageLink.limit;
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

    function getEntityView(entityViewId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityView/' + entityViewId;
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

    function getEntityViews(entityViewIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<entityViewIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += entityViewIds[i];
        }
        var url = '/api/entityViews?entityViewIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var entities = response.data;
            entities.sort(function (entity1, entity2) {
                var id1 =  entity1.id.id;
                var id2 =  entity2.id.id;
                var index1 = entityViewIds.indexOf(id1);
                var index2 = entityViewIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(entities);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }


    function saveEntityView(entityView) {
        var deferred = $q.defer();
        var url = '/api/entityView';

        $http.post(url, entityView).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteEntityView(entityViewId) {
        var deferred = $q.defer();
        var url = '/api/entityView/' + entityViewId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignEntityViewToCustomer(customerId, entityViewId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/entityView/' + entityViewId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignEntityViewFromCustomer(entityViewId) {
        var deferred = $q.defer();
        var url = '/api/customer/entityView/' + entityViewId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeEntityViewPublic(entityViewId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/public/entityView/' + entityViewId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, null, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityViewAttributes(entityViewId, attributeScope, query, successCallback, config) {
        return attributeService.getEntityAttributes(types.entityType.entityView, entityViewId, attributeScope, query, successCallback, config);
    }

    function subscribeForEntityViewAttributes(entityViewId, attributeScope) {
        return attributeService.subscribeForEntityAttributes(types.entityType.entityView, entityViewId, attributeScope);
    }

    function unsubscribeForEntityViewAttributes(subscriptionId) {
        attributeService.unsubscribeForEntityAttributes(subscriptionId);
    }

    function findByQuery(query, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityViews';
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

    function getEntityViewTypes(config) {
        var deferred = $q.defer();
        var url = '/api/entityView/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}
