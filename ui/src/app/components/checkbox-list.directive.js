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
import './checkbox-list.scss';

/* eslint-disable import/no-unresolved, import/default */

import checkboxListTemplate from './checkbox-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.checkboxList', [])
    .directive('tbCheckboxList', CheckboxList)
    .name;

/*@ngInject*/
function CheckboxList() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            disabled:'=ngDisabled',
            titleText: '@?',
            namePlaceholderText: '@?',
            noDataText: '@?',
            checkboxList: '='
        },
        controller: CheckboxController,
        controllerAs: 'vm',
        templateUrl: checkboxListTemplate
    };
}

/*@ngInject*/
function CheckboxController($scope, $mdUtil) {

    let vm = this;

    vm.cbList = [];

    vm.removeCheckbox = removeCheckbox;
    vm.addCheckbox = addCheckbox;

    $scope.$watch('vm.checkboxList', () => {
        stopWatchCbList();
        vm.cbList.length = 0;
        if (vm.checkboxList) {
            for (var property in vm.checkboxList) {
                if (vm.checkboxList.hasOwnProperty(property)) {
                    vm.cbList.push(
                        {
                            name: property + '',
                            checked: vm.checkboxList[property]
                        }
                    );
                }
            }
        }
        $mdUtil.nextTick(() => {
            watchCbList();
        });
    });

    function watchCbList() {
        $scope.cbListWatcher = $scope.$watch('vm.cbList', () => {
            if (!vm.checkboxList) {
                return;
            }
            for (var property in vm.checkboxList) {
                if (vm.checkboxList.hasOwnProperty(property)) {
                    delete vm.checkboxList[property];
                }
            }
            for (var i=0;i<vm.cbList.length;i++) {
                var entry = vm.cbList[i];
                vm.checkboxList[entry.name] = entry.checked;
            }
        }, true);
    }

    function stopWatchCbList() {
        if ($scope.cbListWatcher) {
            $scope.cbListWatcher();
            $scope.cbListWatcher = null;
        }
    }


    function removeCheckbox(index) {
        if (index > -1) {
            vm.cbList.splice(index, 1);
        }
    }

    function addCheckbox() {
        if (!vm.cbList) {
            vm.cbList = [];
        }
        vm.cbList.push(
            {
                name: '',
                checked: false
            }
        );
    }
}
