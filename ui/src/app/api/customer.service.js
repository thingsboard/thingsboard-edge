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
export default angular.module('thingsboard.api.customer', [])
    .factory('customerService', CustomerService)
    .name;

/*@ngInject*/
function CustomerService($http, $q, types) {

    var service = {
        getCustomers: getCustomers,
        getCustomer: getCustomer,
        getShortCustomerInfo: getShortCustomerInfo,
        applyAssignedCustomersInfo: applyAssignedCustomersInfo,
        applyAssignedCustomerInfo: applyAssignedCustomerInfo,
        deleteCustomer: deleteCustomer,
        saveCustomer: saveCustomer
    }

    return service;

    function getCustomers(pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/customers?limit=' + pageLink.limit;
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

    function getCustomer(customerId, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getShortCustomerInfo(customerId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/shortInfo';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function applyAssignedCustomersInfo(items) {
        var deferred = $q.defer();
        var assignedCustomersMap = {};
        function loadNextCustomerInfoOrComplete(i) {
            i++;
            if (i < items.length) {
                loadNextCustomerInfo(i);
            } else {
                deferred.resolve(items);
            }
        }

        function loadNextCustomerInfo(i) {
            var item = items[i];
            item.assignedCustomer = {};
            if (item.customerId && item.customerId.id != types.id.nullUid) {
                item.assignedCustomer.id = item.customerId.id;
                var assignedCustomer = assignedCustomersMap[item.customerId.id];
                if (assignedCustomer){
                    item.assignedCustomer = assignedCustomer;
                    loadNextCustomerInfoOrComplete(i);
                } else {
                    getShortCustomerInfo(item.customerId.id).then(
                        function success(info) {
                            assignedCustomer = {
                                id: item.customerId.id,
                                title: info.title,
                                isPublic: info.isPublic
                            };
                            assignedCustomersMap[assignedCustomer.id] = assignedCustomer;
                            item.assignedCustomer = assignedCustomer;
                            loadNextCustomerInfoOrComplete(i);
                        },
                        function fail() {
                            loadNextCustomerInfoOrComplete(i);
                        }
                    );
                }
            } else {
                loadNextCustomerInfoOrComplete(i);
            }
        }
        if (items.length > 0) {
            loadNextCustomerInfo(0);
        } else {
            deferred.resolve(items);
        }
        return deferred.promise;
    }

    function applyAssignedCustomerInfo(items, customerId) {
        var deferred = $q.defer();
        getShortCustomerInfo(customerId).then(
            function success(info) {
                var assignedCustomer = {
                    id: customerId,
                    title: info.title,
                    isPublic: info.isPublic
                }
                items.forEach(function(item) {
                    item.assignedCustomer = assignedCustomer;
                });
                deferred.resolve(items);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function saveCustomer(customer) {
        var deferred = $q.defer();
        var url = '/api/customer';
        $http.post(url, customer).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteCustomer(customerId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

}
