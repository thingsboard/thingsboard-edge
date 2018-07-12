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
    vm.updateColumn = updateColumn;

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

    function updateColumn(column, updatedColumn) {
        var index = vm.columns.indexOf(column);
        vm.columns[index] = updatedColumn;
        defaultSortOrderChanged(null, updatedColumn);
    }

}