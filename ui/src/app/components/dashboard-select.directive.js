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
import './dashboard-select.scss';

import thingsboardApiDashboard from '../api/dashboard.service';
import thingsboardApiUser from '../api/user.service';

/* eslint-disable import/no-unresolved, import/default */

import dashboardSelectTemplate from './dashboard-select.tpl.html';
import dashboardSelectPanelTemplate from './dashboard-select-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import DashboardSelectPanelController from './dashboard-select-panel.controller';


export default angular.module('thingsboard.directives.dashboardSelect', [thingsboardApiDashboard, thingsboardApiUser])
    .directive('tbDashboardSelect', DashboardSelect)
    .controller('DashboardSelectPanelController', DashboardSelectPanelController)
    .name;

/*@ngInject*/
function DashboardSelect($compile, $templateCache, $q, $mdMedia, $mdPanel, $document, types, dashboardService, userService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(dashboardSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.dashboardId = null;

        var pageLink = {limit: 100};

        var promise;
        if (scope.dashboardsScope === 'customer' || userService.getAuthority() === 'CUSTOMER_USER') {
            if (scope.customerId && scope.customerId != types.id.nullUid) {
                promise = dashboardService.getCustomerDashboards(scope.customerId, pageLink, {ignoreLoading: true});
            } else {
                promise = $q.when({data: []});
            }
        } else {
            promise = dashboardService.getTenantDashboards(pageLink, {ignoreLoading: true});
        }

        promise.then(function success(result) {
            scope.dashboards = result.data;
        }, function fail() {
            scope.dashboards = [];
        });

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.dashboardId);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.dashboardId = ngModelCtrl.$viewValue;
            } else {
                scope.dashboardId = null;
            }
        }

        scope.$watch('dashboardId', function () {
            scope.updateView();
        });

        scope.openDashboardSelectPanel = function (event) {
            if (scope.disabled) {
                return;
            }
            var position;
            var panelHeight = $mdMedia('min-height: 350px') ? 250 : 150;
            var panelWidth = 300;
            var offset = element[0].getBoundingClientRect();
            var bottomY = offset.bottom - $(window).scrollTop(); //eslint-disable-line
            var leftX = offset.left - $(window).scrollLeft(); //eslint-disable-line
            var yPosition;
            var xPosition;
            if (bottomY + panelHeight > $( window ).height()) { //eslint-disable-line
                yPosition = $mdPanel.yPosition.ABOVE;
            } else {
                yPosition = $mdPanel.yPosition.BELOW;
            }
            if (leftX + panelWidth > $( window ).width()) { //eslint-disable-line
                xPosition = $mdPanel.xPosition.CENTER;
            } else {
                xPosition = $mdPanel.xPosition.ALIGN_START;
            }
            position = $mdPanel.newPanelPosition()
                .relativeTo(element)
                .addPanelPosition(xPosition, yPosition);
            var config = {
                attachTo: angular.element($document[0].body),
                controller: 'DashboardSelectPanelController',
                controllerAs: 'vm',
                templateUrl: dashboardSelectPanelTemplate,
                panelClass: 'tb-dashboard-select-panel',
                position: position,
                fullscreen: false,
                locals: {
                    dashboards: scope.dashboards,
                    dashboardId: scope.dashboardId,
                    onDashboardSelected: (dashboardId) => {
                        if (scope.panelRef) {
                            scope.panelRef.close();
                        }
                        scope.dashboardId = dashboardId;
                    }
                },
                openFrom: event,
                clickOutsideToClose: true,
                escapeToClose: true,
                focusOnOpen: false
            };
            $mdPanel.open(config).then(function(result) {
                scope.panelRef = result;
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
            customerId: '=',
            tbRequired: '=?',
            disabled:'=ngDisabled'
        }
    };
}
