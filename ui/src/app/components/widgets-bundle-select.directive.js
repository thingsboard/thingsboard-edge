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
import './widgets-bundle-select.scss';

import thingsboardApiWidget from '../api/widget.service';

/* eslint-disable import/no-unresolved, import/default */

import widgetsBundleSelectTemplate from './widgets-bundle-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.widgetsBundleSelect', [thingsboardApiWidget])
    .directive('tbWidgetsBundleSelect', WidgetsBundleSelect)
    .name;

/*@ngInject*/
function WidgetsBundleSelect($compile, $templateCache, widgetService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(widgetsBundleSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.widgetsBundle = null;
        scope.widgetsBundles = [];

        var widgetsBundleFetchFunction = widgetService.getAllWidgetsBundles;
        if (angular.isDefined(scope.bundlesScope)) {
            if (scope.bundlesScope === 'system') {
                widgetsBundleFetchFunction = widgetService.getSystemWidgetsBundles;
            } else if (scope.bundlesScope === 'tenant') {
                widgetsBundleFetchFunction = widgetService.getTenantWidgetsBundles;
            }
        }

        widgetsBundleFetchFunction({ignoreLoading: true}).then(
            function success(widgetsBundles) {
                scope.widgetsBundles = widgetsBundles;
                if (scope.selectFirstBundle) {
                    if (widgetsBundles.length > 0) {
                        scope.widgetsBundle = widgetsBundles[0];
                    }
                } else if (angular.isDefined(scope.selectBundleAlias)) {
                    selectWidgetsBundleByAlias(scope.selectBundleAlias);
                }
            },
            function fail() {
            }
        );

        function selectWidgetsBundleByAlias(alias) {
            if (scope.widgetsBundles && alias) {
                for (var w in scope.widgetsBundles) {
                    var widgetsBundle = scope.widgetsBundles[w];
                    if (widgetsBundle.alias === alias) {
                        scope.widgetsBundle = widgetsBundle;
                        break;
                    }
                }
            }
        }

        scope.isSystem = function(item) {
            return item && item.tenantId.id === types.id.nullUid;
        }

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.widgetsBundle);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.widgetsBundle = ngModelCtrl.$viewValue;
            }
        }

        scope.$watch('widgetsBundle', function () {
            scope.updateView();
        });

        scope.$watch('selectBundleAlias', function (newVal, prevVal) {
            if (newVal !== prevVal) {
                selectWidgetsBundleByAlias(scope.selectBundleAlias);
            }
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            bundlesScope: '@',
            theForm: '=?',
            tbRequired: '=?',
            selectFirstBundle: '=',
            selectBundleAlias: '=?'
        }
    };
}