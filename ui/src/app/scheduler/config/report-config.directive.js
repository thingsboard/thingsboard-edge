/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import reportConfigTemplate from './report-config.tpl.html';
import selectDashboardStateDialogTemplate from './select-dashboard-state-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ReportConfigDirective($compile, $templateCache, $mdDialog, $document,
                                              types, utils, userService, reportService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(reportConfigTemplate);
        element.html(template);

        scope.types = types;

        scope.isTenantAdmin = userService.getAuthority() === 'TENANT_ADMIN';

        scope.defaultTimezone = moment.tz.guess(); //eslint-disable-line

        scope.$watch('reportConfig', function (newReportConfig, oldReportConfig) {
            if (!angular.equals(newReportConfig, oldReportConfig)) {
                ngModelCtrl.$setViewValue(scope.reportConfig);
            }
        });

        scope.useCurrentUserCredentialsChange = function() {
            if (scope.reportConfig.useCurrentUserCredentials) {
                scope.reportConfig.userId = userService.getCurrentUser().userId;
            } else {
                scope.reportConfig.userId = null;
            }
        };

        scope.$watch('reportConfig.dashboardId', function (newDashboardId, oldDashboardId) {
            if (!angular.equals(newDashboardId, oldDashboardId)) {
                scope.reportConfig.state = '';
            }
        });

        scope.selectDashboardState = function($event) {

            var onShowingCallback = {
                onShowed: () => {
                }
            };

            $mdDialog.show({
                controller: 'SelectDashboardStateController',
                controllerAs: 'vm',
                templateUrl: selectDashboardStateDialogTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    dashboardId: scope.reportConfig.dashboardId,
                    state: scope.reportConfig.state,
                    onShowingCallback: onShowingCallback
                },
                fullscreen: true,
                multiple: true,
                targetEvent: $event,
                onComplete: () => {
                    onShowingCallback.onShowed();
                }
            }).then(
                (state) => {
                    scope.reportConfig.state = state;
                    scope.reportConfigForm.$setDirty();
                },
                () => {}
            );
        };

        scope.generateTestReport = function($event) {
            reportService.downloadTestReport($event, scope.reportConfig, scope.reportsServerEndpointUrl);
        };

        ngModelCtrl.$render = function () {
            scope.reportConfig = ngModelCtrl.$viewValue;
            if (!scope.reportConfig) {
                scope.reportConfig = createDefaultReportConfig();
                ngModelCtrl.$setViewValue(scope.reportConfig);
            }
        };

        function createDefaultReportConfig() {
            var reportConfig = {};
            reportConfig.baseUrl = utils.baseUrl();
            reportConfig.useDashboardTimewindow = true;
            reportConfig.timewindow = {
                history: {
                    interval: 1000,
                    timewindowMs: 24 * 60* 60 * 1000
                }
            };
            reportConfig.namePattern = 'report-%d{yyyy-MM-dd_HH:mm:ss}';
            reportConfig.type = types.reportType.pdf.value;
            reportConfig.timezone = scope.defaultTimezone;
            reportConfig.useCurrentUserCredentials = true;
            reportConfig.userId = userService.getCurrentUser().userId;
            return reportConfig;
        }

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            reportsServerEndpointUrl: '=?',
            readonly:'=ngReadonly'
        },
        link: linker
    };
}
