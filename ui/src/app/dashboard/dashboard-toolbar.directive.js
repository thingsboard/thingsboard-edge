/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
import './dashboard-toolbar.scss';

import 'javascript-detect-element-resize/detect-element-resize';

/* eslint-disable import/no-unresolved, import/default */

import dashboardToolbarTemplate from './dashboard-toolbar.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DashboardToolbar() {
    return {
        restrict: "E",
        scope: true,
        transclude: true,
        bindToController: {
            toolbarOpened: '=',
            forceFullscreen: '=',
            onTriggerClick: '&'
        },
        controller: DashboardToolbarController,
        controllerAs: 'vm',
        templateUrl: dashboardToolbarTemplate
    };
}

/* eslint-disable angular/angularelement */


/*@ngInject*/
function DashboardToolbarController($rootScope, $scope, $element, $timeout, $mdColors, mdFabToolbarAnimation) {

    let vm = this;

    vm.mdFabToolbarElement = angular.element($element[0].querySelector('md-fab-toolbar'));

    function initElements() {
        $timeout(function() {
            vm.mdFabBackgroundElement = angular.element(vm.mdFabToolbarElement[0].querySelector('.md-fab-toolbar-background'));
            vm.mdFabTriggerElement = angular.element(vm.mdFabToolbarElement[0].querySelector('md-fab-trigger button'));
            if (!vm.mdFabBackgroundElement || !vm.mdFabBackgroundElement[0]) {
                initElements();
            } else {
                triggerFabResize();
            }
        });
    }

    addResizeListener(vm.mdFabToolbarElement[0], triggerFabResize); // eslint-disable-line no-undef

    $scope.$on("$destroy", function () {
        removeResizeListener(vm.mdFabToolbarElement[0], triggerFabResize); // eslint-disable-line no-undef
    });

    initElements();

    function triggerFabResize() {
        if (!vm.mdFabBackgroundElement || !vm.mdFabBackgroundElement[0]) {
            return;
        }
        var ctrl = vm.mdFabToolbarElement.controller('mdFabToolbar');
        if (ctrl.isOpen) {
            var color = $mdColors.getThemeColor(`primary-500`);
            vm.mdFabBackgroundElement.css({backgroundColor: color});
            if (!vm.mdFabBackgroundElement[0].offsetWidth) {
                mdFabToolbarAnimation.addClass(vm.mdFabToolbarElement, 'md-is-open', function () {
                });
            } else {
                var width = vm.mdFabToolbarElement[0].offsetWidth;
                var scale = 2 * (width / vm.mdFabTriggerElement[0].offsetWidth);
                vm.mdFabBackgroundElement[0].style.backgroundColor = color;
                vm.mdFabBackgroundElement[0].style.borderRadius = width + 'px';

                var transform = vm.mdFabBackgroundElement[0].style.transform;
                var targetTransform = 'scale(' + scale + ')';
                if (!transform || !angular.equals(transform, targetTransform)) {
                    vm.mdFabBackgroundElement[0].style.transform = targetTransform;
                }
            }
        }
    }
}

