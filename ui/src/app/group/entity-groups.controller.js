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
/* eslint-disable import/no-unresolved, import/default */

import addEntityGroupTemplate from './add-entity-group.tpl.html';
import entityGroupCard from './entity-group-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function EntityGroupCardController() {

    var vm = this; //eslint-disable-line

}


/*@ngInject*/
export function EntityGroupsController($rootScope, $state, utils, entityGroupService, $stateParams,
                                      $q, $translate, types) {

    var groupType = $stateParams.groupType;

    var entityGroupActionsList = [
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('entity-group.details') },
            details: function() { return $translate.instant('entity-group.entity-group-details') },
            icon: "edit"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('entity-group.delete') },
            icon: "delete",
            isEnabled: function(entityGroup) {
                return !entityGroup.groupAll;
            }
        }

    ];

    var vm = this;

    vm.types = types;

    vm.entityGroupGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteEntityGroupTitle,
        deleteItemContentFunc: deleteEntityGroupText,
        deleteItemsTitleFunc: deleteEntityGroupsTitle,
        deleteItemsActionTitleFunc: deleteEntityGroupsActionTitle,
        deleteItemsContentFunc: deleteEntityGroupsText,

        fetchItemsFunc: fetchEntityGroups,
        saveItemFunc: saveEntityGroup,
        deleteItemFunc: deleteEntityGroup,

        clickItemFunc: openEntityGroup,

        getItemTitleFunc: getEntityGroupTitle,

        itemCardController: 'EntityGroupCardController',
        itemCardTemplateUrl: entityGroupCard,
        parentCtl: vm,

        actionsList: entityGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addEntityGroupTemplate,

        addItemText: function() { return $translate.instant('entity-group.add-entity-group-text') },
        noItemsText: function() { return $translate.instant('entity-group.no-entity-groups-text') },
        itemDetailsText: function() { return $translate.instant('entity-group.entity-group-details') },
        isDetailsReadOnly: function() {
            return false;
        },
        isSelectionEnabled: function(entityGroup) {
            return !entityGroup.groupAll;
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.deviceGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.deviceGridConfig.topIndex = $stateParams.topIndex;
    }

    function deleteEntityGroupTitle(entityGroup) {
        return $translate.instant('entity-group.delete-entity-group-title', {entityGroupName: entityGroup.name});
    }

    function deleteEntityGroupText() {
        return $translate.instant('entity-group.delete-entity-group-text');
    }

    function deleteEntityGroupsTitle(selectedCount) {
        return $translate.instant('entity-group.delete-entity-groups-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEntityGroupsActionTitle(selectedCount) {
        return $translate.instant('entity-group.delete-entity-groups-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEntityGroupsText () {
        return $translate.instant('entity-group.delete-entity-groups-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getEntityGroupTitle(entityGroup) {
        return entityGroup ? entityGroup.name : '';
    }

    function fetchEntityGroups(pageLink) {
        var deferred = $q.defer();
        entityGroupService.getTenantEntityGroups(groupType).then(
            function success(entityGroups) {
                utils.filterSearchTextEntities(entityGroups, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function saveEntityGroup(entityGroup) {
        var deferred = $q.defer();
        entityGroup.type = groupType;
        entityGroupService.saveEntityGroup(entityGroup).then(
            function success(entityGroup) {
                deferred.resolve(entityGroup);
                $rootScope.$broadcast(groupType+'changed');
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function deleteEntityGroup(entityGroupId) {
        var deferred = $q.defer();
        entityGroupService.deleteEntityGroup(entityGroupId).then(
            function success() {
                deferred.resolve();
                $rootScope.$broadcast(groupType+'changed');
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function openEntityGroup($event, entityGroup) {
        if ($event) {
            $event.stopPropagation();
        }
        var targetState;
        if (entityGroup.type == types.entityType.device) {
            targetState = 'home.deviceGroups.deviceGroup';
        } else if (entityGroup.type == types.entityType.asset) {
            targetState = 'home.assetGroups.assetGroup';
        }
        if (targetState) {
            $state.go(targetState, {entityGroupId: entityGroup.id.id});
        }
    }

}
