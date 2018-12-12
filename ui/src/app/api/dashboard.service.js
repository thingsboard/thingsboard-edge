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
export default angular.module('thingsboard.api.dashboard', [])
    .factory('dashboardService', DashboardService).name;

/*@ngInject*/
function DashboardService($rootScope, $http, $q, $location, $filter) {

    var stDiffPromise;

    $rootScope.dadshboardServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        stDiffPromise = undefined;
    });


    var service = {
        assignDashboardToCustomer: assignDashboardToCustomer,
        getCustomerDashboards: getCustomerDashboards,
        getServerTimeDiff: getServerTimeDiff,
        getDashboard: getDashboard,
        getDashboardInfo: getDashboardInfo,
        getTenantDashboardsByTenantId: getTenantDashboardsByTenantId,
        getTenantDashboards: getTenantDashboards,
        deleteDashboard: deleteDashboard,
        saveDashboard: saveDashboard,
        unassignDashboardFromCustomer: unassignDashboardFromCustomer,
        updateDashboardCustomers: updateDashboardCustomers,
        addDashboardCustomers: addDashboardCustomers,
        removeDashboardCustomers: removeDashboardCustomers,
        makeDashboardPublic: makeDashboardPublic,
        makeDashboardPrivate: makeDashboardPrivate,
        getPublicDashboardLink: getPublicDashboardLink
    }

    return service;

    function getTenantDashboardsByTenantId(tenantId, pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/tenant/' + tenantId + '/dashboards?limit=' + pageLink.limit;
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
            deferred.resolve(prepareDashboards(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getTenantDashboards(pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/tenant/dashboards?limit=' + pageLink.limit;
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
            deferred.resolve(prepareDashboards(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerDashboards(customerId, pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboards?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, config).then(function success(response) {
            response.data = prepareDashboards(response.data);
            if (pageLink.textSearch) {
                response.data.data = $filter('filter')(response.data.data, {title: pageLink.textSearch});
            }
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getServerTimeDiff() {
        if (stDiffPromise) {
            return stDiffPromise;
        } else {
            var deferred = $q.defer();
            stDiffPromise = deferred.promise;
            var url = '/api/dashboard/serverTime';
            var ct1 = Date.now();
            $http.get(url, {ignoreLoading: true}).then(function success(response) {
                var ct2 = Date.now();
                var st = response.data;
                var stDiff = Math.ceil(st - (ct1 + ct2) / 2);
                deferred.resolve(stDiff);
            }, function fail() {
                deferred.reject();
            });
        }
        return stDiffPromise;
    }

    function getDashboard(dashboardId, config) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDashboardInfo(dashboardId, config) {
        var deferred = $q.defer();
        var url = '/api/dashboard/info/' + dashboardId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveDashboard(dashboard) {
        var deferred = $q.defer();
        var url = '/api/dashboard';
        $http.post(url, cleanDashboard(dashboard)).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteDashboard(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignDashboardToCustomer(customerId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboard/' + dashboardId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignDashboardFromCustomer(customerId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboard/' + dashboardId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function updateDashboardCustomers(dashboardId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId + '/customers';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function addDashboardCustomers(dashboardId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId + '/customers/add';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeDashboardCustomers(dashboardId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId + '/customers/remove';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDashboardPublic(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/dashboard/' + dashboardId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDashboardPrivate(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/dashboard/' + dashboardId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getPublicDashboardLink(dashboard) {
        var url = $location.protocol() + '://' + $location.host();
        var port = $location.port();
        if (port != 80 && port != 443) {
            url += ":" + port;
        }
        url += "/dashboard/" + dashboard.id.id + "?publicId=" + dashboard.publicCustomerId;
        return url;
    }

    function prepareDashboards(dashboardsData) {
        if (dashboardsData.data) {
            for (var i = 0; i < dashboardsData.data.length; i++) {
                dashboardsData.data[i] = prepareDashboard(dashboardsData.data[i]);
            }
        }
        return dashboardsData;
    }

    function prepareDashboard(dashboard) {
        dashboard.publicCustomerId = null;
        dashboard.assignedCustomersText = "";
        dashboard.assignedCustomersIds = [];
        if (dashboard.assignedCustomers && dashboard.assignedCustomers.length) {
            var assignedCustomersTitles = [];
            for (var i = 0; i < dashboard.assignedCustomers.length; i++) {
                var assignedCustomer = dashboard.assignedCustomers[i];
                dashboard.assignedCustomersIds.push(assignedCustomer.customerId.id);
                if (assignedCustomer.public) {
                    dashboard.publicCustomerId = assignedCustomer.customerId.id;
                } else {
                    assignedCustomersTitles.push(assignedCustomer.title);
                }
            }
            dashboard.assignedCustomersText = assignedCustomersTitles.join(', ');
        }
        return dashboard;
    }

    function cleanDashboard(dashboard) {
        delete dashboard.publicCustomerId;
        delete dashboard.assignedCustomersText;
        delete dashboard.assignedCustomersIds;
        return dashboard;
    }

}
