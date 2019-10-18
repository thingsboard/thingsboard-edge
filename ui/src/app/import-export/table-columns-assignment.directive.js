/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
/* eslint-disable import/no-unresolved, import/default */

import tableColumnsAssignment from './table-columns-assignment.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function TableColumnsAssignment() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            theForm: '=?',
            columns: '=',
            entityType: '=',
        },
        templateUrl: tableColumnsAssignment,
        controller: TableColumnsAssignmentController,
        controllerAs: 'vm'
    };
}

/*@ngInject*/
function TableColumnsAssignmentController($scope, types, $timeout) {
    var vm = this;

    vm.columnTypes = {};

    vm.columnTypes.name = types.importEntityColumnType.name;
    vm.columnTypes.type = types.importEntityColumnType.type;
    vm.columnTypes.label = types.importEntityColumnType.label;

    switch (vm.entityType) {
        case types.entityType.device:
            vm.columnTypes.sharedAttribute = types.importEntityColumnType.sharedAttribute;
            vm.columnTypes.serverAttribute = types.importEntityColumnType.serverAttribute;
            vm.columnTypes.timeseries = types.importEntityColumnType.timeseries;
            vm.columnTypes.accessToken = types.importEntityColumnType.accessToken;
            break;
        case types.entityType.asset:
            vm.columnTypes.serverAttribute = types.importEntityColumnType.serverAttribute;
            vm.columnTypes.timeseries = types.importEntityColumnType.timeseries;
            break;
    }

    $scope.$watch('vm.columns', function(newVal){
        if (newVal) {
            var isSelectName = false;
            var isSelectType = false;
            var isSelectLabel = false;
            var isSelectCredentials = false;
            for (var i = 0; i < newVal.length; i++) {
                switch (newVal[i].type) {
                    case types.importEntityColumnType.name.value:
                        isSelectName = true;
                        break;
                    case types.importEntityColumnType.type.value:
                        isSelectType = true;
                        break;
                    case types.importEntityColumnType.label.value:
                        isSelectLabel = true;
                        break;
                    case types.importEntityColumnType.accessToken.value:
                        isSelectCredentials = true;
                        break;
                }
            }
            if(isSelectName && isSelectType) {
                vm.theForm.$setDirty();
            } else {
                vm.theForm.$setPristine();
            }
            $timeout(function () {
                vm.columnTypes.name.disable = isSelectName;
                vm.columnTypes.type.disable = isSelectType;
                vm.columnTypes.label.disable = isSelectLabel;
                if (angular.isDefined(vm.columnTypes.accessToken)) {
                    vm.columnTypes.accessToken.disable = isSelectCredentials;
                }
            });
        }
    }, true);
}
