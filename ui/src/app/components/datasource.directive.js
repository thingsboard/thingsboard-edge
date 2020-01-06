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
import './datasource.scss';

import thingsboardTypes from '../common/types.constant';
import thingsboardDatasourceFunc from './datasource-func.directive'
import thingsboardDatasourceEntity from './datasource-entity.directive';

/* eslint-disable import/no-unresolved, import/default */

import datasourceTemplate from './datasource.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.datasource', [thingsboardTypes, thingsboardDatasourceFunc, thingsboardDatasourceEntity])
    .directive('tbDatasource', Datasource)
    .name;

/*@ngInject*/
function Datasource($compile, $templateCache, utils, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(datasourceTemplate);
        element.html(template);

        scope.types = types;

        if (scope.functionsOnly) {
            scope.datasourceTypes = [types.datasourceType.function];
        } else{
            scope.datasourceTypes = [types.datasourceType.entity, types.datasourceType.function];
        }

        scope.updateView = function () {
            if (!scope.model.dataKeys) {
                scope.model.dataKeys = [];
            }
            ngModelCtrl.$setViewValue(scope.model);
        }

        scope.$watch('model.type', function (newType, prevType) {
            if (newType && prevType && newType != prevType) {
                if (scope.widgetType == types.widgetType.alarm.value) {
                    scope.model.dataKeys = utils.getDefaultAlarmDataKeys();
                } else {
                    scope.model.dataKeys = [];
                }
            }
        });

        scope.$watch('model', function () {
            scope.updateView();
        }, true);

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.model = ngModelCtrl.$viewValue;
            } else {
                scope.model = {};
            }
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            aliasController: '=',
            maxDataKeys: '=',
            optDataKeys: '=',
            widgetType: '=',
            functionsOnly: '=',
            datakeySettingsSchema: '=',
            generateDataKey: '&',
            fetchEntityKeys: '&',
            onCreateEntityAlias: '&'
        },
        link: linker
    };
}
