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
import './entity-group-column.scss';

import EntityGroupColumnDialogController from './entity-group-column-dialog.controller';

/* eslint-disable import/no-unresolved, import/default */

import entityGroupColumnTemplate from './entity-group-column.tpl.html';
import entityGroupColumnDialogTemplate from './entity-group-column-dialog.tpl.html';

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
            onRemoveColumn: '&',
            onUpdateColumn: '&'
        },
        controller: EntityGroupColumnController,
        controllerAs: 'vm',
        templateUrl: entityGroupColumnTemplate
    };
}

/*@ngInject*/
function EntityGroupColumnController($scope, $mdMedia, $mdDialog, $document, types) {
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
            vm.entityField.assigned_customer = types.entityGroup.entityField.assigned_customer;
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
        $mdDialog.show({
            controller: EntityGroupColumnDialogController,
            controllerAs: 'vm',
            templateUrl: entityGroupColumnDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                readOnly: vm.readOnly,
                column: angular.copy(vm.column),
                entityType: vm.entityType,
                columnTypes: vm.columnTypes,
                entityField: vm.entityField
            },
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (column) {
            if (vm.onUpdateColumn) {
                vm.onUpdateColumn({updatedColumn: column});
            }
        });

    }
}