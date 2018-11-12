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
import './material-icon-select.scss';

import MaterialIconsDialogController from './material-icons-dialog.controller';

/* eslint-disable import/no-unresolved, import/default */

import materialIconSelectTemplate from './material-icon-select.tpl.html';
import materialIconsDialogTemplate from './material-icons-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.materialIconSelect', [])
    .controller('MaterialIconsDialogController', MaterialIconsDialogController)
    .directive('tbMaterialIconSelect', MaterialIconSelect)
    .name;

/*@ngInject*/
function MaterialIconSelect($compile, $templateCache, $document, $mdDialog) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(materialIconSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.icon = null;

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.icon);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.icon = ngModelCtrl.$viewValue;
            }
            if (!scope.icon || !scope.icon.length) {
                scope.icon = 'more_horiz';
            }
        }

        scope.$watch('icon', function () {
            scope.updateView();
        });

        scope.openIconDialog = function($event) {
            if ($event) {
                $event.stopPropagation();
            }
            $mdDialog.show({
                controller: 'MaterialIconsDialogController',
                controllerAs: 'vm',
                templateUrl: materialIconsDialogTemplate,
                parent: angular.element($document[0].body),
                locals: {icon: scope.icon},
                multiple: true,
                fullscreen: true,
                targetEvent: $event
            }).then(function (icon) {
                scope.icon = icon;
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
        }
    };
}
