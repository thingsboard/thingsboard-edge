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
import './details-sidenav.scss';

/* eslint-disable import/no-unresolved, import/default */

import detailsSidenavTemplate from './details-sidenav.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.detailsSidenav', [])
    .directive('tbDetailsSidenav', DetailsSidenav)
    .name;

/*@ngInject*/
function DetailsSidenav($timeout) {

    var linker = function (scope, element, attrs) {

        if (angular.isUndefined(attrs.isReadOnly)) {
            attrs.isReadOnly = false;
        }

        if (angular.isUndefined(scope.headerHeightPx)) {
            scope.headerHeightPx = 100;
        }

        if (angular.isDefined(attrs.isAlwaysEdit) && attrs.isAlwaysEdit) {
            scope.isEdit = true;
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
                scope.onApplyDetails();
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