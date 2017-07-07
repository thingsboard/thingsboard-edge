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

/*@ngInject*/
export default function EntityGroupController($rootScope, $scope, $mdMedia, utils, entityGroupService, $stateParams,
                                       $q, $translate, types, entityGroup) {

    var vm = this; //eslint-disable-line

    vm.types = types;

    vm.entityGroup = entityGroup;

    vm.columns = vm.entityGroup.configuration.columns;

    //TODO:
    vm.actionCellDescriptors = [];

    vm.showData = true;
    vm.hasData = false;

    vm.entities = [];
    vm.selectedEntities = [];
    vm.entitiesCount = 0;

    vm.allEntities = null;

    vm.currentEntity = null;

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

    vm.fetchMore = fetchMore;
    vm.getColumnTitle = getColumnTitle;
    vm.cellStyle = cellStyle;
    vm.cellContent = cellContent;

    fetchMore();

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

    function fetchMore() {

    }

    function getColumnTitle(column) {
        //TODO:
        return column.key;
    }

    function cellStyle(/*entity, column*/) {
        //TODO:
        return {};
    }

    function cellContent(entity, column) {
        //TODO:
        return entity[column.key];
    }

}
