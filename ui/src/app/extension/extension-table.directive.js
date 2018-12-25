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
import 'angular-material-data-table/dist/md-data-table.min.css';
import './extension-table.scss';

/* eslint-disable import/no-unresolved, import/default */

import extensionTableTemplate from './extension-table.tpl.html';
import extensionDialogTemplate from './extension-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import ExtensionDialogController from './extension-dialog.controller'
import $ from 'jquery';

/*@ngInject*/
export default function ExtensionTableDirective() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            readonly: '=',
            entityId: '=',
            entityType: '@',
            inWidget: '@?',
            ctx: '=?',
            entityName: '='
        },
        controller: ExtensionTableController,
        controllerAs: 'vm',
        templateUrl: extensionTableTemplate
    };
}

/*@ngInject*/
function ExtensionTableController($scope, $filter, $document, $translate, $timeout, $mdDialog, types, attributeService, telemetryWebsocketService, importExport) {

    let vm = this;

    vm.extensions = [];
    vm.allExtensions = [];
    vm.selectedExtensions = [];
    vm.extensionsCount = 0;

    vm.query = {
        order: 'id',
        limit: 5,
        page: 1,
        search: null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.addExtension = addExtension;
    vm.editExtension = editExtension;
    vm.viewExtension = viewExtension;
    vm.deleteExtension = deleteExtension;
    vm.deleteExtensions = deleteExtensions;
    vm.reloadExtensions = reloadExtensions;
    vm.updateExtensions = updateExtensions;

    $scope.$watch("vm.entityId", function(newVal) {
        if (newVal) {
            if ($scope.subscriber) {
                telemetryWebsocketService.unsubscribe($scope.subscriber);
                $scope.subscriber = null;
            }

            vm.subscribed = false;
            vm.syncLastTime = $translate.instant('extension.sync.not-available');

            subscribeForClientAttributes();

            reloadExtensions();
        }
    });

    $scope.$on('$destroy', function() {
        if ($scope.subscriber) {
            telemetryWebsocketService.unsubscribe($scope.subscriber);
            $scope.subscriber = null;
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateExtensions();
        }
    });

    $scope.$watch('vm.selectedExtensions.length', function (newLength) {
        var selectionMode = newLength ? true : false;
        if (vm.ctx) {
            if (selectionMode) {
                vm.ctx.hideTitlePanel = true;
                $scope.$emit("selectedExtensions", true);
            } else if (vm.query.search == null) {
                vm.ctx.hideTitlePanel = false;
                $scope.$emit("selectedExtensions", false);
            }
        }
    });

    $scope.$on("showSearch", function($event, source) {
        if(source.entityId == vm.entityId) {
            enterFilterMode();
            $scope.$emit("filterMode", true);
        }
    });
    $scope.$on("refreshExtensions", function($event, source) {
        if(source.entityId == vm.entityId) {
            reloadExtensions();
        }
    });
    $scope.$on("addExtension", function($event, source) {
        if(source.entityId == vm.entityId) {
            addExtension();
        }
    });
    $scope.$on("exportExtensions", function($event, source) {
        if(source.entityId == vm.entityId) {
            vm.exportExtensions(source.entityName);
        }
    });
    $scope.$on("importExtensions", function($event, source) {
        if(source.entityId == vm.entityId) {
            vm.importExtensions();
        }
    });

    function enterFilterMode(event) {
        let $button = angular.element(event.currentTarget);
        let $toolbarsContainer = $button.closest('.toolbarsContainer');

        vm.query.search = '';
        if(vm.inWidget) {
            vm.ctx.hideTitlePanel = true;
        }
        $timeout(()=>{
            $toolbarsContainer.find('.searchInput').focus();
        })
    }

    function exitFilterMode() {
        vm.query.search = null;
        updateExtensions();
        if(vm.inWidget) {
            vm.ctx.hideTitlePanel = false;
            $scope.$emit("filterMode", false);
        }
    }

    function onReorder() {
        updateExtensions();
    }

    function onPaginate() {
        updateExtensions();
    }

    function addExtension($event) {
        if ($event) {
            $event.stopPropagation();
        }
        openExtensionDialog($event);
    }

    function editExtension($event, extension) {
        if ($event) {
            $event.stopPropagation();
        }
        openExtensionDialog($event, extension);
    }

    function viewExtension($event, extension) {
        if ($event) {
            $event.stopPropagation();
        }
        openExtensionDialog($event, extension, true);
    }

    function openExtensionDialog($event, extension, readonly) {
        if ($event) {
            $event.stopPropagation();
        }
        var isAdd = false;
        if(!extension) {
            isAdd = true;
        }
        $mdDialog.show({
            controller: ExtensionDialogController,
            controllerAs: 'vm',
            templateUrl: extensionDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                isAdd: isAdd,
                readonly: readonly,
                allExtensions: vm.allExtensions,
                entityId: vm.entityId,
                entityType: vm.entityType,
                extension: extension
            },
            bindToController: true,
            targetEvent: $event,
            fullscreen: true,
            multiple: true
        }).then(function() {
            reloadExtensions();
        }, function () {
        });
    }

    function deleteExtension($event, extension) {
        if ($event) {
            $event.stopPropagation();
        }
        if(extension) {
            var title = $translate.instant('extension.delete-extension-title', {extensionId: extension.id});
            var content = $translate.instant('extension.delete-extension-text');

            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function() {
                var editedExtensions = vm.allExtensions.filter(function(ext) {
                    return ext.id !== extension.id;
                });
                var editedValue = angular.toJson(editedExtensions);
                attributeService.saveEntityAttributes(vm.entityType, vm.entityId, types.attributesScope.shared.value, [{key:"configuration", value:editedValue}]).then(
                    function success() {
                        reloadExtensions();
                    }
                );
            });
        }
    }

    function deleteExtensions($event) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.selectedExtensions && vm.selectedExtensions.length > 0) {
            var title = $translate.instant('extension.delete-extensions-title', {count: vm.selectedExtensions.length}, 'messageformat');
            var content = $translate.instant('extension.delete-extensions-text');

            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(function () {
                var editedExtensions = angular.copy(vm.allExtensions);
                for (var i = 0; i < vm.selectedExtensions.length; i++) {
                    editedExtensions = editedExtensions.filter(function (ext) {
                        return ext.id !== vm.selectedExtensions[i].id;
                    });
                }
                var editedValue = angular.toJson(editedExtensions);
                attributeService.saveEntityAttributes(vm.entityType, vm.entityId, types.attributesScope.shared.value, [{key:"configuration", value:editedValue}]).then(
                    function success() {
                        reloadExtensions();
                    }
                );
            });
        }
    }

    function reloadExtensions() {
        vm.subscribed = false;
        vm.allExtensions.length = 0;
        vm.extensions.length = 0;
        vm.extensionsPromise = attributeService.getEntityAttributesValues(vm.entityType, vm.entityId, types.attributesScope.shared.value, ["configuration"]);
        vm.extensionsPromise.then(
            function success(data) {
                if (data.length) {
                    vm.allExtensions = angular.fromJson(data[0].value);
                } else {
                    vm.allExtensions = [];
                }

                vm.selectedExtensions = [];
                updateExtensions();
                vm.extensionsPromise = null;
            },
            function fail() {
                vm.extensions = [];
                vm.selectedExtensions = [];
                updateExtensions();
                vm.extensionsPromise = null;
            }
        );
    }

    function updateExtensions() {
        vm.selectedExtensions = [];
        var result = $filter('orderBy')(vm.allExtensions, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, function(extension) {
                if(!vm.query.search || (extension.id.indexOf(vm.query.search) != -1) || (extension.type.indexOf(vm.query.search) != -1)) {
                    return true;
                }
                return false;
            });
        }
        vm.extensionsCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.extensions = result.slice(startIndex, startIndex + vm.query.limit);

        vm.extensionsJSON = angular.toJson(vm.extensions);
        checkForSync();
    }

    function subscribeForClientAttributes() {
        if (!vm.subscribed) {
            if (vm.entityId && vm.entityType) {
                $scope.subscriber = {
                    subscriptionCommands: [{
                        entityType: vm.entityType,
                        entityId: vm.entityId,
                        scope: 'CLIENT_SCOPE'
                    }],
                    type: 'attribute',
                    onData: function (data) {
                        if (data.data) {
                            onSubscriptionData(data.data);
                        }
                        vm.subscribed = true;
                    }
                };
                telemetryWebsocketService.subscribe($scope.subscriber);
            }
        }
    }
    function onSubscriptionData(data) {

        if ($.isEmptyObject(data)) {
            vm.appliedConfiguration = undefined;
        } else {
            if (data.appliedConfiguration && data.appliedConfiguration[0] && data.appliedConfiguration[0][1]) {
                vm.appliedConfiguration = data.appliedConfiguration[0][1];
            }
        }

        updateExtensions();
        $scope.$digest();
    }


    function checkForSync() {
        if (vm.appliedConfiguration && vm.extensionsJSON && vm.appliedConfiguration === vm.extensionsJSON) {
            vm.syncStatus = $translate.instant('extension.sync.sync');
            vm.syncLastTime = formatDate();
            $scope.isSync = true;
        } else {
            vm.syncStatus = $translate.instant('extension.sync.not-sync');

            $scope.isSync = false;
        }
    }

    function formatDate(date) {
        let d;
        if (date) {
            d = date;
        } else {
            d = new Date();
        }

        d = d.getFullYear() +'/'+ addZero(d.getMonth()+1) +'/'+ addZero(d.getDate()) + ' ' + addZero(d.getHours()) + ':' + addZero(d.getMinutes()) +':'+ addZero(d.getSeconds());
        return d;

        function addZero(num) {
            if ((angular.isNumber(num) && num < 10) || (angular.isString(num) && num.length === 1)) {
                num = '0' + num;
            }
            return num;
        }
    }

    vm.importExtensions = function($event) {
        importExport.importExtension($event, {"entityType":vm.entityType, "entityId":vm.entityId, "successFunc":reloadExtensions});
    };
    vm.exportExtensions = function(widgetSourceEntityName) {
        if(vm.inWidget) {
            importExport.exportToPc(vm.extensionsJSON,  widgetSourceEntityName + '_configuration.json');
        } else {
            importExport.exportToPc(vm.extensionsJSON,  vm.entityName + '_configuration.json');
        }
    };

    /*change function for widget implementing, like vm.exportExtensions*/
    vm.exportExtension = function($event, extension) {
        if ($event) {
            $event.stopPropagation();
        }
        importExport.exportToPc(extension,  vm.entityName +'_'+ extension.id +'_configuration.json');
    };
}