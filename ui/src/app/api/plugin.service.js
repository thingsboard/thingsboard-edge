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
export default angular.module('thingsboard.api.plugin', [])
    .factory('pluginService', PluginService).name;

/*@ngInject*/
function PluginService($http, $q, $rootScope, $filter, componentDescriptorService, types, utils) {

    var allPlugins = undefined;
    var allActionPlugins = undefined;
    var systemPlugins = undefined;
    var tenantPlugins = undefined;

    $rootScope.pluginServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        invalidatePluginsCache();
    });

    var service = {
        getSystemPlugins: getSystemPlugins,
        getTenantPlugins: getTenantPlugins,
        getAllPlugins: getAllPlugins,
        getAllActionPlugins: getAllActionPlugins,
        getPluginByToken: getPluginByToken,
        getPlugin: getPlugin,
        deletePlugin: deletePlugin,
        savePlugin: savePlugin,
        activatePlugin: activatePlugin,
        suspendPlugin: suspendPlugin
    }

    return service;

    function invalidatePluginsCache() {
        allPlugins = undefined;
        allActionPlugins = undefined;
        systemPlugins = undefined;
        tenantPlugins = undefined;
    }

    function loadPluginsCache(config) {
        var deferred = $q.defer();
        if (!allPlugins) {
            var url = '/api/plugins';
            $http.get(url, config).then(function success(response) {
                componentDescriptorService.getComponentDescriptorsByType(types.componentType.plugin).then(
                    function success(pluginComponents) {
                        allPlugins = response.data;
                        allActionPlugins = [];
                        systemPlugins = [];
                        tenantPlugins = [];
                        allPlugins = $filter('orderBy')(allPlugins, ['+name', '-createdTime']);
                        var pluginHasActionsByClazz = {};
                        for (var index in pluginComponents) {
                            pluginHasActionsByClazz[pluginComponents[index].clazz] =
                                (pluginComponents[index].actions != null && pluginComponents[index].actions.length > 0);
                        }
                        for (var i = 0; i < allPlugins.length; i++) {
                            var plugin = allPlugins[i];
                            if (pluginHasActionsByClazz[plugin.clazz] === true) {
                                allActionPlugins.push(plugin);
                            }
                            if (plugin.tenantId.id === types.id.nullUid) {
                                systemPlugins.push(plugin);
                            } else {
                                tenantPlugins.push(plugin);
                            }
                        }
                        deferred.resolve();
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            }, function fail() {
                deferred.reject();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function getSystemPlugins(pageLink, config) {
        var deferred = $q.defer();
        loadPluginsCache(config).then(
            function success() {
                utils.filterSearchTextEntities(systemPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getTenantPlugins(pageLink, config) {
        var deferred = $q.defer();
        loadPluginsCache(config).then(
            function success() {
                utils.filterSearchTextEntities(tenantPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllActionPlugins(pageLink, config) {
        var deferred = $q.defer();
        loadPluginsCache(config).then(
            function success() {
                utils.filterSearchTextEntities(allActionPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllPlugins(pageLink, config) {
        var deferred = $q.defer();
        loadPluginsCache(config).then(
            function success() {
                utils.filterSearchTextEntities(allPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getPluginByToken(pluginToken) {
        var deferred = $q.defer();
        var url = '/api/plugin/token/' + pluginToken;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getPlugin(pluginId, config) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function savePlugin(plugin) {
        var deferred = $q.defer();
        var url = '/api/plugin';
        $http.post(url, plugin).then(function success(response) {
            invalidatePluginsCache();
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deletePlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId;
        $http.delete(url).then(function success() {
            invalidatePluginsCache();
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function activatePlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId + '/activate';
        $http.post(url, null).then(function success(response) {
            invalidatePluginsCache();
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function suspendPlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId + '/suspend';
        $http.post(url, null).then(function success(response) {
            invalidatePluginsCache();
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

}
