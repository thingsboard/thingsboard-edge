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
import './datasource-entity.scss';

import 'md-color-picker';
import tinycolor from 'tinycolor2';
import $ from 'jquery';
import thingsboardTypes from '../common/types.constant';
import thingsboardDatakeyConfigDialog from './datakey-config-dialog.controller';
import thingsboardTruncate from './truncate.filter';

/* eslint-disable import/no-unresolved, import/default */

import datasourceEntityTemplate from './datasource-entity.tpl.html';
import datakeyConfigDialogTemplate from './datakey-config-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.datasourceEntity', [thingsboardTruncate, thingsboardTypes, thingsboardDatakeyConfigDialog])
    .directive('tbDatasourceEntity', DatasourceEntity)
    .name;

/*@ngInject*/
function DatasourceEntity($compile, $templateCache, $q, $mdDialog, $window, $document, $mdColorPicker, $mdConstant, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(datasourceEntityTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;

        scope.alarmFields = [];
        for (var alarmField in types.alarmFields) {
            scope.alarmFields.push(alarmField);
        }

        scope.selectedDataKey = null;
        scope.dataKeySearchText = null;

        scope.selectedAlarmDataKey = null;
        scope.alarmDataKeySearchText = null;

        scope.updateValidity = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var dataValid = angular.isDefined(value) && value != null;
                ngModelCtrl.$setValidity('entityData', dataValid);
                if (dataValid) {
                    ngModelCtrl.$setValidity('entityAlias',
                        angular.isDefined(value.entityAliasId) &&
                        value.entityAliasId != null);
                    if (scope.optDataKeys) {
                        ngModelCtrl.$setValidity('entityKeys', true);
                    } else {
                        ngModelCtrl.$setValidity('entityKeys',
                            angular.isDefined(value.dataKeys) &&
                            value.dataKeys != null &&
                            value.dataKeys.length > 0);
                    }
                }
            }
        };

        scope.$watch('entityAlias', function () {
            if (ngModelCtrl.$viewValue) {
                if (scope.entityAlias) {
                    ngModelCtrl.$viewValue.entityAliasId = scope.entityAlias.id;
                } else {
                    ngModelCtrl.$viewValue.entityAliasId = null;
                }
                scope.updateValidity();
                scope.selectedEntityAliasChange();
            }
        });

        scope.$watch('dataKeys', function () {
            updateDataKeys();
        }, true);

        scope.$watch('alarmDataKeys', function () {
            updateDataKeys();
        }, true);

        function updateDataKeys() {
            if (ngModelCtrl.$viewValue) {
                var dataKeys = [];
                dataKeys = dataKeys.concat(scope.dataKeys);
                dataKeys = dataKeys.concat(scope.alarmDataKeys);
                if (!angular.equals(ngModelCtrl.$viewValue.dataKeys, dataKeys))
                {
                   ngModelCtrl.$setDirty();
                   ngModelCtrl.$viewValue.dataKeys = dataKeys;
                }
                scope.updateValidity();
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var entityAliasId = ngModelCtrl.$viewValue.entityAliasId;
                var entityAliases = scope.aliasController.getEntityAliases();
                if (entityAliases[entityAliasId]) {
                    scope.entityAlias = entityAliases[entityAliasId];
                } else {
                    scope.entityAlias = null;
                }
                var dataKeys = [];
                var alarmDataKeys = [];
                for (var d in ngModelCtrl.$viewValue.dataKeys) {
                    var dataKey = ngModelCtrl.$viewValue.dataKeys[d];
                    if ((dataKey.type === types.dataKeyType.timeseries) || (dataKey.type === types.dataKeyType.attribute) || (dataKey.type === types.dataKeyType.entityField)) {
                        dataKeys.push(dataKey);
                    } else if (dataKey.type === types.dataKeyType.alarm) {
                        alarmDataKeys.push(dataKey);
                    }
                }
                scope.dataKeys = dataKeys;
                scope.alarmDataKeys = alarmDataKeys;
            }
        };

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.selectedEntityAliasChange = function () {
            if (!scope.dataKeySearchText || scope.dataKeySearchText === '') {
                scope.dataKeySearchText = scope.dataKeySearchText === '' ? null : '';
            }
            if (!scope.alarmDataKeySearchText || scope.alarmDataKeySearchText === '') {
                scope.alarmDataKeySearchText = scope.alarmDataKeySearchText === '' ? null : '';
            }
        };

        scope.transformDataKeyChip = function (chip) {
            if (scope.maxDataKeys > 0 && ngModelCtrl.$viewValue.dataKeys.length >= scope.maxDataKeys ) {
                return null;
            } else {
                if (chip.type) {
                    return scope.generateDataKey({chip: chip.name, type: chip.type});
                } else {
                    if (scope.widgetType != types.widgetType.latest.value) {
                        return scope.generateDataKey({chip: chip, type: types.dataKeyType.timeseries});
                    } else {
                        return null;
                    }
                }
            }
        };

        scope.transformAlarmDataKeyChip = function (chip) {
            if (chip.type) {
                return scope.generateDataKey({chip: chip.name, type: chip.type});
            } else {
                return scope.generateDataKey({chip: chip, type: types.dataKeyType.alarm});
            }
        };

        scope.showColorPicker = function (event, dataKey) {
            $mdColorPicker.show({
                value: dataKey.color,
                defaultValue: '#fff',
                random: tinycolor.random(),
                clickOutsideToClose: false,
                hasBackdrop: false,
                multiple: true,
                preserveScope: false,

                mdColorAlphaChannel: true,
                mdColorSpectrum: true,
                mdColorSliders: true,
                mdColorGenericPalette: false,
                mdColorMaterialPalette: true,
                mdColorHistory: false,
                mdColorDefaultTab: 2,

                $event: event

            }).then(function (color) {
                dataKey.color = color;
                ngModelCtrl.$setDirty();
            });
        };

        scope.editDataKey = function (event, dataKey) {

            $mdDialog.show({
                controller: 'DatakeyConfigDialogController',
                controllerAs: 'vm',
                templateUrl: datakeyConfigDialogTemplate,
                locals: {
                    dataKey: angular.copy(dataKey),
                    dataKeySettingsSchema: scope.datakeySettingsSchema,
                    entityAlias: scope.entityAlias,
                    aliasController: scope.aliasController
                },
                parent: angular.element($document[0].body),
                fullscreen: true,
                targetEvent: event,
                multiple: true,
                onComplete: function () {
                    var w = angular.element($window);
                    w.triggerHandler('resize');
                }
            }).then(function (newDataKey) {
                if ((newDataKey.type === types.dataKeyType.timeseries) || (newDataKey.type === types.dataKeyType.attribute) || (newDataKey.type === types.dataKeyType.entityField)) {
                    let index = scope.dataKeys.indexOf(dataKey);
                    scope.dataKeys[index] = newDataKey;
                } else if (newDataKey.type === types.dataKeyType.alarm) {
                    let index = scope.alarmDataKeys.indexOf(dataKey);
                    scope.alarmDataKeys[index] = newDataKey;
                }
                ngModelCtrl.$setDirty();
            }, function () {
            });
        };

        scope.dataKeysSearch = function (searchText) {
            if (scope.widgetType == types.widgetType.alarm.value) {
                var dataKeys = searchText ? scope.alarmFields.filter(
                    scope.createFilterForDataKey(searchText)) : scope.alarmFields;
                return dataKeys;
            } else {
                if (scope.entityAlias) {
                    var deferred = $q.defer();
                    scope.fetchEntityKeys({entityAliasId: scope.entityAlias.id, query: searchText, type: types.dataKeyType.timeseries})
                        .then(function (dataKeys) {
                            var items = [];
                            for (var i = 0; i < dataKeys.length; i++) {
                                items.push({ name: dataKeys[i], type: types.dataKeyType.timeseries });
                            }
                            if (scope.widgetType == types.widgetType.latest.value) {
                                var keysType = [types.dataKeyType.attribute, types.dataKeyType.entityField];
                                var promises = [];
                                keysType.forEach((type) => {
                                    promises.push(scope.fetchEntityKeys({entityAliasId: scope.entityAlias.id, query: searchText, type: type}));
                                });
                                $q.all(promises).then(function (dataKeys) {
                                        for (var i = 0; i < dataKeys.length; i++) {
                                            for (var j = 0; j < dataKeys[i].length; j++) {
                                                items.push({name: dataKeys[i][j], type: keysType[i]});
                                            }
                                        }
                                        deferred.resolve(items);
                                    }, function (e) {
                                        deferred.reject(e);
                                    });
							}
							else
                                deferred.resolve(items);
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

        scope.createKey = function (event, type, chipsId) {
            var chipsChild = $(chipsId, element)[0].firstElementChild;
            var el = angular.element(chipsChild);
            var chipBuffer = el.scope().$mdChipsCtrl.getChipBuffer();
            event.preventDefault();
            event.stopPropagation();
            el.scope().$mdChipsCtrl.appendChip({ name: chipBuffer.trim(), type: type});
            el.scope().$mdChipsCtrl.resetChipBuffer();
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            widgetType: '=',
            maxDataKeys: '=',
            optDataKeys: '=',
            aliasController: '=',
            datakeySettingsSchema: '=',
            generateDataKey: '&',
            fetchEntityKeys: '&',
            onCreateEntityAlias: '&'
        },
        link: linker
    };
}

/* eslint-enable angular/angularelement */
