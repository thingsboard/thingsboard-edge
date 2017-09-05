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
/* eslint-disable import/no-unresolved, import/default */

import materialIconsCodepoints from 'raw-loader!material-design-icons/iconfont/codepoints';

/* eslint-enable import/no-unresolved, import/default */

import tinycolor from 'tinycolor2';
import jsonSchemaDefaults from 'json-schema-defaults';
import base64js from 'base64-js';
import {utf8Encode, utf8Decode} from './utf8-support';

import thingsboardTypes from './types.constant';

export default angular.module('thingsboard.utils', [thingsboardTypes])
    .factory('utils', Utils)
    .name;

const varsRegex = /\$\{([^\}]*)\}/g;

/*@ngInject*/
function Utils($mdColorPalette, $rootScope, $window, $filter, $translate, $q, $timeout, types) {

    var predefinedFunctions = {},
        predefinedFunctionsList = [],
        materialColors = [],
        materialIcons = [];

    var commonMaterialIcons = [ 'more_horiz', 'more_vert', 'open_in_new', 'visibility', 'play_arrow', 'arrow_back', 'arrow_downward',
        'arrow_forward', 'arrow_upwards', 'close', 'refresh', 'menu', 'show_chart', 'multiline_chart', 'pie_chart', 'insert_chart', 'people',
        'person', 'domain', 'devices_other', 'now_widgets', 'dashboards', 'map', 'pin_drop', 'my_location', 'extension', 'search',
        'settings', 'notifications', 'notifications_active', 'info', 'info_outline', 'warning', 'list', 'file_download', 'import_export',
        'share', 'add', 'edit', 'done' ];

    predefinedFunctions['Sin'] = "return Math.round(1000*Math.sin(time/5000));";
    predefinedFunctions['Cos'] = "return Math.round(1000*Math.cos(time/5000));";
    predefinedFunctions['Random'] =
        "var value = prevValue + Math.random() * 100 - 50;\n" +
        "var multiplier = Math.pow(10, 2 || 0);\n" +
        "var value = Math.round(value * multiplier) / multiplier;\n" +
        "if (value < -1000) {\n" +
        "	value = -1000;\n" +
        "} else if (value > 1000) {\n" +
        "	value = 1000;\n" +
        "}\n" +
        "return value;";

    for (var func in predefinedFunctions) {
        predefinedFunctionsList.push(func);
    }

    var colorPalettes = ['blue', 'green', 'red', 'amber', 'blue-grey', 'purple', 'light-green', 'indigo', 'pink', 'yellow', 'light-blue', 'orange', 'deep-purple', 'lime', 'teal', 'brown', 'cyan', 'deep-orange', 'grey'];
    var colorSpectrum = ['500', 'A700', '600', '700', '800', '900', '300', '400', 'A200', 'A400'];

    angular.forEach($mdColorPalette, function (value, key) {
        angular.forEach(value, function (color, label) {
            if (colorSpectrum.indexOf(label) > -1) {
                var rgb = 'rgb(' + color.value[0] + ',' + color.value[1] + ',' + color.value[2] + ')';
                color = tinycolor(rgb);
                var isDark = color.isDark();
                var colorItem = {
                    value: color.toHexString(),
                    group: key,
                    label: label,
                    isDark: isDark
                };
                materialColors.push(colorItem);
            }
        });
    });

    materialColors.sort(function (colorItem1, colorItem2) {
        var spectrumIndex1 = colorSpectrum.indexOf(colorItem1.label);
        var spectrumIndex2 = colorSpectrum.indexOf(colorItem2.label);
        var result = spectrumIndex1 - spectrumIndex2;
        if (result === 0) {
            var paletteIndex1 = colorPalettes.indexOf(colorItem1.group);
            var paletteIndex2 = colorPalettes.indexOf(colorItem2.group);
            result = paletteIndex1 - paletteIndex2;
        }
        return result;
    });

    var defaultDataKey = {
        name: 'f(x)',
        type: types.dataKeyType.function,
        label: 'Sin',
        color: getMaterialColor(0),
        funcBody: getPredefinedFunctionBody('Sin'),
        settings: {},
        _hash: Math.random()
    };

    var defaultDatasource = {
        type: types.datasourceType.function,
        name: types.datasourceType.function,
        dataKeys: [angular.copy(defaultDataKey)]
    };

    var defaultAlarmFields = [
        types.alarmFields.createdTime.keyName,
        types.alarmFields.originator.keyName,
        types.alarmFields.type.keyName,
        types.alarmFields.severity.keyName,
        types.alarmFields.status.keyName
    ];

    var defaultAlarmDataKeys = [];
    for (var i=0;i<defaultAlarmFields.length;i++) {
        var name = defaultAlarmFields[i];
        var dataKey = {
            name: name,
            type: types.dataKeyType.alarm,
            label: $translate.instant(types.alarmFields[name].name)+'',
            color: getMaterialColor(i),
            settings: {},
            _hash: Math.random()
        };
        defaultAlarmDataKeys.push(dataKey);
    }

    var service = {
        getDefaultDatasource: getDefaultDatasource,
        getDefaultDatasourceJson: getDefaultDatasourceJson,
        getDefaultAlarmDataKeys: getDefaultAlarmDataKeys,
        defaultAlarmFieldContent: defaultAlarmFieldContent,
        getMaterialColor: getMaterialColor,
        getMaterialIcons: getMaterialIcons,
        getCommonMaterialIcons: getCommonMaterialIcons,
        getPredefinedFunctionBody: getPredefinedFunctionBody,
        getPredefinedFunctionsList: getPredefinedFunctionsList,
        genMaterialColor: genMaterialColor,
        objectHashCode: objectHashCode,
        parseException: parseException,
        processWidgetException: processWidgetException,
        isDescriptorSchemaNotEmpty: isDescriptorSchemaNotEmpty,
        filterSearchTextEntities: filterSearchTextEntities,
        guid: guid,
        cleanCopy: cleanCopy,
        isLocalUrl: isLocalUrl,
        validateDatasources: validateDatasources,
        createKey: createKey,
        createLabelFromDatasource: createLabelFromDatasource,
        insertVariable: insertVariable,
        customTranslation: customTranslation,
        objToBase64: objToBase64,
        base64toObj: base64toObj,
        groupConfigDefaults: groupConfigDefaults,
        groupSettingsDefaults: groupSettingsDefaults
    }

    return service;

    function getPredefinedFunctionsList() {
        return predefinedFunctionsList;
    }

    function getPredefinedFunctionBody(func) {
        return predefinedFunctions[func];
    }

    function getMaterialColor(index) {
        var colorIndex = index % materialColors.length;
        return materialColors[colorIndex].value;
    }

    function getMaterialIcons() {
        var deferred = $q.defer();
        if (materialIcons.length) {
            deferred.resolve(materialIcons);
        } else {
            $timeout(function() {
                var codepointsArray = materialIconsCodepoints.split("\n");
                codepointsArray.forEach(function (codepoint) {
                    if (codepoint && codepoint.length) {
                        var values = codepoint.split(' ');
                        if (values && values.length == 2) {
                            materialIcons.push(values[0]);
                        }
                    }
                });
                deferred.resolve(materialIcons);
            });
        }
        return deferred.promise;
    }

    function getCommonMaterialIcons() {
        return commonMaterialIcons;
    }

    function genMaterialColor(str) {
        var hash = Math.abs(hashCode(str));
        return getMaterialColor(hash);
    }

    function hashCode(str) {
        var hash = 0;
        var i, char;
        if (str.length == 0) return hash;
        for (i = 0; i < str.length; i++) {
            char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32bit integer
        }
        return hash;
    }

    function objectHashCode(obj) {
        var hash = 0;
        if (obj) {
            var str = angular.toJson(obj);
            hash = hashCode(str);
        }
        return hash;
    }

    function parseException(exception, lineOffset) {
        var data = {};
        if (exception) {
            if (angular.isString(exception) || exception instanceof String) {
                data.message = exception;
            } else {
                if (exception.name) {
                    data.name = exception.name;
                } else {
                    data.name = 'UnknownError';
                }
                if (exception.message) {
                    data.message = exception.message;
                }
                if (exception.lineNumber) {
                    data.lineNumber = exception.lineNumber;
                    if (exception.columnNumber) {
                        data.columnNumber = exception.columnNumber;
                    }
                } else if (exception.stack) {
                    var lineInfoRegexp = /(.*<anonymous>):(\d*)(:)?(\d*)?/g;
                    var lineInfoGroups = lineInfoRegexp.exec(exception.stack);
                    if (lineInfoGroups != null && lineInfoGroups.length >= 3) {
                        if (angular.isUndefined(lineOffset)) {
                            lineOffset = -2;
                        }
                        data.lineNumber = Number(lineInfoGroups[2]) + lineOffset;
                        if (lineInfoGroups.length >= 5) {
                            data.columnNumber = lineInfoGroups[4];
                        }
                    }
                }
            }
        }
        return data;
    }

    function processWidgetException(exception) {
        var parentScope = $window.parent.angular.element($window.frameElement).scope();
        var data = parseException(exception, -5);
        if ($rootScope.widgetEditMode) {
            parentScope.$emit('widgetException', data);
            parentScope.$apply();
        }
        return data;
    }

    function getDefaultDatasource(dataKeySchema) {
        var datasource = angular.copy(defaultDatasource);
        if (angular.isDefined(dataKeySchema)) {
            datasource.dataKeys[0].settings = jsonSchemaDefaults(dataKeySchema);
        }
        return datasource;
    }

    function getDefaultDatasourceJson(dataKeySchema) {
        return angular.toJson(getDefaultDatasource(dataKeySchema));
    }

    function getDefaultAlarmDataKeys() {
        return angular.copy(defaultAlarmDataKeys);
    }

    function defaultAlarmFieldContent(key, value) {
        if (angular.isDefined(value)) {
            var alarmField = types.alarmFields[key.name];
            if (alarmField) {
                if (alarmField.time) {
                    return $filter('date')(value, 'yyyy-MM-dd HH:mm:ss');
                } else if (alarmField.value == types.alarmFields.severity.value) {
                    return $translate.instant(types.alarmSeverity[value].name);
                } else if (alarmField.value == types.alarmFields.status.value) {
                    return $translate.instant('alarm.display-status.'+value);
                } else if (alarmField.value == types.alarmFields.originatorType.value) {
                    return $translate.instant(types.entityTypeTranslations[value].type);
                }
                else {
                    return value;
                }
            } else {
                return value;
            }
        } else {
            return '';
        }
    }

    function isDescriptorSchemaNotEmpty(descriptor) {
        if (descriptor && descriptor.schema && descriptor.schema.properties) {
            for(var prop in descriptor.schema.properties) {
                if (descriptor.schema.properties.hasOwnProperty(prop)) {
                    return true;
                }
            }
        }
        return false;
    }

    function filterSearchTextEntities(entities, searchTextField, pageLink, deferred) {
        var response = {
            data: [],
            hasNext: false,
            nextPageLink: null
        };
        var limit = pageLink.limit;
        var textSearch = '';
        if (pageLink.textSearch) {
            textSearch = pageLink.textSearch.toLowerCase();
        }

        for (var i=0;i<entities.length;i++) {
            var entity = entities[i];
            var text = entity[searchTextField].toLowerCase();
            var createdTime = entity.createdTime;
            if (pageLink.textOffset && pageLink.textOffset.length > 0) {
                var comparison = text.localeCompare(pageLink.textOffset);
                if (comparison === 0
                    && createdTime < pageLink.createdTimeOffset) {
                    response.data.push(entity);
                    if (response.data.length === limit) {
                        break;
                    }
                } else if (comparison > 0 && text.startsWith(textSearch)) {
                    response.data.push(entity);
                    if (response.data.length === limit) {
                        break;
                    }
                }
            } else if (textSearch.length > 0) {
                if (text.startsWith(textSearch)) {
                    response.data.push(entity);
                    if (response.data.length === limit) {
                        break;
                    }
                }
            } else {
                response.data.push(entity);
                if (response.data.length === limit) {
                    break;
                }
            }
        }
        if (response.data.length === limit) {
            var lastEntity = response.data[limit-1];
            response.nextPageLink = {
                limit: pageLink.limit,
                textSearch: textSearch,
                idOffset: lastEntity.id.id,
                createdTimeOffset: lastEntity.createdTime,
                textOffset: lastEntity[searchTextField].toLowerCase()
            };
            response.hasNext = true;
        }
        deferred.resolve(response);
    }

    function guid() {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    }

    function cleanCopy(object) {
        var copy = angular.copy(object);
        for (var prop in copy) {
            if (prop && prop.startsWith('$$')) {
                delete copy[prop];
            }
        }
        return copy;
    }

    function genNextColor(datasources) {
        var index = 0;
        if (datasources) {
            for (var i = 0; i < datasources.length; i++) {
                var datasource = datasources[i];
                index += datasource.dataKeys.length;
            }
        }
        return getMaterialColor(index);
    }

    function isLocalUrl(url) {
        var parser = document.createElement('a'); //eslint-disable-line
        parser.href = url;
        var host = parser.hostname;
        if (host === "localhost" || host === "127.0.0.1") {
            return true;
        } else {
            return false;
        }
    }

    function validateDatasources(datasources) {
        datasources.forEach(function (datasource) {
            if (datasource.type === 'device') {
                datasource.type = types.datasourceType.entity;
                datasource.entityType = types.entityType.device;
                if (datasource.deviceId) {
                    datasource.entityId = datasource.deviceId;
                } else if (datasource.deviceAliasId) {
                    datasource.entityAliasId = datasource.deviceAliasId;
                }
                if (datasource.deviceName) {
                    datasource.entityName = datasource.deviceName;
                }
            }
            if (datasource.type === types.datasourceType.entity && datasource.entityId) {
                datasource.name = datasource.entityName;
            }
        });
        return datasources;
    }

    function createKey(keyInfo, type, datasources) {
        var label;
        if (type === types.dataKeyType.alarm && !keyInfo.label) {
            var alarmField = types.alarmFields[keyInfo.name];
            if (alarmField) {
                label = $translate.instant(alarmField.name)+'';
            }
        }
        if (!label) {
            label = keyInfo.label || keyInfo.name;
        }
        var dataKey = {
            name: keyInfo.name,
            type: type,
            label: label,
            funcBody: keyInfo.funcBody,
            settings: {},
            _hash: Math.random()
        }
        if (keyInfo.color) {
            dataKey.color = keyInfo.color;
        } else {
            dataKey.color = genNextColor(datasources);
        }
        return dataKey;
    }

    function createLabelFromDatasource(datasource, pattern) {
        var label = angular.copy(pattern);
        var match = varsRegex.exec(pattern);
        while (match !== null) {
            var variable = match[0];
            var variableName = match[1];
            if (variableName === 'dsName') {
                label = label.split(variable).join(datasource.name);
            } else if (variableName === 'entityName') {
                label = label.split(variable).join(datasource.entityName);
            } else if (variableName === 'deviceName') {
                label = label.split(variable).join(datasource.entityName);
            } else if (variableName === 'aliasName') {
                label = label.split(variable).join(datasource.aliasName);
            }
            match = varsRegex.exec(pattern);
        }
        return label;
    }

    function insertVariable(pattern, name, value) {
        var result = angular.copy(pattern);
        var match = varsRegex.exec(pattern);
        while (match !== null) {
            var variable = match[0];
            var variableName = match[1];
            if (variableName === name) {
                result = result.split(variable).join(value);
            }
            match = varsRegex.exec(pattern);
        }
        return result;
    }

    function customTranslation(translationValue, defaultValue) {
        var result = '';
        var translationId = types.translate.customTranslationsPrefix + translationValue;
        var translation = $translate.instant(translationId);
        if (translation != translationId) {
            result = translation + '';
        } else {
            result = defaultValue;
        }
        return result;
    }

    function objToBase64(obj) {
        var json = angular.toJson(obj);
        var encoded = utf8Encode(json);
        var b64Encoded = base64js.fromByteArray(encoded);
        return b64Encoded;
    }

    function base64toObj(b64Encoded) {
        var encoded = base64js.toByteArray(b64Encoded);
        var json = utf8Decode(encoded);
        var obj = angular.fromJson(json);
        return obj;
    }

    function groupConfigDefaults(groupConfig) {
        groupConfig.deleteEntityTitle = groupConfig.deleteEntityTitle ||
            (() => { return $translate.instant('entity-table.delete-entity-title'); });

        groupConfig.deleteEntityContent = groupConfig.deleteEntityContent ||
            (() => { return $translate.instant('entity-table.delete-entity-text'); });

        groupConfig.deleteEntitiesTitle = groupConfig.deleteEntitiesTitle ||
            ((count) => { return $translate.instant('entity-table.delete-entities-title', {count: count}, 'messageformat'); });

        groupConfig.deleteEntitiesContent = groupConfig.deleteEntitiesContent ||
            (() => { return $translate.instant('entity-table.delete-entities-text'); });

        groupConfig.actionCellDescriptors = groupConfig.actionCellDescriptors || [];
        groupConfig.groupActionDescriptors = groupConfig.groupActionDescriptors || [];

        groupConfig.addEnabled = groupConfig.addEnabled ||
            (() => { return true });

        groupConfig.deleteEnabled = groupConfig.deleteEnabled ||
            (() => { return true });

        groupConfig.entitiesDeleteEnabled = groupConfig.entitiesDeleteEnabled ||
            (() => { return true });
        groupConfig.detailsReadOnly = groupConfig.detailsReadOnly ||
            (() => { return false });

        groupConfig.loadEntity = groupConfig.loadEntity ||
            ((/*entityId*/) => { return $q.when() });

        groupConfig.deleteEntity = groupConfig.deleteEntity ||
            (() => { return $q.when() });

        groupConfig.saveEntity = groupConfig.saveEntity ||
            ((entity) => { return $q.when(entity) });

       // groupConfig.addEntityController = groupConfig.addEntityController || 'AddEntityController';
       // groupConfig.addEntityTemplateUrl = groupConfig.addEntityTemplateUrl || addEntityTemplate;

    }

    function groupSettingsDefaults(entityType, settings) {
        if (angular.isUndefined(settings.groupTableTitle)) {
            settings.groupTableTitle = '';
        }
        if (angular.isUndefined(settings.enableSearch)) {
            settings.enableSearch = true;
        }
        if (angular.isUndefined(settings.enableAdd)) {
            settings.enableAdd = true;
        }
        if (angular.isUndefined(settings.enableDelete)) {
            settings.enableDelete = true;
        }
        if (angular.isUndefined(settings.enableSelection)) {
            settings.enableSelection = true;
        }
        if (angular.isUndefined(settings.enableGroupTransfer)) {
            settings.enableGroupTransfer = true;
        }
        if (angular.isUndefined(settings.detailsMode)) {
            settings.detailsMode = types.entityGroup.detailsMode.onRowClick.value;
        }
        if (angular.isUndefined(settings.displayPagination)) {
            settings.displayPagination = true;
        }
        if (angular.isUndefined(settings.defaultPageSize)) {
            settings.defaultPageSize = 10;
        }
        if (entityType == types.entityType.device || entityType == types.entityType.asset) {
            if (angular.isUndefined(settings.enableAssignment)) {
                settings.enableAssignment = true;
            }
        }
        if (entityType == types.entityType.device) {
            if (angular.isUndefined(settings.enableCredentialsManagement)) {
                settings.enableCredentialsManagement = true;
            }
        }
        if (entityType == types.entityType.customer) {
            if (angular.isUndefined(settings.enableUsersManagement)) {
                settings.enableUsersManagement = true;
            }
            if (angular.isUndefined(settings.enableAssetsManagement)) {
                settings.enableAssetsManagement = true;
            }
            if (angular.isUndefined(settings.enableDevicesManagement)) {
                settings.enableDevicesManagement = true;
            }
            if (angular.isUndefined(settings.enableDashboardsManagement)) {
                settings.enableDashboardsManagement = true;
            }
        }
        return settings;
    }
}
