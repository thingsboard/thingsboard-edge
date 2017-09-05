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
import './datakey-config.scss';

import thingsboardJsonForm from "./json-form.directive";
import thingsboardTypes from '../common/types.constant';
import thingsboardJsFunc from './js-func.directive';

/* eslint-disable import/no-unresolved, import/default */

import datakeyConfigTemplate from './datakey-config.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.datakeyConfig', [thingsboardTypes, thingsboardJsFunc, thingsboardJsonForm])
    .directive('tbDatakeyConfig', DatakeyConfig)
    .name;

/*@ngInject*/
function DatakeyConfig($compile, $templateCache, $q, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(datakeyConfigTemplate);

        if (scope.datakeySettingsSchema.schema) {
            scope.dataKeySchema = scope.datakeySettingsSchema.schema;
            scope.dataKeyForm = scope.datakeySettingsSchema.form || ['*'];
            template = '<md-tabs md-border-bottom class="tb-datakey-config">\n' +
                '<md-tab label="{{\'datakey.settings\' | translate}}">\n' +
                template +
                '</md-tab>\n' +
                '<md-tab label="{{\'datakey.advanced\' | translate}}">\n' +
                '<md-content class="md-padding" layout="column">\n' +
                '<div style="overflow: auto;">\n' +
                '<ng-form name="ngform" ' +
                'layout="column" ' +
                'layout-padding>' +
                '<tb-json-form schema="dataKeySchema"' +
                'form="dataKeyForm"' +
                'model="model.settings"' +
                'form-control="ngform">' +
                '</tb-json-form>' +
                '</ng-form>\n' +
                '</div>\n' +
                '</md-content>\n' +
                '</md-tab>\n' +
                '</md-tabs>';
        }

        element.html(template);

        scope.types = types;

        scope.alarmFields = [];
        for (var alarmField in types.alarmFields) {
            scope.alarmFields.push(alarmField);
        }

        scope.selectedKey = null;
        scope.keySearchText = null;
        scope.usePostProcessing = false;

        scope.functions = {};

        ngModelCtrl.$render = function () {
            scope.model = {};
            if (ngModelCtrl.$viewValue) {
                scope.model.type = ngModelCtrl.$viewValue.type;
                scope.model.name = ngModelCtrl.$viewValue.name;
                scope.model.label = ngModelCtrl.$viewValue.label;
                scope.model.color = ngModelCtrl.$viewValue.color;
                scope.model.units = ngModelCtrl.$viewValue.units;
                scope.model.decimals = ngModelCtrl.$viewValue.decimals;
                scope.model.funcBody = ngModelCtrl.$viewValue.funcBody;
                scope.model.postFuncBody = ngModelCtrl.$viewValue.postFuncBody;
                scope.model.usePostProcessing = scope.model.postFuncBody ? true : false;
                scope.model.settings = ngModelCtrl.$viewValue.settings;
            }
        };

        scope.$watch('model', function (newVal, oldVal) {
            if (newVal.usePostProcessing != oldVal.usePostProcessing) {
                if (scope.model.usePostProcessing && !scope.model.postFuncBody) {
                    scope.model.postFuncBody = "return value;";
                } else if (!scope.model.usePostProcessing && scope.model.postFuncBody) {
                    delete scope.model.postFuncBody;
                }
            }
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                value.type = scope.model.type;
                value.name = scope.model.name;
                value.label = scope.model.label;
                value.color = scope.model.color;
                value.units = scope.model.units;
                value.decimals = scope.model.decimals;
                value.funcBody = scope.model.funcBody;
                if (!scope.model.postFuncBody) {
                    delete value.postFuncBody;
                } else {
                    value.postFuncBody = scope.model.postFuncBody;
                }
                ngModelCtrl.$setViewValue(value);
            }
        }, true);

        scope.keysSearch = function (searchText) {
            if (scope.model.type === types.dataKeyType.alarm) {
                var dataKeys = searchText ? scope.alarmFields.filter(
                    scope.createFilterForDataKey(searchText)) : scope.alarmFields;
                dataKeys.push(searchText);
                return dataKeys;
            } else {
                if (scope.entityAlias) {
                    var deferred = $q.defer();
                    scope.fetchEntityKeys({
                        entityAliasId: scope.entityAlias.id,
                        query: searchText,
                        type: scope.model.type
                    })
                        .then(function (keys) {
                            keys.push(searchText);
                            deferred.resolve(keys);
                        }, function (e) {
                            deferred.reject(e);
                        });
                    return deferred.promise;
                } else {
                    return $q.when([]);
                }
            }
        };

        scope.createFilterForDataKey = function (query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(dataKey) {
                return (angular.lowercase(dataKey).indexOf(lowercaseQuery) === 0);
            };
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: 'E',
        require: '^ngModel',
        scope: {
            entityAlias: '=',
            fetchEntityKeys: '&',
            datakeySettingsSchema: '='
        },
        link: linker
    };
}