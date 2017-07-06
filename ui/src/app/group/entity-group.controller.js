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
export function EntityGroupController($rootScope, utils, entityGroupService, $stateParams,
                                      $q, $translate, types) {

    var groupType = $stateParams.groupType;

    var entityGroupActionsList = [];

    var entityGroupGroupActionsList = [];

    var vm = this;

    vm.types = types;

    vm.entityGroupGridConfig = {
        deleteItemTitleFunc: deleteEntityGroupTitle,
        deleteItemContentFunc: deleteEntityGroupText,
        deleteItemsTitleFunc: deleteEntityGroupsTitle,
        deleteItemsActionTitleFunc: deleteEntityGroupsActionTitle,
        deleteItemsContentFunc: deleteEntityGroupsText,

        saveItemFunc: saveEntityGroup,

        clickItemFunc: openEntityGroup,

        getItemTitleFunc: getEntityGroupTitle,

        itemCardController: 'EntityGroupCardController',
        itemCardTemplateUrl: entityGroupCard,
        parentCtl: vm,

        actionsList: entityGroupActionsList,
        groupActionsList: entityGroupGroupActionsList,

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

    initController();

    function initController() {

        vm.entityGroupGridConfig.refreshParamsFunc = function() {
            return {"topIndex": vm.topIndex};
        };
        vm.entityGroupGridConfig.deleteItemFunc = function (entityGroupId) {
            return entityGroupService.deleteEntityGroup(entityGroupId);
        };
        vm.entityGroupGridConfig.fetchItemsFunc = function (pageLink) {
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
        };

        entityGroupActionsList.push(
            {
                onAction: function ($event, item) {
                    vm.grid.deleteItem($event, item);
                },
                name: function() { return $translate.instant('action.delete') },
                details: function() { return $translate.instant('entity-group.delete') },
                icon: "delete"
            }
        );

        entityGroupGroupActionsList.push(
            {
                onAction: function ($event) {
                    vm.grid.deleteItems($event);
                },
                name: function() { return $translate.instant('entity-group.delete-entity-groups') },
                details: deleteEntityGroupsActionTitle,
                icon: "delete"
            }
        );

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

    function saveEntityGroup(entityGroup) {
        entityGroup.type = groupType;
        return entityGroupService.saveEntityGroup(entityGroup);
    }

    function openEntityGroup($event, entityGroup) {
        if ($event) {
            $event.stopPropagation();
        }
        console.log('openEntityGroup: ' + entityGroup.id.id); //eslint-disable-line
        //$state.go('home.dashboards.dashboard', {entityGroupId: entityGroup.id.id});
    }

}
