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
/* eslint-disable import/no-unresolved, import/default */

import importDialogTemplate from './import-dialog.tpl.html';
import entityAliasesTemplate from '../entity/alias/entity-aliases.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable no-undef, angular/window-service, angular/document-service */

/*@ngInject*/
export default function ImportExport($log, $translate, $q, $mdDialog, $document, $http, itembuffer, utils, types,
                                     dashboardUtils, entityService, dashboardService, ruleChainService,
                                     converterService, widgetService, toast, attributeService) {


    const JSON_TYPE = {
        mimeType: 'text/json',
        extension: 'json'
    };
    const CSV_TYPE = {
        mimeType: 'attachament/csv',
        extension: 'csv'
    };

    const XLS_TYPE = {
        mimeType: 'application/vnd.ms-excel',
        extension: 'xls'
    };

    const TEMPLATE_XLS = `
        <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">
        <meta http-equiv="content-type" content="application/vnd.ms-excel; charset=UTF-8"/>
        <head><!--[if gte mso 9]><xml>
        <x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>{title}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml>
        <![endif]--></head>
        <body>{table}</body></html>`;

    var service = {
        exportDashboard: exportDashboard,
        importDashboard: importDashboard,
        exportWidget: exportWidget,
        importWidget: importWidget,
        exportConverter: exportConverter,
        importConverter: importConverter,
        exportRuleChain: exportRuleChain,
        importRuleChain: importRuleChain,
        exportWidgetType: exportWidgetType,
        importWidgetType: importWidgetType,
        exportWidgetsBundle: exportWidgetsBundle,
        importWidgetsBundle: importWidgetsBundle,
        exportCsv: exportCsv,
        exportXls: exportXls,
        exportExtension: exportExtension,
        importExtension: importExtension,
        exportToPc: exportToPc
    };

    return service;

    // Widgets bundle functions

    function exportWidgetsBundle(widgetsBundleId) {
        widgetService.getWidgetsBundle(widgetsBundleId).then(
            function success(widgetsBundle) {
                var bundleAlias = widgetsBundle.alias;
                var isSystem = widgetsBundle.tenantId.id === types.id.nullUid;
                widgetService.getBundleWidgetTypes(bundleAlias, isSystem).then(
                    function success (widgetTypes) {
                        prepareExport(widgetsBundle);
                        var widgetsBundleItem = {
                           widgetsBundle:  prepareExport(widgetsBundle),
                           widgetTypes: []
                        };
                        for (var t in widgetTypes) {
                            var widgetType = widgetTypes[t];
                            if (angular.isDefined(widgetType.bundleAlias)) {
                                delete widgetType.bundleAlias;
                            }
                            widgetsBundleItem.widgetTypes.push(prepareExport(widgetType));
                        }
                        var name = widgetsBundle.title;
                        name = name.toLowerCase().replace(/\W/g,"_");
                        exportToPc(widgetsBundleItem, name);
                    },
                    function fail (rejection) {
                        var message = rejection;
                        if (!message) {
                            message = $translate.instant('error.unknown-error');
                        }
                        toast.showError($translate.instant('widgets-bundle.export-failed-error', {error: message}));
                    }
                );
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('widgets-bundle.export-failed-error', {error: message}));
            }
        );
    }

    function importNextWidgetType(widgetTypes, bundleAlias, index, deferred) {
        if (!widgetTypes || widgetTypes.length <= index) {
            deferred.resolve();
        } else {
            var widgetType = widgetTypes[index];
            widgetType.bundleAlias = bundleAlias;
            widgetService.saveImportedWidgetType(widgetType).then(
                function success() {
                    index++;
                    importNextWidgetType(widgetTypes, bundleAlias, index, deferred);
                },
                function fail() {
                    deferred.reject();
                }
            );

        }
    }

    function importWidgetsBundle($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'widgets-bundle.import', 'widgets-bundle.widgets-bundle-file').then(
            function success(widgetsBundleItem) {
                if (!validateImportedWidgetsBundle(widgetsBundleItem)) {
                    toast.showError($translate.instant('widgets-bundle.invalid-widgets-bundle-file-error'));
                    deferred.reject();
                } else {
                    var widgetsBundle = widgetsBundleItem.widgetsBundle;
                    widgetService.saveWidgetsBundle(widgetsBundle).then(
                        function success(savedWidgetsBundle) {
                            var bundleAlias = savedWidgetsBundle.alias;
                            var widgetTypes = widgetsBundleItem.widgetTypes;
                            importNextWidgetType(widgetTypes, bundleAlias, 0, deferred);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedWidgetsBundle(widgetsBundleItem) {
        if (angular.isUndefined(widgetsBundleItem.widgetsBundle)) {
            return false;
        }
        if (angular.isUndefined(widgetsBundleItem.widgetTypes)) {
            return false;
        }
        var widgetsBundle = widgetsBundleItem.widgetsBundle;
        if (angular.isUndefined(widgetsBundle.title)) {
            return false;
        }
        var widgetTypes = widgetsBundleItem.widgetTypes;
        for (var t in widgetTypes) {
            var widgetType = widgetTypes[t];
            if (!validateImportedWidgetType(widgetType)) {
                return false;
            }
        }

        return true;
    }

    // Widget type functions

    function exportWidgetType(widgetTypeId) {
        widgetService.getWidgetTypeById(widgetTypeId).then(
            function success(widgetType) {
                if (angular.isDefined(widgetType.bundleAlias)) {
                    delete widgetType.bundleAlias;
                }
                var name = widgetType.name;
                name = name.toLowerCase().replace(/\W/g,"_");
                exportToPc(prepareExport(widgetType), name);
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('widget-type.export-failed-error', {error: message}));
            }
        );
    }

    function importWidgetType($event, bundleAlias) {
        var deferred = $q.defer();
        openImportDialog($event, 'widget-type.import', 'widget-type.widget-type-file').then(
            function success(widgetType) {
                if (!validateImportedWidgetType(widgetType)) {
                    toast.showError($translate.instant('widget-type.invalid-widget-type-file-error'));
                    deferred.reject();
                } else {
                    widgetType.bundleAlias = bundleAlias;
                    widgetService.saveImportedWidgetType(widgetType).then(
                        function success() {
                            deferred.resolve();
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedWidgetType(widgetType) {
        if (angular.isUndefined(widgetType.name)
            || angular.isUndefined(widgetType.descriptor))
        {
            return false;
        }
        return true;
    }

    // Rule chain functions

    function exportRuleChain(ruleChainId) {
        ruleChainService.getRuleChain(ruleChainId).then(
            (ruleChain) => {
                ruleChainService.getRuleChainMetaData(ruleChainId).then(
                    (ruleChainMetaData) => {
                        var ruleChainExport = {
                            ruleChain: prepareRuleChain(ruleChain),
                            metadata: prepareRuleChainMetaData(ruleChainMetaData)
                        };
                        var name = ruleChain.name;
                        name = name.toLowerCase().replace(/\W/g,"_");
                        exportToPc(ruleChainExport, name);
                    },
                    (rejection) => {
                        processExportRuleChainRejection(rejection);
                    }
                );
            },
            (rejection) => {
                processExportRuleChainRejection(rejection);
            }
        );
    }

    function prepareRuleChain(ruleChain) {
        ruleChain = prepareExport(ruleChain);
        if (ruleChain.firstRuleNodeId) {
            ruleChain.firstRuleNodeId = null;
        }
        ruleChain.root = false;
        return ruleChain;
    }

    function prepareRuleChainMetaData(ruleChainMetaData) {
        delete ruleChainMetaData.ruleChainId;
        for (var i=0;i<ruleChainMetaData.nodes.length;i++) {
            var node = ruleChainMetaData.nodes[i];
            delete node.ruleChainId;
            ruleChainMetaData.nodes[i] = prepareExport(node);
        }
        return ruleChainMetaData;
    }


    function processExportRuleChainRejection(rejection) {
        var message = rejection;
        if (!message) {
            message = $translate.instant('error.unknown-error');
        }
        toast.showError($translate.instant('rulechain.export-failed-error', {error: message}));
    }

    function importRuleChain($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'rulechain.import', 'rulechain.rulechain-file').then(
            function success(ruleChainImport) {
                if (!validateImportedRuleChain(ruleChainImport)) {
                    toast.showError($translate.instant('rulechain.invalid-rulechain-file-error'));
                    deferred.reject();
                } else {
                    deferred.resolve(ruleChainImport);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedRuleChain(ruleChainImport) {
        if (angular.isUndefined(ruleChainImport.ruleChain)) {
            return false;
        }
        if (angular.isUndefined(ruleChainImport.metadata)) {
            return false;
        }
        if (angular.isUndefined(ruleChainImport.ruleChain.name)) {
            return false;
        }
        return true;
    }

    // Converter functions

    function exportConverter(converterId) {
        converterService.getConverter(converterId).then(
            function success(converter) {
                if (!converter.configuration || converter.configuration === null) {
                    converter.configuration = {};
                }
                var name = converter.name;
                name = name.toLowerCase().replace(/\W/g,"_");
                exportToPc(prepareExport(converter), name);
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('converter.export-failed-error', {error: message}));
            }
        );
    }

    function importConverter($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'converter.import', 'converter.converter-file').then(
            function success(converter) {
                if (!validateImportedConverter(converter)) {
                    toast.showError($translate.instant('converter.invalid-converter-file-error'));
                    deferred.reject();
                } else {
                    converterService.saveConverter(converter).then(
                        function success() {
                            deferred.resolve();
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedConverter(converter) {
        if (angular.isUndefined(converter.name)
            || angular.isUndefined(converter.type)
            || angular.isUndefined(converter.configuration))
        {
            return false;
        }
        if (!types.converterType[converter.type]) {
            return false;
        }
        if (converter.type == types.converterType.UPLINK.value) {
            if (!converter.configuration.decoder || !converter.configuration.decoder.length) {
                return false;
            }
        }
        if (converter.type == types.converterType.DOWNLINK.value) {
            if (!converter.configuration.encoder || !converter.configuration.encoder.length) {
                return false;
            }
        }
        return true;
    }

    // Widget functions

    function exportWidget(dashboard, sourceState, sourceLayout, widget) {
        var widgetItem = itembuffer.prepareWidgetItem(dashboard, sourceState, sourceLayout, widget);
        var name = widgetItem.widget.config.title;
        name = name.toLowerCase().replace(/\W/g,"_");
        exportToPc(prepareExport(widgetItem), name);
    }

    function prepareAliasesInfo(aliasesInfo) {
        var datasourceAliases = aliasesInfo.datasourceAliases;
        var targetDeviceAliases = aliasesInfo.targetDeviceAliases;
        var datasourceIndex;
        if (datasourceAliases || targetDeviceAliases) {
            if (datasourceAliases) {
                for (datasourceIndex in datasourceAliases) {
                    datasourceAliases[datasourceIndex] = prepareEntityAlias(datasourceAliases[datasourceIndex]);
                }
            }
            if (targetDeviceAliases) {
                for (datasourceIndex in targetDeviceAliases) {
                    targetDeviceAliases[datasourceIndex] = prepareEntityAlias(targetDeviceAliases[datasourceIndex]);
                }
            }
        }
        return aliasesInfo;
    }

    function prepareEntityAlias(aliasInfo) {
        var alias;
        var filter;
        if (aliasInfo.deviceId) {
            alias = aliasInfo.aliasName;
            filter = {
                type: types.aliasFilterType.entityList.value,
                entityType: types.entityType.device,
                entityList: [aliasInfo.deviceId],
                resolveMultiple: false
            };
        } else if (aliasInfo.deviceFilter) {
            alias = aliasInfo.aliasName;
            filter = {
                type: aliasInfo.deviceFilter.useFilter ? types.aliasFilterType.entityName.value : types.aliasFilterType.entityList.value,
                entityType: types.entityType.device,
                resolveMultiple: false
            }
            if (filter.type == types.aliasFilterType.entityList.value) {
                filter.entityList = aliasInfo.deviceFilter.deviceList
            } else {
                filter.entityNameFilter = aliasInfo.deviceFilter.deviceNameFilter;
            }
        } else if (aliasInfo.entityFilter) {
            alias = aliasInfo.aliasName;
            filter = {
                type: aliasInfo.entityFilter.useFilter ? types.aliasFilterType.entityName.value : types.aliasFilterType.entityList.value,
                entityType: aliasInfo.entityType,
                resolveMultiple: false
            }
            if (filter.type == types.aliasFilterType.entityList.value) {
                filter.entityList = aliasInfo.entityFilter.entityList;
            } else {
                filter.entityNameFilter = aliasInfo.entityFilter.entityNameFilter;
            }
        } else {
            alias = aliasInfo.alias;
            filter = aliasInfo.filter;
        }
        return {
            alias: alias,
            filter: filter
        };
    }

    function importWidget($event, dashboard, targetState, targetLayoutFunction, onAliasesUpdateFunction) {
        var deferred = $q.defer();
        openImportDialog($event, 'dashboard.import-widget', 'dashboard.widget-file').then(
            function success(widgetItem) {
                if (!validateImportedWidget(widgetItem)) {
                    toast.showError($translate.instant('dashboard.invalid-widget-file-error'));
                    deferred.reject();
                } else {
                    var widget = widgetItem.widget;
                    widget = dashboardUtils.validateAndUpdateWidget(widget);
                    var aliasesInfo = prepareAliasesInfo(widgetItem.aliasesInfo);
                    var originalColumns = widgetItem.originalColumns;
                    var originalSize = widgetItem.originalSize;

                    var datasourceAliases = aliasesInfo.datasourceAliases;
                    var targetDeviceAliases = aliasesInfo.targetDeviceAliases;
                    if (datasourceAliases || targetDeviceAliases) {
                        var entityAliases = {};
                        var datasourceAliasesMap = {};
                        var targetDeviceAliasesMap = {};
                        var aliasId;
                        var datasourceIndex;
                        if (datasourceAliases) {
                            for (datasourceIndex in datasourceAliases) {
                                aliasId = utils.guid();
                                datasourceAliasesMap[aliasId] = datasourceIndex;
                                entityAliases[aliasId] = datasourceAliases[datasourceIndex];
                                entityAliases[aliasId].id = aliasId;
                            }
                        }
                        if (targetDeviceAliases) {
                            for (datasourceIndex in targetDeviceAliases) {
                                aliasId = utils.guid();
                                targetDeviceAliasesMap[aliasId] = datasourceIndex;
                                entityAliases[aliasId] = targetDeviceAliases[datasourceIndex];
                                entityAliases[aliasId].id = aliasId;
                            }
                        }

                        var aliasIds = Object.keys(entityAliases);
                        if (aliasIds.length > 0) {
                            processEntityAliases(entityAliases, aliasIds).then(
                                function(missingEntityAliases) {
                                    if (Object.keys(missingEntityAliases).length > 0) {
                                        editMissingAliases($event, [ widget ],
                                              true, 'dashboard.widget-import-missing-aliases-title', missingEntityAliases).then(
                                            function success(updatedEntityAliases) {
                                                for (var aliasId in updatedEntityAliases) {
                                                    var entityAlias = updatedEntityAliases[aliasId];
                                                    var datasourceIndex;
                                                    if (datasourceAliasesMap[aliasId]) {
                                                        datasourceIndex = datasourceAliasesMap[aliasId];
                                                        datasourceAliases[datasourceIndex] = entityAlias;
                                                    } else if (targetDeviceAliasesMap[aliasId]) {
                                                        datasourceIndex = targetDeviceAliasesMap[aliasId];
                                                        targetDeviceAliases[datasourceIndex] = entityAlias;
                                                    }
                                                }
                                                addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                                                    aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                                            },
                                            function fail() {
                                                deferred.reject();
                                            }
                                        );
                                    } else {
                                        addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                                            aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                                    }
                                }
                            );
                        } else {
                            addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                                aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                        }
                    } else {
                        addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                            aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                    }
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedWidget(widgetItem) {
        if (angular.isUndefined(widgetItem.widget)
            || angular.isUndefined(widgetItem.aliasesInfo)
            || angular.isUndefined(widgetItem.originalColumns)) {
            return false;
        }
        var widget = widgetItem.widget;
        if (angular.isUndefined(widget.isSystemType) ||
            angular.isUndefined(widget.bundleAlias) ||
            angular.isUndefined(widget.typeAlias) ||
            angular.isUndefined(widget.type)) {
            return false;
        }
        return true;
    }

    function addImportedWidget(dashboard, targetState, targetLayoutFunction, event, widget,
                               aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred) {
        targetLayoutFunction(event).then(
            function success(targetLayout) {
                itembuffer.addWidgetToDashboard(dashboard, targetState, targetLayout, widget,
                    aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, -1, -1).then(
                        function() {
                            deferred.resolve(
                                {
                                    widget: widget,
                                    layoutId: targetLayout
                                }
                            );
                        }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
    }

    // Dashboard functions

    function exportDashboard(dashboardId) {
        dashboardService.getDashboard(dashboardId).then(
            function success(dashboard) {
                var name = dashboard.title;
                name = name.toLowerCase().replace(/\W/g,"_");
                exportToPc(prepareDashboardExport(dashboard), name);
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('dashboard.export-failed-error', {error: message}));
            }
        );
    }

    function prepareDashboardExport(dashboard) {
        dashboard = prepareExport(dashboard);
        delete dashboard.assignedCustomers;
        delete dashboard.publicCustomerId;
        delete dashboard.assignedCustomersText;
        delete dashboard.assignedCustomersIds;
        delete dashboard.ownerId;
        return dashboard;
    }

    function importDashboard($event, entityGroupId) {
        var deferred = $q.defer();
        openImportDialog($event, 'dashboard.import', 'dashboard.dashboard-file').then(
            function success(dashboard) {
                if (!validateImportedDashboard(dashboard)) {
                    toast.showError($translate.instant('dashboard.invalid-dashboard-file-error'));
                    deferred.reject();
                } else {
                    dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    var entityAliases = dashboard.configuration.entityAliases;
                    if (entityAliases) {
                        var aliasIds = Object.keys( entityAliases );
                        if (aliasIds.length > 0) {
                            processEntityAliases(entityAliases, aliasIds).then(
                                function(missingEntityAliases) {
                                    if (Object.keys( missingEntityAliases ).length > 0) {
                                        editMissingAliases($event, dashboard.configuration.widgets,
                                                false, 'dashboard.dashboard-import-missing-aliases-title', missingEntityAliases).then(
                                            function success(updatedEntityAliases) {
                                                for (var aliasId in updatedEntityAliases) {
                                                    entityAliases[aliasId] = updatedEntityAliases[aliasId];
                                                }
                                                saveImportedDashboard(dashboard, entityGroupId, deferred);
                                            },
                                            function fail() {
                                                deferred.reject();
                                            }
                                        );
                                    } else {
                                        saveImportedDashboard(dashboard, entityGroupId, deferred);
                                    }
                                }
                            )
                        } else {
                            saveImportedDashboard(dashboard, entityGroupId, deferred);
                        }
                    } else {
                        saveImportedDashboard(dashboard, entityGroupId, deferred);
                    }
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function saveImportedDashboard(dashboard, entityGroupId, deferred) {
        dashboardService.saveDashboard(dashboard, entityGroupId).then(
            function success() {
                deferred.resolve();
            },
            function fail() {
                deferred.reject();
            }
        )
    }

    function validateImportedDashboard(dashboard) {
        if (angular.isUndefined(dashboard.title) || angular.isUndefined(dashboard.configuration)) {
            return false;
        }
        return true;
    }



    function exportExtension(extensionId) {

        getExtension(extensionId)
            .then(
                function success(extension) {
                    var name = extension.title;
                    name = name.toLowerCase().replace(/\W/g,"_");
                    exportToPc(prepareExport(extension), name);
                },
                function fail(rejection) {
                    var message = rejection;
                    if (!message) {
                        message = $translate.instant('error.unknown-error');
                    }
                    toast.showError($translate.instant('extension.export-failed-error', {error: message}));
                }
            );

        function getExtension(extensionId) {
            var deferred = $q.defer();
            var url = '/api/plugins/telemetry/DEVICE/' + extensionId;
            $http.get(url, null)
                .then(function success(response) {
                    deferred.resolve(response.data);
                }, function fail() {
                    deferred.reject();
                });
            return deferred.promise;
        }

    }

    function importExtension($event, options) {
        var deferred = $q.defer();
        openImportDialog($event, 'extension.import-extensions', 'extension.file')
            .then(
                function success(extension) {
                    if (!validateImportedExtension(extension)) {
                        toast.showError($translate.instant('extension.invalid-file-error'));
                        deferred.reject();
                    } else {
                        attributeService
                            .saveEntityAttributes(
                                options.entityType,
                                options.entityId,
                                types.attributesScope.shared.value,
                                [{
                                    key: "configuration",
                                    value: angular.toJson(extension)
                                }]
                            )
                            .then(function success() {
                                options.successFunc();
                            });
                    }
                },
                function fail() {
                    deferred.reject();
                }
            );
        return deferred.promise;
    }

    function validateImportedExtension(configuration) {
        if (configuration.length) {
            for (let i = 0; i < configuration.length; i++) {
                if (angular.isUndefined(configuration[i].configuration) || angular.isUndefined(configuration[i].id )|| angular.isUndefined(configuration[i].type)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    function processEntityAliases(entityAliases, aliasIds) {
        var deferred = $q.defer();
        var missingEntityAliases = {};
        var index = -1;
        checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred);
        return deferred.promise;
    }

    function checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred) {
        index++;
        if (index == aliasIds.length) {
            deferred.resolve(missingEntityAliases);
        } else {
            checkEntityAlias(index, aliasIds, entityAliases, missingEntityAliases, deferred);
        }
    }

    function checkEntityAlias(index, aliasIds, entityAliases, missingEntityAliases, deferred) {
        var aliasId = aliasIds[index];
        var entityAlias = entityAliases[aliasId];
        entityService.checkEntityAlias(entityAlias).then(
            function(result) {
                if (result) {
                    checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred);
                } else {
                    var missingEntityAlias = angular.copy(entityAlias);
                    missingEntityAlias.filter = null;
                    missingEntityAliases[aliasId] = missingEntityAlias;
                    checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred);
                }
            }
        );
    }

    function editMissingAliases($event, widgets, isSingleWidget, customTitle, missingEntityAliases) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'EntityAliasesController',
            controllerAs: 'vm',
            templateUrl: entityAliasesTemplate,
            locals: {
                config: {
                    entityAliases: missingEntityAliases,
                    widgets: widgets,
                    isSingleWidget: isSingleWidget,
                    customTitle: customTitle,
                    disableAdd: true
                }
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (updatedEntityAliases) {
            deferred.resolve(updatedEntityAliases);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

    // Common functions

    function prepareExport(data) {
        var exportedData = angular.copy(data);
        if (angular.isDefined(exportedData.id)) {
            delete exportedData.id;
        }
        if (angular.isDefined(exportedData.createdTime)) {
            delete exportedData.createdTime;
        }
        if (angular.isDefined(exportedData.tenantId)) {
            delete exportedData.tenantId;
        }
        if (angular.isDefined(exportedData.customerId)) {
            delete exportedData.customerId;
        }
        return exportedData;
    }

    function openImportDialog($event, importTitle, importFileLabel) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'ImportDialogController',
            controllerAs: 'vm',
            templateUrl: importDialogTemplate,
            locals: {
                importTitle: importTitle,
                importFileLabel: importFileLabel
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (importData) {
            deferred.resolve(importData);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

    function exportToPc(data, filename) {
        if (!data) {
            $log.error('No data');
            return;
        }
        exportJson(data, filename);
    }

    function exportJson(data, filename) {
        if (angular.isObject(data)) {
            data = angular.toJson(data, 2);
        }
        downloadFile(data, filename, JSON_TYPE);
    }

    function exportCsv(data, filename) {
        var colsHead;
        var colsData;
        if (data && data.length) {
            formatDataAccordingToLocale(data);
            colsHead = Object.keys(data[0]).map(key => [key]).join(';');
            colsData = data.map(obj => [ // obj === row
                Object.keys(obj).map(col => [
                    obj[col]
                ]).join(';')
            ]).join('\n');
        } else {
            colsHead = '';
            colsData = '';
        }
        var csvData = `${colsHead}\n${colsData}`;
        downloadFile(csvData, filename, CSV_TYPE);
    }

    function exportXls(data, filename) {
        var colsHead;
        var colsData;
        if (data && data.length) {
            formatDataAccordingToLocale(data);
            colsHead = `<tr>${Object.keys(data[0]).map(key => `<td><b>${key}</b></td>`).join('')}</tr>`;
            colsData = data.map(obj => [`<tr>
                ${Object.keys(obj).map(col => `<td>${obj[col] ? obj[col] : ''}</td>`).join('')}
            </tr>`])
                .join('');
        } else {
            colsHead = '';
            colsData = '';
        }
        var tableData = `<table>${colsHead}${colsData}</table>`.trim();
        var parameters = { title: filename, table: tableData };
        var xlsData = TEMPLATE_XLS.replace(/{(\w+)}/g, (x, y) => parameters[y]);
        downloadFile(xlsData, filename, XLS_TYPE);
    }

    function downloadFile(data, filename, fileType) {
        if (!filename) {
            filename = 'download';
        }
        filename += '.' + fileType.extension;
        var blob = new Blob([data], {type: fileType.mimeType});
        // FOR IE:
        if (window.navigator && window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            var e = document.createEvent('MouseEvents'),
                a = document.createElement('a');
            a.download = filename;
            a.href = window.URL.createObjectURL(blob);
            a.dataset.downloadurl = [fileType.mimeType, a.download, a.href].join(':');
            e.initEvent('click', true, false, window,
                0, 0, 0, 0, 0, false, false, false, false, 0, null);
            a.dispatchEvent(e);
        }
    }


    function formatDataAccordingToLocale(data) {
        for (var i = 0; i < data.length; i++) {
            for (var key in data[i]) {
                if (angular.isNumber(data[i][key])) {
                    data[i][key] = data[i][key].toLocaleString(undefined, {maximumFractionDigits: 14});
                }
            }
        }
    }

}

/* eslint-enable no-undef, angular/window-service, angular/document-service */
