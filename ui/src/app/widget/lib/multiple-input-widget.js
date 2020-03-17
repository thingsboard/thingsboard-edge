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
function MultipleInputWidgetController($q, $scope, attributeService, toast, types, utils) {
    var vm = this;

    vm.dataKeyDetected = false;
    vm.hasAnyChange = false;
    vm.entityDetected = false;
    vm.isValidParameter = true;
    vm.message = 'No entity selected';

    vm.rows = [];
    vm.rowIndex = 0;

    vm.datasources = null;

    vm.cellStyle = cellStyle;
    vm.textColor = textColor;
    vm.discardAll = discardAll;
    vm.inputChanged = inputChanged;
    vm.postData = postData;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            initializeConfig();
            updateDatasources();
        }
    });

    $scope.$on('multiple-input-data-updated', function(event, formId) {
        if (vm.formId == formId) {
            updateRowData(vm.subscription.data);
            $scope.$digest();
        }
    });

    function defaultStyle() {
        return {};
    }

    function cellStyle(key, rowIndex, firstKey, lastKey) {
        var style = {};
        if (key) {
            var styleInfo = vm.stylesInfo[key.label];
            var value = key.currentValue;
            if (styleInfo.useCellStyleFunction && styleInfo.cellStyleFunction) {
                try {
                    style = styleInfo.cellStyleFunction(value);
                } catch (e) {
                    style = {};
                }
            } else {
                style = defaultStyle();
            }
        }
        if (vm.settings.rowMargin) {
            if (angular.isUndefined(style.marginTop) && rowIndex != 0) {
                style.marginTop = (vm.settings.rowMargin / 2) + 'px';
            }
            if (angular.isUndefined(style.marginBottom)) {
                style.marginBottom = (vm.settings.rowMargin / 2) + 'px';
            }
        }
        if (vm.settings.columnMargin) {
            if (angular.isUndefined(style.marginLeft) && !firstKey) {
                style.marginLeft = (vm.settings.columnMargin / 2) + 'px';
            }
            if (angular.isUndefined(style.marginRight) && !lastKey) {
                style.marginRight = (vm.settings.columnMargin / 2) + 'px';
            }
        }
        return style;
    }

    function textColor(key) {
        var style = {};
        if (key) {
            var styleInfo = vm.stylesInfo[key.label];
            if (styleInfo.color) {
                style = { color: styleInfo.color };
            }
        }
        return style;
    }

    function discardAll() {
        for (var r = 0; r < vm.rows.length; r++) {
            var row = vm.rows[r];
            for (var d = 0; d < row.data.length; d++ ) {
                row.data[d].currentValue = row.data[d].originalValue;
            }
        }
        vm.hasAnyChange = false;
    }

    function inputChanged() {
        var newValue = false;
        for (var r = 0; r < vm.rows.length; r++) {
            var row = vm.rows[r];
            for (var d = 0; d < row.data.length; d++ ) {
                if (!row.data[d].currentValue) {
                    return;
                }
                if (row.data[d].currentValue !== row.data[d].originalValue) {
                    newValue = true;
                }
            }
        }
        vm.hasAnyChange = newValue;
    }

    function postData() {
        var promises = [];
        for (var r = 0; r < vm.rows.length; r++) {
            var row = vm.rows[r];
            var datasource = row.datasource;
            var attributes = [];
            var newValues = false;

            for (var d = 0; d < row.data.length; d++ ) {
                if (row.data[d].currentValue !== row.data[d].originalValue) {
                    attributes.push({
                        key : row.data[d].name,
                        value : row.data[d].currentValue,
                    });
                    newValues = true;
                }
            }

            if (newValues) {
                promises.push(attributeService.saveEntityAttributes(
                                datasource.entityType,
                                datasource.entityId,
                                vm.attributeScope,
                                attributes));
            }
        }

        if (promises.length) {
            $q.all(promises).then(
                function success() {
                    for (var d = 0; d < row.data.length; d++ ) {
                        row.data[d].originalValue = row.data[d].currentValue;
                    }
                    vm.hasAnyChange = false;
                    if (vm.settings.showResultMessage) {
                        toast.showSuccess('Update successful', 1000, angular.element(vm.ctx.$container), 'bottom left');
                    }
                },
                function fail() {
                    if (vm.settings.showResultMessage) {
                        toast.showError('Update failed', angular.element(vm.ctx.$container), 'bottom left');
                    }
                }
            );
        }
    }

    function initializeConfig() {

        if (vm.settings.widgetTitle && vm.settings.widgetTitle.length) {
            vm.widgetTitle = utils.customTranslation(vm.settings.widgetTitle, vm.settings.widgetTitle);
        } else {
            vm.widgetTitle = vm.ctx.widgetConfig.title;
        }

        vm.ctx.widgetTitle = vm.widgetTitle;

        vm.attributeScope = vm.settings.attributesShared ? types.attributesScope.shared.value : types.attributesScope.server.value;
    }

    function updateDatasources() {

        vm.stylesInfo = {};
        vm.rows = [];
        vm.rowIndex = 0;

        if (vm.datasources) {
            vm.entityDetected = true;
            for (var ds = 0; ds < vm.datasources.length; ds++) {
                var row = {};
                var datasource = vm.datasources[ds];
                row.datasource = datasource;
                row.data = [];
                if (datasource.dataKeys) {
                    vm.dataKeyDetected = true;
                    for (var a = 0; a < datasource.dataKeys.length; a++ ) {
                        var dataKey = datasource.dataKeys[a];

                        if (dataKey.units) {
                            dataKey.label += ' (' + dataKey.units + ')';
                        }

                        var keySettings = dataKey.settings;
                        if (keySettings.inputTypeNumber) {
                            keySettings.inputType = 'number';
                        } else {
                            keySettings.inputType = 'text';
                        }

                        var cellStyleFunction = null;
                        var useCellStyleFunction = false;

                        if (keySettings.useCellStyleFunction === true) {
                            if (angular.isDefined(keySettings.cellStyleFunction) && keySettings.cellStyleFunction.length > 0) {
                                try {
                                    cellStyleFunction = new Function('value', keySettings.cellStyleFunction);
                                    useCellStyleFunction = true;
                                } catch (e) {
                                    cellStyleFunction = null;
                                    useCellStyleFunction = false;
                                }
                            }
                        }

                        vm.stylesInfo[dataKey.label] = {
                            useCellStyleFunction: useCellStyleFunction,
                            cellStyleFunction: cellStyleFunction,
                            color: keySettings.color
                        };

                        row.data.push(dataKey);
                    }
                    vm.rows.push(row);
                }
            }
        }
    }

    function updateRowData(data) {
        var dataIndex = 0;
        for (var r = 0; r < vm.rows.length; r++) {
            var row = vm.rows[r];
            for (var d = 0; d < row.data.length; d++ ) {
                var keyData = data[dataIndex++].data;
                if (keyData && keyData.length && keyData[0].length > 1) {
                    row.data[d].currentValue = row.data[d].originalValue = keyData[0][1];
                }
            }
        }
    }

}
