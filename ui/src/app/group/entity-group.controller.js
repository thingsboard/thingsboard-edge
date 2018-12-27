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
import './entity-group.scss';

/* eslint-disable import/no-unresolved, import/default */

import addEntityTemplate from './add-entity.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/*@ngInject*/
export default function EntityGroupController($rootScope, $scope, $state, $injector, $mdMedia, $mdDialog, $window, $document, $timeout, utils,
                                              tbDialogs, entityGroupService, telemetryWebsocketService,
                                              $stateParams, $q, $translate, $filter, types, securityTypes, userPermissionsService, entityGroup) {

    var vm = this;

    vm.types = types;

    vm.customerId = $stateParams.customerId;
    vm.entityGroup = entityGroup;
    vm.entityType = vm.entityGroup.type;
    vm.translations = vm.types.entityTypeTranslations[vm.entityType];

    vm.toggleGroupDetails = toggleGroupDetails;
    vm.onToggleGroupEditMode = onToggleGroupEditMode;
    vm.onCloseGroupDetails = onCloseGroupDetails;
    vm.operatingGroup = operatingGroup;
    vm.saveGroup = saveGroup;
    vm.deleteEntityGroup = deleteEntityGroup;
    vm.addEnabled = addEnabled;
    vm.fetchMore = fetchMore;
    vm.addEntity = addEntity;
    vm.getColumnTitle = getColumnTitle;
    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.onRowClick = onRowClick;

    vm.isCurrent = isCurrent;

    vm.cellStyle = cellStyle;
    vm.cellContent = cellContent;

    vm.onEntityUpdated = onEntityUpdated;

    reloadGroupConfiguration();

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateEntities();
        }
    });

    $scope.$watch(function() { return $mdMedia('gt-xs'); }, function(isGtXs) {
        vm.isGtXs = isGtXs;
    });

    $scope.$watch(function() { return $mdMedia('gt-sm'); }, function(isGtSm) {
        vm.isGtSm = isGtSm;
    });

    $scope.$watch(function() { return $mdMedia('gt-md'); }, function(isGtMd) {
        vm.isGtMd = isGtMd;
        if (vm.isGtMd) {
            vm.limitOptions = [vm.defaultPageSize, vm.defaultPageSize*2, vm.defaultPageSize*3];
        } else {
            vm.limitOptions = null;
        }
    });

    $scope.$on('$destroy', function() {
        clearSubscriptions();
    });

    function reloadGroupConfiguration() {
        clearSubscriptions();

        vm.currentEntityGroup = vm.entityGroup.origEntityGroup;
        vm.isGroupDetailsReadOnly = !userPermissionsService.hasEntityGroupPermission(securityTypes.operation.write, vm.entityGroup);

        vm.entityGroupConfig = vm.entityGroup.entityGroupConfig;
        vm.columns = vm.entityGroup.configuration.columns.filter((column) => {
            if (column.type == types.entityGroup.columnType.timeseries.value) {
                return userPermissionsService.hasGroupEntityPermission(securityTypes.operation.readTelemetry, vm.entityGroup);
            } else if (column.type == types.entityGroup.columnType.clientAttribute.value ||
                column.type == types.entityGroup.columnType.sharedAttribute.value ||
                column.type == types.entityGroup.columnType.serverAttribute.value) {
                return userPermissionsService.hasGroupEntityPermission(securityTypes.operation.readAttributes, vm.entityGroup);
            } else {
                return true;
            }
        });

        vm.columns.forEach((column) => {
            if (column.useCellStyleFunction && column.cellStyleFunction && column.cellStyleFunction.length) {
                try {
                    column.cellStyleFunction = new Function('value, entity', column.cellStyleFunction);
                } catch (e) {
                    delete column.cellStyleFunction;
                }
            } else {
                delete column.cellStyleFunction;
            }
            if (column.useCellContentFunction && column.cellContentFunction && column.cellContentFunction.length) {
                try {
                    column.cellContentFunction = new Function('value, entity, $filter', column.cellContentFunction);
                } catch (e) {
                    delete column.cellContentFunction;
                }
            } else {
                delete column.cellContentFunction;
            }
        });

        vm.entityGroupConfig.onDeleteEntity = deleteEntity;
        vm.entityGroupConfig.onEntityUpdated = onEntityUpdated;
        vm.entityGroupConfig.onEntitiesUpdated = onEntitiesUpdated;

        vm.settings = utils.groupSettingsDefaults(vm.entityType, vm.entityGroup.configuration.settings);
        if (vm.settings.groupTableTitle && vm.settings.groupTableTitle.length) {
            vm.tableTitle = utils.customTranslation(vm.settings.groupTableTitle, vm.settings.groupTableTitle);
            vm.entitiesTitle = '';
        } else {
            vm.tableTitle = utils.customTranslation(vm.entityGroup.name, vm.entityGroup.name);
            vm.entitiesTitle = ': ' + $translate.instant(types.entityTypeTranslations[vm.entityType].typePlural);
        }

        vm.actionDescriptorsBySourceId = {};
        if (vm.entityGroup.configuration.actions) {
            for (var actionSourceId in vm.entityGroup.configuration.actions) {
                var descriptors = vm.entityGroup.configuration.actions[actionSourceId];
                var actionDescriptors = [];
                descriptors.forEach(function(descriptor) {
                    var actionDescriptor = angular.copy(descriptor);
                    actionDescriptor.displayName = utils.customTranslation(descriptor.name, descriptor.name);
                    actionDescriptors.push(actionDescriptor);
                });
                vm.actionDescriptorsBySourceId[actionSourceId] = actionDescriptors;
            }
        }

        var actionCellButtonDescriptors = vm.actionDescriptorsBySourceId['actionCellButton'];
        vm.actionCellDescriptors = [];
        if (actionCellButtonDescriptors) {
            actionCellButtonDescriptors.forEach((descriptor) => {
                vm.actionCellDescriptors.push(
                    {
                        name: descriptor.displayName,
                        icon: descriptor.icon,
                        isEnabled: () => {
                            return true;
                        },
                        onAction: ($event, entity) => {
                            handleDescriptorAction($event, entity, descriptor);
                        }
                    }
                );
            });
        }

        vm.entityGroupConfig.actionCellDescriptors.forEach((descriptor) => {
            vm.actionCellDescriptors.push(descriptor);
        });
        if (vm.settings.detailsMode == types.entityGroup.detailsMode.onActionButtonClick.value) {
            if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.read, vm.entityGroup)) {
                vm.actionCellDescriptors.push(
                    {
                        name: $translate.instant(vm.translations.details),
                        icon: 'edit',
                        isEnabled: () => {
                            return true;
                        },
                        onAction: ($event, entity) => {
                            toggleEntityDetails($event, entity);
                        }
                    }
                );
            }
        }
        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.delete, vm.entityGroup)) {
            vm.actionCellDescriptors.push(
                {
                    name: $translate.instant('action.delete'),
                    icon: 'delete',
                    isEnabled: (entity) => {
                        return vm.entityGroupConfig.deleteEnabled(entity);
                    },
                    onAction: ($event, entity) => {
                        deleteEntity($event, entity);
                    }
                }
            );
        }

        vm.groupActionDescriptors = angular.copy(vm.entityGroupConfig.groupActionDescriptors);

        if (userPermissionsService.hasGenericEntityGroupPermission(securityTypes.operation.addToGroup, vm.entityGroup) &&
            userPermissionsService.isOwnedGroup(vm.entityGroup) &&
            userPermissionsService.hasGenericEntityGroupPermission(securityTypes.operation.read, vm.entityGroup)) {
            vm.groupActionDescriptors.push(
                {
                    name: $translate.instant('entity-group.add-to-group'),
                    icon: 'add_circle',
                    isEnabled: () => {
                        return vm.settings.enableGroupTransfer;
                    },
                    onAction: ($event, entities) => {
                        addEntitiesToEntityGroup($event, entities);
                    }
                }
            );
        }

        if (userPermissionsService.hasEntityGroupPermission(securityTypes.operation.removeFromGroup, vm.entityGroup)) {
            if (userPermissionsService.hasGenericEntityGroupPermission(securityTypes.operation.read, vm.entityGroup)) {
                vm.groupActionDescriptors.push(
                    {
                        name: $translate.instant('entity-group.move-to-group'),
                        icon: 'swap_vertical_circle',
                        isEnabled: () => {
                            return vm.settings.enableGroupTransfer && !vm.entityGroup.groupAll;
                        },
                        onAction: ($event, entities) => {
                            moveEntitiesToEntityGroup($event, entities);
                        }
                    }
                );
            }
            vm.groupActionDescriptors.push(
                {
                    name: $translate.instant('entity-group.remove-from-group'),
                    icon: 'remove_circle',
                    isEnabled: () => {
                        return vm.settings.enableGroupTransfer && !vm.entityGroup.groupAll;
                    },
                    onAction: ($event, entities) => {
                        removeEntitiesFromEntityGroup($event, entities);
                    }
                }
            );
        }

        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.delete, vm.entityGroup)) {
            vm.groupActionDescriptors.push(
                {
                    name: $translate.instant('action.delete'),
                    icon: 'delete',
                    isEnabled: () => {
                        return vm.entityGroupConfig.entitiesDeleteEnabled();
                    },
                    onAction: ($event, entities) => {
                        deleteEntities($event, entities);
                    }
                }
            );
        }

        var tsKeysList = [];
        var attrKeysList = [];

        function addIfAbsent(list, key) {
            var index = list.indexOf(key);
            if (index == -1) {
                list.push(key);
            }
        }

        vm.columns.forEach(function(column) {
            var key = column.key;
            if (column.type == types.entityGroup.columnType.timeseries.value) {
                addIfAbsent(tsKeysList, key);
            } else if (column.type == types.entityGroup.columnType.clientAttribute.value ||
                column.type == types.entityGroup.columnType.sharedAttribute.value ||
                column.type == types.entityGroup.columnType.serverAttribute.value) {
                addIfAbsent(attrKeysList, key);
            }
        });

        vm.tsKeys = tsKeysList.join(',');
        vm.attrKeys = attrKeysList.join(',');
        vm.hasTsData = vm.tsKeys.length > 0;
        vm.hasAttrData = vm.attrKeys.length > 0;
        vm.hasSubscriptionData = vm.hasTsData || vm.hasAttrData;

        vm.entitySubscriptions = {};

        vm.scheduledSubscribe = [];
        vm.scheduledUnsubscribe = [];

        vm.showData = true;
        vm.hasData = false;

        vm.entities = [];
        vm.selectedEntities = [];
        vm.entitiesCount = 0;

        vm.allEntities = [];

        vm.currentEntity = null;
        vm.isDetailsOpen = false;

        vm.displayPagination = vm.settings.displayPagination;
        vm.defaultPageSize = vm.settings.defaultPageSize;
        vm.defaultSortOrder = '-' + types.entityGroup.entityField.created_time.value;

        for (var i=0;i<vm.columns.length;i++) {
            var column = vm.columns[i];
            if (column.sortOrder && column.sortOrder !== types.entityGroup.sortOrder.none.value) {
                if (column.sortOrder == types.entityGroup.sortOrder.desc.value) {
                    vm.defaultSortOrder = '-' + column.key;
                } else {
                    vm.defaultSortOrder = column.key;
                }
                break;
            }
        }

        vm.query = {
            order: vm.defaultSortOrder,
            limit: vm.defaultPageSize,
            page: 1,
            search: null
        };

        vm.pageLink = {
            limit: 100
        };

        vm.hasNext = true;

        fetchMore();
    }

    function addEnabled() {
        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.create, vm.entityGroup) &&
            userPermissionsService.hasEntityGroupPermission(securityTypes.operation.addToGroup, vm.entityGroup)) {
            return vm.entityGroupConfig.addEnabled();
        } else {
            return false;
        }
    }

    function enterFilterMode (event) {
        let $button = angular.element(event.currentTarget);
        let $toolbarsContainer = $button.closest('.toolbarsContainer');

        vm.query.search = '';

        $timeout(()=>{
            $toolbarsContainer.find('.searchInput').focus();
        })
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateEntities();
    }

    function onReorder () {
        updateEntities();
    }

    function onPaginate () {
        updateEntities();
    }

    function onRowClick($event, entity) {
        if (vm.settings.detailsMode == types.entityGroup.detailsMode.onRowClick.value) {
            if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.read, vm.entityGroup)) {
                toggleEntityDetails($event, entity);
            }
        } else {
            var descriptors = vm.actionDescriptorsBySourceId['rowClick'];
            if (descriptors && descriptors.length) {
                var descriptor = descriptors[0];
                handleDescriptorAction($event, entity, descriptor);
            }
        }
    }

    function toggleEntityDetails($event, entity) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.currentEntity != entity) {
            vm.currentEntity = entity;
            vm.isDetailsOpen = true;
            vm.isGroupDetailsOpen = false;
        } else {
            vm.isDetailsOpen = !vm.isDetailsOpen;
            if (vm.isDetailsOpen) {
                vm.isGroupDetailsOpen = false;
            }
        }
    }

    function toggleGroupDetails($event) {
        if ($event) {
            $event.stopPropagation();
        }
        if (!vm.isGroupDetailsOpen) {
            //vm.currentEntityGroup = vm.entityGroup.origEntityGroup;
            vm.isGroupDetailsOpen = true;
            vm.isDetailsOpen = false;
            vm.isGroupDetailsEdit = false;
        } else {
            vm.isGroupDetailsOpen = false;
        }
    }

    function onToggleGroupEditMode(theForm) {
        if (!vm.isGroupDetailsEdit) {
            theForm.$setPristine();
        }
    }

    function onCloseGroupDetails() {
        //vm.currentEntityGroup = null;
    }

    function operatingGroup() {
        if (!vm.isGroupDetailsEdit) {
            if (vm.editingEntityGroup) {
                vm.editingEntityGroup = null;
            }
            return vm.currentEntityGroup;
        } else {
            if (!vm.editingEntityGroup) {
                vm.editingEntityGroup = angular.copy(vm.currentEntityGroup);
            }
            return vm.editingEntityGroup;
        }
    }

    function saveGroup(theForm) {
        entityGroupService.saveEntityGroup(vm.editingEntityGroup).then(
            (entityGroup) => {
                theForm.$setPristine();
                vm.isGroupDetailsEdit = false;
                if (!vm.customerId) {
                    $rootScope.$broadcast(entityGroup.type + 'changed');
                }
                entityGroupService.constructGroupConfig($stateParams, entityGroup, vm.entityGroup.entityGroupConfigFactory).then(
                    (entityGroup) => {
                        vm.entityGroup = entityGroup;
                        reloadGroupConfiguration();
                    }
                );
            }
        );
    }

    function deleteEntityGroup($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('entity-group.delete-entity-group-title', {entityGroupName: vm.currentEntityGroup.name}))
            .htmlContent($translate.instant('entity-group.delete-entity-group-text'))
            .ariaLabel($translate.instant('grid.delete-item'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
                entityGroupService.deleteEntityGroup(vm.currentEntityGroup.id.id).then(
                    function success() {
                        if (!vm.customerId) {
                            $rootScope.$broadcast(vm.currentEntityGroup.type + 'changed');
                        }
                        $window.history.back();
                    }
                );
            },
            function () {
            });
    }

    function isCurrent(entity) {
        return (vm.currentEntity && entity && vm.currentEntity.id && entity.id) &&
            (vm.currentEntity.id.id === entity.id.id);
    }

    function fetchMore() {
        entityGroupService.getEntityGroupEntities(vm.entityGroup.id.id, vm.pageLink).then(
            function success(entities) {
                vm.allEntities = vm.allEntities.concat(entities.data);
                vm.hasNext = entities.hasNext;
                vm.pageLink = entities.nextPageLink;
                updateEntities();
            },
            function fail() {}
        );
    }

    function addEntity($event) {
        var addPromise;
        if (vm.entityGroupConfig.addEntity) {
            addPromise = vm.entityGroupConfig.addEntity($event, vm.entityGroup);
        } else {
            addPromise = $mdDialog.show({
                controller: AddEntityController,
                controllerAs: 'vm',
                templateUrl: addEntityTemplate,
                parent: angular.element($document[0].body),
                locals: {entityGroup: vm.entityGroup},
                fullscreen: true,
                targetEvent: $event
            });
        }
        addPromise.then(function () {
            entityAdded();
        }, function () {
        });
    }

    function entityAdded() {
        var pageLink = {
            limit: 100
        };
        if (vm.allEntities.length) {
            var firstEntity = vm.allEntities[0];
            pageLink.idOffset = firstEntity.id.id;
        }
        entityGroupService.getEntityGroupEntities(vm.entityGroup.id.id, pageLink, true).then(
            function success(entities) {
                var latestEntities = entities.data.reverse();
                vm.allEntities = latestEntities.concat(vm.allEntities);
                updateEntities();
        },
        function fail() {}
        );
    }

    function updateEntities() {
        vm.isDetailsOpen = false;
        var result = $filter('orderBy')(vm.allEntities, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.entitiesCount = result.length;

        if (vm.displayPagination) {
            var startIndex = vm.query.limit * (vm.query.page - 1);
            vm.entities = result.slice(startIndex, startIndex + vm.query.limit);
        } else {
            vm.entities = result;
        }
        updateSubscriptions();
    }

    function getColumnTitle(column) {
        if (column.title && column.title.length) {
            return column.title;
        } else {
            if (column.type == types.entityGroup.columnType.entityField.value) {
                var entityField = types.entityGroup.entityField[column.key];
                return $translate.instant(entityField.name);
            } else {
                return column.key;
            }
        }
    }

    function cellStyle(entity, column) {
        var style;
        if (column.cellStyleFunction) {
            var value = entity[column.key];
            try {
                style = column.cellStyleFunction(value, entity);
            } catch (e) {
                style = {};
            }
        } else {
            style = {};
        }
        return style;
    }

    function cellContent(entity, column) {
        var content;
        var value = entity[column.key];
        value = utils.customTranslation(value, value);
        if (column.cellContentFunction) {
            var strContent = '';
            if (angular.isDefined(value)) {
                strContent = '' + value;
            }
            content = strContent;
            try {
                content = column.cellContentFunction(value, entity, $filter);
            } catch (e) {
                content = strContent;
            }
        } else {
            content = defaultContent(column, value);
        }
        return content;
    }

    function defaultContent(column, value) {
        if (angular.isDefined(value)) {
            if (column.type == types.entityGroup.columnType.entityField.value) {
                var entityField = types.entityGroup.entityField[column.key];
                if (entityField.time) {
                    return $filter('date')(value, 'yyyy-MM-dd HH:mm:ss');
                }
            }
            return value;
        } else {
            return '';
        }
    }

    function onEntityUpdated(entityId, reloadDetails) {
        entityGroupService.getEntityGroupEntity(vm.entityGroup.id.id, entityId).then(
            function success(entity) {
                var result = $filter('filter')(vm.allEntities, { id: { id: entity.id.id }});
                if (result && result.length) {
                    var prevEntity = result[0];
                    var index = vm.allEntities.indexOf(prevEntity);
                    if (index > -1) {
                        vm.allEntities[index] = entity;
                    }
                    index = vm.entities.indexOf(prevEntity);
                    if (index > -1) {
                        vm.entities[index] = entity;
                        if (vm.hasSubscriptionData) {
                            removeSubscription(entity.id.id);
                            createSubscription(entity);
                            commitSubscriptions();
                        }
                    }
                }
            }
        );
        if (reloadDetails) {
            $scope.$broadcast('reloadEntityDetails');
        }
    }

    function onEntitiesUpdated(entityIds, reloadDetails) {
        entityIds.forEach((entityId) => {
            onEntityUpdated(entityId, false);
        });
        if (reloadDetails) {
            $scope.$broadcast('reloadEntityDetails');
        }
        vm.selectedEntities.length = 0;
    }

    function onEntitiesDeleted(entityIds) {
        entityIds.forEach((entityId) => {
            var result = $filter('filter')(vm.allEntities, { id: { id: entityId }});
            if (result && result.length) {
                var prevEntity = result[0];
                var index = vm.allEntities.indexOf(prevEntity);
                if (index > -1) {
                    vm.allEntities.splice(index, 1);
                }
                index = vm.entities.indexOf(prevEntity);
                if (index > -1) {
                    vm.entities.splice(index, 1);
                }
                index = vm.selectedEntities.indexOf(prevEntity);
                if (index > -1) {
                    vm.selectedEntities.splice(index, 1);
                }
            }
        });
        updateEntities();
    }

    function handleDescriptorAction($event, entity, descriptor) {
        if ($event) {
            $event.stopPropagation();
        }
        var entityId = entity.id;
        var entityName = entity.name;
        var type = descriptor.type;
        var targetEntityParamName = descriptor.stateEntityParamName;
        var targetEntityId;
        if (descriptor.setEntityId) {
            targetEntityId = entityId;
        }
        switch (type) {
            case types.entityGroupActionTypes.openDashboard.value:
                var targetDashboardId = descriptor.targetDashboardId;
                var targetDashboardStateId = descriptor.targetDashboardStateId;
                var stateObject = {};
                stateObject.params = {};
                updateEntityParams(stateObject.params, targetEntityParamName, targetEntityId, entityName);
                if (targetDashboardStateId) {
                    stateObject.id = targetDashboardStateId;
                }
                var stateParams = {
                    dashboardId: targetDashboardId,
                    state: utils.objToBase64([ stateObject ])
                }
                $state.go('home.dashboard', stateParams);
                break;
            case types.widgetActionTypes.custom.value:
                var customFunction = descriptor.customFunction;
                if (angular.isDefined(customFunction) && customFunction.length > 0) {
                    try {
                        var customActionFunction = new Function('$event', '$injector', 'entityId', 'entityName', customFunction);
                        customActionFunction($event, $injector, entityId, entityName);
                    } catch (e) {
                        //
                    }
                }
                break;
        }
    }

    function updateEntityParams(params, targetEntityParamName, targetEntityId, entityName) {
        if (targetEntityId) {
            var targetEntityParams;
            if (targetEntityParamName && targetEntityParamName.length) {
                targetEntityParams = params[targetEntityParamName];
                if (!targetEntityParams) {
                    targetEntityParams = {};
                    params[targetEntityParamName] = targetEntityParams;
                }
            } else {
                targetEntityParams = params;
            }
            targetEntityParams.entityId = targetEntityId;
            if (entityName) {
                targetEntityParams.entityName = entityName;
            }
        }
    }

    function deleteEntity($event, entity) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(vm.entityGroupConfig.deleteEntityTitle(entity))
            .htmlContent(vm.entityGroupConfig.deleteEntityContent(entity))
            .ariaLabel(vm.entityGroupConfig.deleteEntityTitle(entity))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
                vm.entityGroupConfig.deleteEntity(entity.id.id).then(function success() {
                    onEntitiesDeleted([entity.id.id]);
                });
            },
            function () {
            });

    }

    function deleteEntities($event, entities) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(vm.entityGroupConfig.deleteEntitiesTitle(entities.length))
            .htmlContent(vm.entityGroupConfig.deleteEntitiesContent(entities.length))
            .ariaLabel(vm.entityGroupConfig.deleteEntitiesTitle(entities.length))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
                var tasks = [];
                var entityIds = [];
                entities.forEach((entity) => {
                    if (vm.entityGroupConfig.deleteEnabled(entity)) {
                        tasks.push(vm.entityGroupConfig.deleteEntity(entity.id.id));
                        entityIds.push(entity.id.id);
                    }
                });
                $q.all(tasks).then(function () {
                    onEntitiesDeleted(entityIds);
                });
            },
            function () {}
        );
    }

    function addEntitiesToEntityGroup($event, entities) {
        var onEntityGroupSelected = (targetEntityGroupId) => {
            var entityIds = [];
            entities.forEach((entity) => {
                entityIds.push(entity.id.id);
            });
            return entityGroupService.addEntitiesToEntityGroup(targetEntityGroupId, entityIds);
        };
        var ownerId = userPermissionsService.getUserOwnerId();
        if (vm.customerId) {
            ownerId = {
                id: vm.customerId,
                entityType: types.entityType.customer
            }
        }
        tbDialogs.selectEntityGroup($event, ownerId,  vm.entityType,
            'entity-group.add-to-group', 'action.add',
            vm.translations.selectGroupToAdd,
            'entity-group.no-entity-groups-matching',
            'entity-group.target-entity-group-required', onEntityGroupSelected, [vm.entityGroup.id.id]).then(
            () => { vm.selectedEntities.length = 0; }
        );
    }

    function moveEntitiesToEntityGroup($event, entities) {
        var entityIds = [];
        entities.forEach((entity) => {
            entityIds.push(entity.id.id);
        });
        var onEntityGroupSelected = (targetEntityGroupId) => {
            var tasks = [];
            tasks.push(entityGroupService.removeEntitiesFromEntityGroup(vm.entityGroup.id.id, entityIds));
            tasks.push(entityGroupService.addEntitiesToEntityGroup(targetEntityGroupId, entityIds));
            return $q.all(tasks);
        };
        var ownerId = userPermissionsService.getUserOwnerId();
        if (vm.customerId) {
            ownerId = {
                id: vm.customerId,
                entityType: types.entityType.customer
            }
        }
        tbDialogs.selectEntityGroup($event, ownerId, vm.entityType,
            'entity-group.move-to-group', 'action.move',
            vm.translations.selectGroupToMove,
            'entity-group.no-entity-groups-matching',
            'entity-group.target-entity-group-required', onEntityGroupSelected, [vm.entityGroup.id.id]).then(
            () => { onEntitiesDeleted(entityIds); }
        );
    }

    function removeEntitiesFromEntityGroup($event, entities) {
        var title = $translate.instant('entity-group.remove-from-group');
        var content = $translate.instant(vm.translations.removeFromGroup, {count: entities.length, entityGroup: vm.entityGroup.name}, 'messageformat');
        tbDialogs.confirm($event, title, content, title).then( () => {
            var entityIds = [];
            entities.forEach((entity) => {
                entityIds.push(entity.id.id);
            });
            entityGroupService.removeEntitiesFromEntityGroup(vm.entityGroup.id.id, entityIds).then(() => {
                onEntitiesDeleted(entityIds);
            });
        });
    }


    function updateSubscriptions() {
        if (vm.tsKeys.length || vm.attrKeys.length) {
            var entityIds = [];
            for (var i=0;i<vm.entities.length;i++) {
                var entity = vm.entities[i];
                updateSubscription(entity);
                entityIds.push(entity.id.id);
            }
        }
        for (var entityId in vm.entitySubscriptions) {
            if (entityIds.indexOf(entityId) == -1) {
                removeSubscription(entityId);
            }
        }
        commitSubscriptions();
    }

    function updateSubscription(entity) {
        var entityId = entity.id.id;
        var entitySubscription = vm.entitySubscriptions[entityId];
        if (!entitySubscription) {
            createSubscription(entity);
        }
    }

    function createSubscription(entity) {
        var entitySubscription = {};
        if (vm.hasTsData) {
            entitySubscription.tsSubscriber = createSubscriber(entity, true);
        }
        if (vm.hasAttrData) {
            entitySubscription.attrSubscriber = createSubscriber(entity, false);
        }
        vm.entitySubscriptions[entity.id.id] = entitySubscription;
    }

    function createSubscriber(entity, tsElseAttr) {
        var subscriptionCommand = {
            entityType: vm.entityGroup.type,
            entityId: entity.id.id,
            keys: tsElseAttr ? vm.tsKeys : vm.attrKeys
        };
        var subscriber = {
            subscriptionCommands: [ subscriptionCommand ],
            type: tsElseAttr ? types.dataKeyType.timeseries : types.dataKeyType.attribute,
            onData: function(data) {
                if (data.data) {
                    onData(entity, data.data);
                }
            }
        }
        vm.scheduledSubscribe.push(subscriber);
        return subscriber;
    }

    function removeSubscription(entityId) {
        var entitySubscription = vm.entitySubscriptions[entityId];
        if (entitySubscription) {
            if (entitySubscription.tsSubscriber) {
                vm.scheduledUnsubscribe.push(entitySubscription.tsSubscriber);
            }
            if (entitySubscription.attrSubscriber) {
                vm.scheduledUnsubscribe.push(entitySubscription.attrSubscriber);
            }
            delete vm.entitySubscriptions[entityId];
        }
    }

    function clearSubscriptions() {
        for (var entityId in vm.entitySubscriptions) {
            removeSubscription(entityId);
        }
        commitSubscriptions();
    }

    function commitSubscriptions() {
        if (vm.scheduledUnsubscribe) {
            telemetryWebsocketService.batchUnsubscribe(vm.scheduledUnsubscribe);
            vm.scheduledUnsubscribe.length = 0;
        }
        if (vm.scheduledSubscribe) {
            telemetryWebsocketService.batchSubscribe(vm.scheduledSubscribe);
            vm.scheduledSubscribe.length = 0;
        }
        telemetryWebsocketService.publishCommands();
    }

    function onData(entity, data) {
        var updated = false;
        for (var key in data) {
            var keyData = data[key];
            if (keyData && keyData.length) {
                var value = keyData[0][1];
                if (!angular.equals(entity[key], value)) {
                    entity[key] = value;
                    updated = true;
                }
            }
        }
        if (updated) {
            $scope.$digest();
        }
    }
}

/*@ngInject*/
function AddEntityController($scope, $mdDialog, types, helpLinks, entityService, entityGroupService, entityGroup) {

    var vm = this;

    vm.helpLinks = helpLinks;
    vm.entityGroup = entityGroup;
    vm.entityType = entityGroup.type;
    vm.entity = {
        id: {
            entityType: vm.entityType
        }
    };

    vm.translations = types.entityTypeTranslations[vm.entityType];
    vm.resources = types.entityTypeResources[vm.entityType];

    vm.entityHelpLinkId = entityHelpLinkId;
    vm.add = add;
    vm.cancel = cancel;

    function entityHelpLinkId() {
        if (vm.entityType == types.entityType.plugin) {
            return vm.helpLinks.getPluginLink(vm.entity);
        } else {
            return vm.resources.helpId;
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        if (vm.entityGroup.ownerId.entityType === types.entityType.customer) {
            if (vm.entityType === types.entityType.customer) {
                vm.entity.parentCustomerId = vm.entityGroup.ownerId;
            } else {
                vm.entity.customerId = vm.entityGroup.ownerId;
            }
        }
        entityService.saveEntity(vm.entity).then(function success(entity) {
            vm.entity = entity;
            $scope.theForm.$setPristine();
            if (!vm.entityGroup.groupAll) {
                entityGroupService.addEntityToEntityGroup(vm.entityGroup.id.id, entity.id.id).then(
                    function success() {
                        $mdDialog.hide();
                    }
                );
            } else {
                $mdDialog.hide();
            }
        });
    }
}
