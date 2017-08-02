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

import './entity-group-columns.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityGroupColumnsTemplate from './entity-group-columns.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityGroupColumns() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            columns: '=',
            entityType: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '='
        },
        controller: EntityGroupColumnsController,
        controllerAs: 'vm',
        templateUrl: entityGroupColumnsTemplate
    };
}

/*@ngInject*/
function EntityGroupColumnsController($element, $scope, $mdMedia, $filter, $translate, utils, types) {

    var vm = this;

    vm.addColumn = addColumn;
    vm.defaultSortOrderChanged = defaultSortOrderChanged;
    vm.removeColumn = removeColumn;

    $scope.$watch(function() { return $mdMedia('gt-xs'); }, function(isGtXs) {
        vm.isGtXs = isGtXs;
    });

    $scope.$watch('vm.columns', function(newVal, prevVal)  {
        if (vm.isEdit && !angular.equals(newVal, prevVal)) {
            vm.theForm.$setDirty();
        }
    }, true);

    function addColumn($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var column = {
            type: types.entityGroup.columnType.entityField.value,
            key: types.entityGroup.entityField.name.value,
            sortOrder: types.entityGroup.sortOrder.none.value,
            mobileHide: false
        };
        vm.columns.push(column);
    }

    function defaultSortOrderChanged($event, column) {
        var index = vm.columns.indexOf(column);
        if (index > -1 && column.sortOrder != types.entityGroup.sortOrder.none.value) {
            for (var i=0;i<vm.columns.length;i++) {
                if (i != index) {
                    vm.columns[i].sortOrder = types.entityGroup.sortOrder.none.value;
                }
            }
        }
    }

    function removeColumn($event, column) {
        if ($event) {
            $event.stopPropagation();
        }
        var index = vm.columns.indexOf(column);
        if (index > -1) {
            vm.columns.splice(index, 1);
        }
    }

}