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
import './details-sidenav.scss';

/* eslint-disable import/no-unresolved, import/default */

import detailsSidenavTemplate from './details-sidenav.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.detailsSidenav', [])
    .directive('tbDetailsSidenav', DetailsSidenav)
    .name;

/*@ngInject*/
function DetailsSidenav($timeout, $mdUtil, $q, $animate, utils) {

    var linker = function (scope, element, attrs) {

        scope.utils = utils;

        if (angular.isUndefined(attrs.isReadOnly)) {
            attrs.isReadOnly = false;
        }

        if (angular.isUndefined(scope.headerHeightPx)) {
            scope.headerHeightPx = 100;
        }

        if (angular.isDefined(attrs.isAlwaysEdit) && attrs.isAlwaysEdit) {
            scope.isEdit = true;
        }

        var backdrop;
        var previousContainerStyles;

        if (Object.prototype.hasOwnProperty.call(attrs,'tbEnableBackdrop')) {
            backdrop = $mdUtil.createBackdrop(scope, "md-sidenav-backdrop md-opaque ng-enter");
            element.on('$destroy', function() {
                backdrop && backdrop.remove();
            });
            scope.$on('$destroy', function(){
                backdrop && backdrop.remove();
            });
            scope.$watch('isOpen', updateIsOpen);
        }

        function updateIsOpen(isOpen) {
            backdrop[isOpen ? 'on' : 'off']('click', (ev)=>{
                ev.preventDefault();
                scope.isOpen = false;
                scope.$apply();
            });
            var parent = element.parent();
            var restorePositioning = updateContainerPositions(parent, isOpen);

            return $q.all([
                isOpen && backdrop ? $animate.enter(backdrop, parent) : backdrop ?
                    $animate.leave(backdrop) : $q.when(true)
            ]).then(function() {
                restorePositioning && restorePositioning();
            });
        }

        function updateContainerPositions(parent, willOpen) {
            var drawerEl = element[0];
            var scrollTop = parent[0].scrollTop;
            if (willOpen && scrollTop) {
                previousContainerStyles = {
                    top: drawerEl.style.top,
                    bottom: drawerEl.style.bottom,
                    height: drawerEl.style.height
                };
                var positionStyle = {
                    top: scrollTop + 'px',
                    bottom: 'auto',
                    height: parent[0].clientHeight + 'px'
                };
                backdrop.css(positionStyle);
            }
            if (!willOpen && previousContainerStyles) {
                return function() {
                    backdrop[0].style.top = null;
                    backdrop[0].style.bottom = null;
                    backdrop[0].style.height = null;
                    previousContainerStyles = null;
                };
            }
        }

        scope.toggleDetailsEditMode = function () {
            if (!scope.isAlwaysEdit) {
                if (!scope.isEdit) {
                    scope.isEdit = true;
                } else {
                    scope.isEdit = false;
                }
            }
            $timeout(function () {
                scope.onToggleDetailsEditMode();
            });
        };

        scope.detailsApply = function () {
            $timeout(function () {
                scope.$broadcast('form-submit');
                if (scope.theForm.$valid) {
                    scope.onApplyDetails();
                }
            });
        }

        scope.closeDetails = function () {
            scope.isOpen = false;
            $timeout(function () {
                scope.onCloseDetails();
            });
        };
    }

    return {
        restrict: "E",
        transclude: {
            headerPane: '?headerPane',
            detailsButtons: '?detailsButtons'
        },
        scope: {
            headerTitle: '@',
            headerSubtitle: '@',
            headerHeightPx: '@',
            isReadOnly: '=',
            isOpen: '=',
            isEdit: '=?',
            isAlwaysEdit: '=?',
            theForm: '=',
            onCloseDetails: '&',
            onToggleDetailsEditMode: '&',
            onApplyDetails: '&'
        },
        link: linker,
        templateUrl: detailsSidenavTemplate
    };
}