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

export default angular.module('thingsboard.api.device', [thingsboardTypes])
    .factory('deviceService', DeviceService)
    .name;

/*@ngInject*/
function DeviceService($http, $q, $window, userService, attributeService, customerService, types) {

    var service = {
        assignDeviceToCustomer: assignDeviceToCustomer,
        deleteDevice: deleteDevice,
        getCustomerDevices: getCustomerDevices,
        getDevice: getDevice,
        getDevices: getDevices,
        getUserDevices: getUserDevices,
        getDeviceCredentials: getDeviceCredentials,
        getTenantDevices: getTenantDevices,
        saveDevice: saveDevice,
        saveDeviceCredentials: saveDeviceCredentials,
        unassignDeviceFromCustomer: unassignDeviceFromCustomer,
        makeDevicePublic: makeDevicePublic,
        getDeviceAttributes: getDeviceAttributes,
        subscribeForDeviceAttributes: subscribeForDeviceAttributes,
        unsubscribeForDeviceAttributes: unsubscribeForDeviceAttributes,
        saveDeviceAttributes: saveDeviceAttributes,
        deleteDeviceAttributes: deleteDeviceAttributes,
        sendOneWayRpcCommand: sendOneWayRpcCommand,
        sendTwoWayRpcCommand: sendTwoWayRpcCommand,
        findByQuery: findByQuery,
        getDeviceTypes: getDeviceTypes
    }

    return service;

    function getTenantDevices(pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/tenant/devices?limit=' + pageLink.limit;
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

    function getCustomerDevices(customerId, pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/devices?limit=' + pageLink.limit;
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

    function getDevice(deviceId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId;
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

    function getDevices(deviceIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<deviceIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += deviceIds[i];
        }
        var url = '/api/devices?deviceIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var devices = response.data;
            devices.sort(function (device1, device2) {
               var id1 =  device1.id.id;
               var id2 =  device2.id.id;
               var index1 = deviceIds.indexOf(id1);
               var index2 = deviceIds.indexOf(id2);
               return index1 - index2;
            });
            deferred.resolve(devices);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getUserDevices(pageLink, config, type) {
        var deferred = $q.defer();
        var url = '/api/user/devices?limit=' + pageLink.limit;
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

    function saveDevice(device) {
        var deferred = $q.defer();
        var url = '/api/device';
        $http.post(url, device).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteDevice(deviceId) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDeviceCredentials(deviceId, sync) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId + '/credentials';
        if (sync) {
            var request = new $window.XMLHttpRequest();
            request.open('GET', url, false);
            request.setRequestHeader("Accept", "application/json, text/plain, */*");
            userService.setAuthorizationRequestHeader(request);
            request.send(null);
            if (request.status === 200) {
                deferred.resolve(angular.fromJson(request.responseText));
            } else {
                deferred.reject();
            }
        } else {
            $http.get(url, null).then(function success(response) {
                deferred.resolve(response.data);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred.promise;
    }

    function saveDeviceCredentials(deviceCredentials) {
        var deferred = $q.defer();
        var url = '/api/device/credentials';
        $http.post(url, deviceCredentials).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignDeviceToCustomer(customerId, deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/device/' + deviceId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignDeviceFromCustomer(deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/device/' + deviceId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDevicePublic(deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/device/' + deviceId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDeviceAttributes(deviceId, attributeScope, query, successCallback, config) {
        return attributeService.getEntityAttributes(types.entityType.device, deviceId, attributeScope, query, successCallback, config);
    }

    function subscribeForDeviceAttributes(deviceId, attributeScope) {
        return attributeService.subscribeForEntityAttributes(types.entityType.device, deviceId, attributeScope);
    }

    function unsubscribeForDeviceAttributes(subscriptionId) {
        attributeService.unsubscribeForEntityAttributes(subscriptionId);
    }

    function saveDeviceAttributes(deviceId, attributeScope, attributes) {
        return attributeService.saveEntityAttributes(types.entityType.device, deviceId, attributeScope, attributes);
    }

    function deleteDeviceAttributes(deviceId, attributeScope, attributes) {
        return attributeService.deleteEntityAttributes(types.entityType.device, deviceId, attributeScope, attributes);
    }

    function sendOneWayRpcCommand(deviceId, requestBody) {
        var deferred = $q.defer();
        var url = '/api/plugins/rpc/oneway/' + deviceId;
        $http.post(url, requestBody).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(rejection) {
            deferred.reject(rejection);
        });
        return deferred.promise;
    }

    function sendTwoWayRpcCommand(deviceId, requestBody) {
        var deferred = $q.defer();
        var url = '/api/plugins/rpc/twoway/' + deviceId;
        $http.post(url, requestBody).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(rejection) {
            deferred.reject(rejection);
        });
        return deferred.promise;
    }

    function findByQuery(query, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/devices';
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

    function getDeviceTypes(config) {
        var deferred = $q.defer();
        var url = '/api/device/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}
