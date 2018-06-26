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

export default angular.module('thingsboard.api.reportService', [])
    .factory('reportService', ReportService)
    .factory('reportStore', function($rootScope, store) {
        var reportStore = store.getNamespacedStore('tbReportStore', 'sessionStorage', null, false);
        var tbReportView = reportStore.get('report_view');
        if (tbReportView) {
            $rootScope.reportView = true;
            var reportTimewindow = reportStore.get('report_timewindow');
            if (reportTimewindow) {
                $rootScope.reportTimewindow = angular.fromJson(reportTimewindow);
            }
            var tzOffset = reportStore.get('report_tz_offset');
            if (angular.isNumber(tzOffset)) {
                Date.setTimezoneOffset(Number(tzOffset));
            }
        }
        return reportStore;
    })
    .name;

/*@ngInject*/
function ReportService($http, $q, $document, $window, $translate, tbDialogs) {

    var service = {
        downloadDashboardReport: downloadDashboardReport
    };

    return service;

    function downloadDashboardReport($event, dashboardId, reportType, state, timewindow, tzOffset) {
        var url = '/api/report/' + dashboardId +  '/download';
        var reportParams = {
            type: reportType,
            tzOffset: tzOffset
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

    function _downloadDashboardReport(url, reportParams) {
        var deferred = $q.defer();
        $http({
            method: 'POST',
            url: url,
            params: {},
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
