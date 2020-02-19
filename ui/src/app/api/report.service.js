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
export default angular.module('thingsboard.api.reportService', [])
    .factory('reportService', ReportService)
    .name;

/*@ngInject*/
function ReportService($rootScope, $http, $q, $document, $window, $translate, tbDialogs) {

    var service = {
        downloadDashboardReport: downloadDashboardReport,
        downloadTestReport: downloadTestReport,
        loadReportParams: loadReportParams
    };

    return service;

    function loadReportParams(locationSearch) {
        if (locationSearch.reportView) {
            $rootScope.reportView = true;
            if (locationSearch.reportTimewindow) {
                $rootScope.reportTimewindow = angular.fromJson(locationSearch.reportTimewindow);
            }
        }
    }

    function downloadDashboardReport($event, dashboardId, reportType, state, timewindow) {
        var url = '/api/report/' + dashboardId +  '/download';
        var reportParams = {
            type: reportType,
            timezone: moment.tz.guess() //eslint-disable-line
        };
        if (state) {
            reportParams.state = state;
        }
        if (timewindow) {
            reportParams.timewindow = timewindow;
        }
        var progressText = $translate.instant('dashboard.download-dashboard-progress', {reportType: reportType});
        var progressFunction = () => _downloadDashboardReport(url, reportParams);
        tbDialogs.progress($event, progressFunction, progressText);
    }

    function downloadTestReport($event, reportConfig, reportsServerEndpointUrl) {
        var url = '/api/report/test';
        var params = {};
        if (reportsServerEndpointUrl) {
            params['reportsServerEndpointUrl'] = reportsServerEndpointUrl;
        }
        var progressText = $translate.instant('dashboard.download-dashboard-progress', {reportType: reportConfig.type});
        var progressFunction = () => _downloadDashboardReport(url, reportConfig, params);
        tbDialogs.progress($event, progressFunction, progressText);
    }

    function _downloadDashboardReport(url, reportParams, params) {
        if (!params) {
            params = {};
        }
        var deferred = $q.defer();
        $http({
            method: 'POST',
            url: url,
            params: params,
            data: reportParams,
            responseType: 'arraybuffer'
        }).success(function (data, status, headers) {
            headers = headers();
            var filename = headers['x-filename'];
            var contentType = headers['content-type'];
            var linkElement = $document[0].createElement('a');
            try {
                var blob = new Blob([data], { type: contentType }); //eslint-disable-line
                var url = $window.URL.createObjectURL(blob);
                linkElement.setAttribute('href', url);
                linkElement.setAttribute("download", filename);
                var clickEvent = new MouseEvent("click", { //eslint-disable-line
                    "view": $window,
                    "bubbles": true,
                    "cancelable": false
                });
                linkElement.dispatchEvent(clickEvent);
                deferred.resolve();
            } catch (ex) {
                deferred.reject(ex);
            }
        }).error(function (data) {
            deferred.reject(data);
        });
        return deferred.promise;
    }
}
