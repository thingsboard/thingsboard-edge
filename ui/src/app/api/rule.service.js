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
export default angular.module('thingsboard.api.rule', [])
    .factory('ruleService', RuleService).name;

/*@ngInject*/
function RuleService($http, $q, $rootScope, $filter, types, utils) {

    var allRules = undefined;
    var systemRules = undefined;
    var tenantRules = undefined;

    $rootScope.ruleServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        invalidateRulesCache();
    });

    var service = {
        getSystemRules: getSystemRules,
        getTenantRules: getTenantRules,
        getAllRules: getAllRules,
        getRulesByPluginToken: getRulesByPluginToken,
        getRule: getRule,
        deleteRule: deleteRule,
        saveRule: saveRule,
        activateRule: activateRule,
        suspendRule: suspendRule
    }

    return service;

    function invalidateRulesCache() {
        allRules = undefined;
        systemRules = undefined;
        tenantRules = undefined;
    }

    function loadRulesCache() {
        var deferred = $q.defer();
        if (!allRules) {
            var url = '/api/rules';
            $http.get(url, null).then(function success(response) {
                allRules = response.data;
                systemRules = [];
                tenantRules = [];
                allRules = $filter('orderBy')(allRules, ['+name', '-createdTime']);
                for (var i = 0; i < allRules.length; i++) {
                    var rule = allRules[i];
                    if (rule.tenantId.id === types.id.nullUid) {
                        systemRules.push(rule);
                    } else {
                        tenantRules.push(rule);
                    }
                }
                deferred.resolve();
            }, function fail() {
                deferred.reject();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function getSystemRules(pageLink) {
        var deferred = $q.defer();
        loadRulesCache().then(
            function success() {
                utils.filterSearchTextEntities(systemRules, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getTenantRules(pageLink) {
        var deferred = $q.defer();
        loadRulesCache().then(
            function success() {
                utils.filterSearchTextEntities(tenantRules, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllRules(pageLink) {
        var deferred = $q.defer();
        loadRulesCache().then(
            function success() {
                utils.filterSearchTextEntities(allRules, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getRulesByPluginToken(pluginToken) {
        var deferred = $q.defer();
        var url = '/api/rule/token/' + pluginToken;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveRule(rule) {
        var deferred = $q.defer();
        var url = '/api/rule';
        $http.post(url, rule).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function activateRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId + '/activate';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function suspendRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId + '/suspend';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

}
