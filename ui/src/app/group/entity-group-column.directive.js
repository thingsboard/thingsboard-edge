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

import './entity-group-column.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityGroupColumnTemplate from './entity-group-column.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityGroupColumn() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            column: '=',
            entityType: '=',
            readOnly: '=',
            onDefaultSortOrderChanged: '&',
            onRemoveColumn: '&'
        },
        controller: EntityGroupColumnController,
        controllerAs: 'vm',
        templateUrl: entityGroupColumnTemplate
    };
}

/*@ngInject*/
function EntityGroupColumnController($element, $scope, $filter, $mdMedia, $translate, utils, types) {
    var vm = this;
    vm.columnTypes = {};
    vm.entityField = {};
    vm.sortOrder = types.entityGroup.sortOrder;

    switch(vm.entityType) {
        case types.entityType.user:
        case types.entityType.customer:
        case types.entityType.asset:
            vm.columnTypes.serverAttribute = types.entityGroup.columnType.serverAttribute;
            vm.columnTypes.timeseries = types.entityGroup.columnType.timeseries;
            vm.columnTypes.entityField = types.entityGroup.columnType.entityField;
            break;
        case types.entityType.device:
            vm.columnTypes = types.entityGroup.columnType;
            break;
    }

    vm.entityField.created_time = types.entityGroup.entityField.created_time;
    vm.entityField.name = types.entityGroup.entityField.name;

    switch(vm.entityType) {
        case types.entityType.user:
            vm.entityField.email = types.entityGroup.entityField.email;
            vm.entityField.authority = types.entityGroup.entityField.authority;
            vm.entityField.first_name = types.entityGroup.entityField.first_name;
            vm.entityField.last_name = types.entityGroup.entityField.last_name;
            break;
        case types.entityType.customer:
            vm.entityField.title = types.entityGroup.entityField.title;
            vm.entityField.email = types.entityGroup.entityField.email;
            vm.entityField.country = types.entityGroup.entityField.country;
            vm.entityField.state = types.entityGroup.entityField.state;
            vm.entityField.city = types.entityGroup.entityField.city;
            vm.entityField.address = types.entityGroup.entityField.address;
            vm.entityField.address2 = types.entityGroup.entityField.address2;
            vm.entityField.zip = types.entityGroup.entityField.zip;
            vm.entityField.phone = types.entityGroup.entityField.phone;
            break;
        case types.entityType.asset:
        case types.entityType.device:
            vm.entityField.type = types.entityGroup.entityField.type;
            break;
    }

    $scope.$watch(function() { return $mdMedia('gt-xs'); }, function(isGtXs) {
        vm.isGtXs = isGtXs;
    });

    vm.openColumn = openColumn;

    function openColumn($event) {
        if ($event) {
            $event.stopPropagation();
        }
    }
}