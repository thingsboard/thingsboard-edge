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
import './plugin-select.scss';

import thingsboardApiPlugin from '../api/plugin.service';

/* eslint-disable import/no-unresolved, import/default */

import pluginSelectTemplate from './plugin-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.pluginSelect', [thingsboardApiPlugin])
    .directive('tbPluginSelect', PluginSelect)
    .name;

/*@ngInject*/
function PluginSelect($compile, $templateCache, $q, pluginService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(pluginSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.plugin = null;
        scope.pluginSearchText = '';
        scope.searchTextChanged = false;

        scope.pluginFetchFunction = pluginService.getAllPlugins;
        if (angular.isDefined(scope.pluginsScope)) {
            if (scope.pluginsScope === 'action') {
                scope.pluginFetchFunction = pluginService.getAllActionPlugins;
            } else if (scope.pluginsScope === 'system') {
                scope.pluginFetchFunction = pluginService.getSystemPlugins;
            } else if (scope.pluginsScope === 'tenant') {
                scope.pluginFetchFunction = pluginService.getTenantPlugins;
            }
        }

        scope.fetchPlugins = function(searchText) {
            var pageLink = {limit: 10, textSearch: searchText};

            var deferred = $q.defer();

            scope.pluginFetchFunction(pageLink).then(function success(result) {
                deferred.resolve(result.data);
            }, function fail() {
                deferred.reject();
            });

            return deferred.promise;
        }

        scope.pluginSearchTextChanged = function() {
            scope.searchTextChanged = true;
        }

        scope.isSystem = function(item) {
            return item && item.tenantId.id === types.id.nullUid;
        }

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.plugin);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.plugin = ngModelCtrl.$viewValue;
            }
        }

        scope.$watch('plugin', function () {
            scope.updateView();
        })

        if (scope.selectFirstPlugin) {
            var pageLink = {limit: 1, textSearch: ''};
            scope.pluginFetchFunction(pageLink).then(function success(result) {
                var plugins = result.data;
                if (plugins.length > 0) {
                    scope.plugin = plugins[0];
                }
            }, function fail() {
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            pluginsScope: '@',
            theForm: '=?',
            tbRequired: '=?',
            selectFirstPlugin: '='
        }
    };
}
