/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
export default angular.module('thingsboard.api.asset', [])
    .factory('assetService', AssetService)
    .name;

/*@ngInject*/
function AssetService($http, $q, customerService, userService) {

    var service = {
        getAsset: getAsset,
        getAssets: getAssets,
        saveAsset: saveAsset,
        deleteAsset: deleteAsset,
        assignAssetToCustomer: assignAssetToCustomer,
        unassignAssetFromCustomer: unassignAssetFromCustomer,
        makeAssetPublic: makeAssetPublic,
        getTenantAssets: getTenantAssets,
        getCustomerAssets: getCustomerAssets,
        findByQuery: findByQuery,
        fetchAssetsByNameFilter: fetchAssetsByNameFilter,
        getAssetTypes: getAssetTypes
    }

    return service;

    function getAsset(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId;
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

    function getAssets(assetIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<assetIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += assetIds[i];
        }
        var url = '/api/assets?assetIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var assets = response.data;
            assets.sort(function (asset1, asset2) {
                var id1 =  asset1.id.id;
                var id2 =  asset2.id.id;
                var index1 = assetIds.indexOf(id1);
                var index2 = assetIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(assets);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveAsset(asset, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/asset';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, asset, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteAsset(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.delete(url, config).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignAssetToCustomer(customerId, assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/asset/' + assetId;
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

    function unassignAssetFromCustomer(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.delete(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeAssetPublic(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/public/asset/' + assetId;
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

    function getTenantAssets(pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/tenant/assets?limit=' + pageLink.limit;
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

    function getCustomerAssets(customerId, pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/assets?limit=' + pageLink.limit;
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
        var url = '/api/assets';
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

    function fetchAssetsByNameFilter(assetNameFilter, limit, applyCustomersInfo, config) {
        var deferred = $q.defer();
        var user = userService.getCurrentUser();
        var promise;
        var pageLink = {limit: limit, textSearch: assetNameFilter};
        if (user.authority === 'CUSTOMER_USER') {
            var customerId = user.customerId;
            promise = getCustomerAssets(customerId, pageLink, applyCustomersInfo, config);
        } else {
            promise = getTenantAssets(pageLink, applyCustomersInfo, config);
        }
        promise.then(
            function success(result) {
                if (result.data && result.data.length > 0) {
                    deferred.resolve(result.data);
                } else {
                    deferred.resolve(null);
                }
            },
            function fail() {
                deferred.resolve(null);
            }
        );
        return deferred.promise;
    }

    function getAssetTypes(config) {
        var deferred = $q.defer();
        var url = '/api/asset/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}
