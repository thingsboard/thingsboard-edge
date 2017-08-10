/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import './entity-group.scss';

/* eslint-disable import/no-unresolved, import/default */

import addEntityTemplate from './add-entity.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/*@ngInject*/
export default function EntityGroupController($rootScope, $scope, $mdMedia, $mdDialog, $document, utils, entityGroupService, telemetryWebsocketService,
                                              $stateParams, $q, $translate, $filter, types, entityGroup) {

    var vm = this;

    vm.types = types;

    vm.entityGroup = entityGroup;
    vm.entityGroupConfig = vm.entityGroup.entityGroupConfig;
    vm.entityType = vm.entityGroup.type;
    vm.columns = vm.entityGroup.configuration.columns;
    vm.translations = vm.types.entityTypeTranslations[vm.entityType];

    vm.entityGroupConfig.onDeleteEntity = deleteEntity;
    vm.entityGroupConfig.onEntityUpdated = onEntityUpdated;
    vm.entityGroupConfig.onEntitiesUpdated = onEntitiesUpdated;

    vm.actionCellDescriptors = angular.copy(vm.entityGroupConfig.actionCellDescriptors);
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

    vm.groupActionDescriptors = angular.copy(vm.entityGroupConfig.groupActionDescriptors);
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

    vm.displayPagination = true;
    vm.defaultPageSize = 10;
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

    fetchMore();

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateEntities();
        }
    });

    $scope.$watch(function() { return $mdMedia('gt-xs'); }, function(isGtXs) {
        vm.isGtXs = isGtXs;
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

    function enterFilterMode () {
        vm.query.search = '';
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
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.currentEntity != entity) {
            vm.currentEntity = entity;
            vm.isDetailsOpen = true;
            /*var descriptors = vm.ctx.actionsApi.getActionDescriptors('rowClick');
            if (descriptors.length) {
                var entityId;
                var entityName;
                if (vm.currentEntity) {
                    entityId = vm.currentEntity.id;
                    entityName = vm.currentEntity.entityName;
                }
                vm.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName);
            }*/
        } else {
            vm.isDetailsOpen = !vm.isDetailsOpen;
        }
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
        $mdDialog.show({
            controller: AddEntityController,
            controllerAs: 'vm',
            templateUrl: addEntityTemplate,
            parent: angular.element($document[0].body),
            locals: {entityGroup: vm.entityGroup},
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
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

    function cellStyle(/*entity, column*/) {
        //TODO:
        return {};
    }

    function cellContent(entity, column) {
        //TODO:
        var content;
        var value = entity[column.key];
        if (column.useCellContentFunction && column.cellContentFunction) {
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
            }
        });
        updateEntities();
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
            subscriptionCommand: subscriptionCommand,
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
        telemetryWebsocketService.batchUnsubscribe(vm.scheduledUnsubscribe)
        vm.scheduledUnsubscribe.length = 0;
        telemetryWebsocketService.batchSubscribe(vm.scheduledSubscribe);
        vm.scheduledSubscribe.length = 0;
        telemetryWebsocketService.publishCommands();
    }

    function onData(entity, data) {
        var updated = false;
        for (var key in data) {
            var keyData = data[key];
            var value;
            if (keyData && keyData.length) {
                value = keyData[0][1];
            }
            if (!angular.equals(entity[key], value)) {
                entity[key] = value;
                updated = true;
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
