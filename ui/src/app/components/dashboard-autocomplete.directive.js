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
import './dashboard-autocomplete.scss';

import thingsboardApiDashboard from '../api/dashboard.service';
import thingsboardApiUser from '../api/user.service';

/* eslint-disable import/no-unresolved, import/default */

import dashboardAutocompleteTemplate from './dashboard-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.dashboardAutocomplete', [thingsboardApiDashboard, thingsboardApiUser])
    .directive('tbDashboardAutocomplete', DashboardAutocomplete)
    .name;

/*@ngInject*/
function DashboardAutocomplete($compile, $templateCache, $q, dashboardService, userService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(dashboardAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.dashboard = null;
        scope.dashboardSearchText = '';

        scope.fetchDashboards = function(searchText) {
            var pageLink = {limit: 50, textSearch: searchText};

            var deferred = $q.defer();

            var promise;
            if (scope.dashboardsScope === 'customer' || userService.getAuthority() === 'CUSTOMER_USER') {
                if (scope.customerId) {
                    promise = dashboardService.getCustomerDashboards(scope.customerId, pageLink, {ignoreLoading: true});
                } else {
                    promise = $q.when({data: []});
                }
            } else {
                if (userService.getAuthority() === 'SYS_ADMIN') {
                    if (scope.tenantId) {
                        promise = dashboardService.getTenantDashboardsByTenantId(scope.tenantId, pageLink, {ignoreLoading: true});
                    } else {
                        promise = $q.when({data: []});
                    }
                } else {
                    promise = dashboardService.getTenantDashboards(pageLink, {ignoreLoading: true});
                }
            }

            promise.then(function success(result) {
                deferred.resolve(result.data);
            }, function fail() {
                deferred.reject();
            });

            return deferred.promise;
        }

        scope.dashboardSearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.dashboard ? scope.dashboard.id.id : null);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                dashboardService.getDashboardInfo(ngModelCtrl.$viewValue).then(
                    function success(dashboard) {
                        scope.dashboard = dashboard;
                        startWatchers();
                    },
                    function fail() {
                        scope.dashboard = null;
                        scope.updateView();
                        startWatchers();
                    }
                );
            } else {
                scope.dashboard = null;
                startWatchers();
            }
        }

        function startWatchers() {
            scope.$watch('dashboard', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
            scope.$watch('disabled', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
        }

        if (scope.selectFirstDashboard) {
            var pageLink = {limit: 1, textSearch: ''};
            scope.dashboardFetchFunction(pageLink).then(function success(result) {
                var dashboards = result.data;
                if (dashboards.length > 0) {
                    scope.dashboard = dashboards[0];
                    scope.updateView();
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
            dashboardsScope: '@',
            tenantId: '=',
            customerId: '=',
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            selectFirstDashboard: '='
        }
    };
}
