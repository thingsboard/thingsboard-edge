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
import './multiple-input-widget.scss';

/* eslint-disable import/no-unresolved, import/default */

import multipleInputWidgetTemplate from './multiple-input-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.multipleInputWidget', [])
    .directive('tbMultipleInputWidget', MultipleInputWidget)
    .name;

/*@ngInject*/
function MultipleInputWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            formId: '=',
            ctx: '='
        },
        controller: MultipleInputWidgetController,
        controllerAs: 'vm',
        templateUrl: multipleInputWidgetTemplate
    };
}

/*@ngInject*/
function MultipleInputWidgetController($q, $scope, $translate, attributeService, toast, types, utils) {
    var vm = this;

    vm.entityDetected = false;
    vm.isAllParametersValid = true;

    vm.sources = [];
    vm.datasources = null;

    vm.discardAll = discardAll;
    vm.inputChanged = inputChanged;
    vm.save = save;
    vm.getGroupTitle = getGroupTitle;

    $scope.$watch('vm.ctx', function () {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            initializeConfig();
            updateDatasources();
        }
    });

    $scope.$on('multiple-input-data-updated', function (event, formId) {
        if (vm.formId == formId) {
            updateWidgetData(vm.subscription.data);
            $scope.$digest();
        }
    });

    $scope.$on('multiple-input-resize', function (event, formId) {
        if (vm.formId == formId) {
            updateWidgetDisplaying();
        }
    });

    function discardAll() {
        for (var i = 0; i < vm.sources.length; i++) {
            for (var j = 0; j < vm.sources[i].keys.length; j++) {
                vm.sources[i].keys[j].data.currentValue = vm.sources[i].keys[j].data.originalValue;
            }
        }
        $scope.multipleInputForm.$setPristine();
    }

    function inputChanged(source, key) {
        if (!vm.settings.showActionButtons) {
            if (!key.settings.required || (key.settings.required && key.data && angular.isDefined(key.data.currentValue))) {
                var dataToSave = {
                    datasource: source.datasource,
                    keys: [key]
                };
                vm.save(dataToSave);
            }
        }
    }

    function save(dataToSave) {
        var tasks = [];
        var config = {
            ignoreLoading: !vm.settings.showActionButtons
        };
        var data;
        if (dataToSave) {
            data = [dataToSave];
        } else {
            data = vm.sources;
        }
        for (let i = 0; i < data.length; i++) {
            var serverAttributes = [], sharedAttributes = [], telemetry = [];
            for (let j = 0; j < data[i].keys.length; j++) {
                var key = data[i].keys[j];
                if ((key.data.currentValue !== key.data.originalValue) || vm.settings.updateAllValues) {
                    var attribute = {
                        key: key.name
                    };
                    if (key.data.currentValue) {
                        switch (key.settings.dataKeyValueType) {
                            case 'dateTime':
                            case 'date':
                                attribute.value = key.data.currentValue.getTime();
                                break;
                            case 'time':
                                attribute.value = key.data.currentValue.getTime() - moment().startOf('day').valueOf();//eslint-disable-line
                                break;
                            default:
                                attribute.value = key.data.currentValue;
                        }
                    } else {
                        if (key.data.currentValue === '') {
                            attribute.value = null;
                        } else {
                            attribute.value = key.data.currentValue;
                        }
                    }

                    switch (key.settings.dataKeyType) {
                        case 'shared':
                            sharedAttributes.push(attribute);
                            break;
                        case 'timeseries':
                            telemetry.push(attribute);
                            break;
                        default:
                            serverAttributes.push(attribute);
                    }
                }
            }
            if (serverAttributes.length) {
                tasks.push(attributeService.saveEntityAttributes(
                    data[i].datasource.entityType,
                    data[i].datasource.entityId,
                    types.attributesScope.server.value,
                    serverAttributes,
                    config));
            }
            if (sharedAttributes.length) {
                tasks.push(attributeService.saveEntityAttributes(
                    data[i].datasource.entityType,
                    data[i].datasource.entityId,
                    types.attributesScope.shared.value,
                    sharedAttributes,
                    config));
            }
            if (telemetry.length) {
                tasks.push(attributeService.saveEntityTimeseries(
                    data[i].datasource.entityType,
                    data[i].datasource.entityId,
                    types.latestTelemetry.value,
                    telemetry,
                    config));
            }
        }

        if (tasks.length) {
            $q.all(tasks).then(
                function success() {
                    $scope.multipleInputForm.$setPristine();
                    if (vm.settings.showResultMessage) {
                        toast.showSuccess($translate.instant('widgets.input-widgets.update-successful'), 1000, angular.element(vm.ctx.$container), 'bottom left');
                    }
                },
                function fail() {
                    if (vm.settings.showResultMessage) {
                        toast.showError($translate.instant('widgets.input-widgets.update-failed'), angular.element(vm.ctx.$container), 'bottom left');
                    }
                }
            );
        } else {
            $scope.multipleInputForm.$setPristine();
        }
    }

    function initializeConfig() {

        if (vm.settings.widgetTitle && vm.settings.widgetTitle.length) {
            vm.widgetTitle = utils.customTranslation(vm.settings.widgetTitle, vm.settings.widgetTitle);
        } else {
            vm.widgetTitle = vm.ctx.widgetConfig.title;
        }

        vm.ctx.widgetTitle = vm.widgetTitle;

        vm.settings.groupTitle = vm.settings.groupTitle || "${entityName}";

        //For backward compatibility
        if (angular.isUndefined(vm.settings.showActionButtons)) {
            vm.settings.showActionButtons = true;
        }
        if (angular.isUndefined(vm.settings.fieldsAlignment)) {
            vm.settings.fieldsAlignment = 'row';
        }
        if (angular.isUndefined(vm.settings.fieldsInRow)) {
            vm.settings.fieldsInRow = 2;
        }
        //For backward compatibility

        vm.isVerticalAlignment = !(vm.settings.fieldsAlignment === 'row');

        if (!vm.isVerticalAlignment && vm.settings.fieldsInRow) {
            vm.inputWidthSettings = 100 / vm.settings.fieldsInRow + '%';
        }
    }

    function updateDatasources() {
        if (vm.datasources && vm.datasources.length) {
            vm.entityDetected = true;
            for (var i = 0; i < vm.datasources.length; i++) {
                var datasource = vm.datasources[i];
                var source = {
                    datasource: datasource,
                    keys: []
                };
                if (datasource.type === types.datasourceType.entity) {
                    for (var j = 0; j < datasource.dataKeys.length; j++) {
                        if ((datasource.entityType !== types.entityType.device) && (datasource.dataKeys[j].settings.dataKeyType == 'shared')) {
                            vm.isAllParametersValid = false;
                        }
                        source.keys.push(datasource.dataKeys[j]);
                        if (source.keys[j].units) {
                            source.keys[j].label += ' (' + source.keys[j].units + ')';
                        }
                        source.keys[j].data = {};

                        //For backward compatibility
                        if (angular.isUndefined(source.keys[j].settings.dataKeyType)) {
                            if (vm.settings.attributesShared) {
                                source.keys[j].settings.dataKeyType = 'shared';
                            } else {
                                source.keys[j].settings.dataKeyType = 'server';
                            }
                        }

                        if (angular.isUndefined(source.keys[j].settings.dataKeyValueType)) {
                            if (source.keys[j].settings.inputTypeNumber) {
                                source.keys[j].settings.dataKeyValueType = 'double';
                            } else {
                                source.keys[j].settings.dataKeyValueType = 'string';
                            }
                        }

                        if (angular.isUndefined(source.keys[j].settings.isEditable)) {
                            if (source.keys[j].settings.readOnly) {
                                source.keys[j].settings.isEditable = 'readonly';
                            } else {
                                source.keys[j].settings.isEditable = 'editable';
                            }
                        }
                        //For backward compatibility

                    }
                } else {
                    vm.entityDetected = false;
                }
                vm.sources.push(source);
            }
        }
    }

    function updateWidgetData(data) {
        var dataIndex = 0;
        for (var i = 0; i < vm.sources.length; i++) {
            var source = vm.sources[i];
            for (var j = 0; j < source.keys.length; j++) {
                var keyData = data[dataIndex].data;
                var key = source.keys[j];
                if (keyData && keyData.length) {
                    var value;
                    switch (key.settings.dataKeyValueType) {
                        case 'dateTime':
                        case 'date':
                            value = moment(keyData[0][1]).toDate(); // eslint-disable-line
                            break;
                        case 'time':
                            value = moment().startOf('day').add(keyData[0][1], 'ms').toDate(); // eslint-disable-line
                            break;
                        case 'booleanCheckbox':
                        case 'booleanSwitch':
                            value = (keyData[0][1] === 'true');
                            break;
                        default:
                            value = keyData[0][1];
                    }

                    key.data = {
                        currentValue: value,
                        originalValue: value
                    };
                }

                if (key.settings.isEditable === 'editable' && key.settings.disabledOnDataKey) {
                    var conditions = data.filter((item) => {
                        return item.dataKey.name === key.settings.disabledOnDataKey;
                    });
                    if (conditions && conditions.length) {
                        if (conditions[0].data.length) {
                            if (conditions[0].data[0][1] === 'false') {
                                key.settings.disabledOnCondition = true;
                            } else {
                                key.settings.disabledOnCondition = !conditions[0].data[0][1];
                            }
                        }
                    }
                }
                dataIndex++;
            }
        }
    }

    function updateWidgetDisplaying() {
        vm.changeAlignment = (vm.ctx.$container[0].offsetWidth < 620);
        vm.smallWidthContainer = (vm.ctx.$container[0].offsetWidth < 420);
    }

    function getGroupTitle(datasource) {
        return utils.createLabelFromDatasource(datasource, vm.settings.groupTitle);
    }
}
