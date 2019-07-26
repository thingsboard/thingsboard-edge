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
import './kv-map.scss';

/* eslint-disable import/no-unresolved, import/default */

import kvMapTemplate from './kv-map.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.keyValMap', [])
    .directive('tbKeyValMap', KeyValMap)
    .name;

/*@ngInject*/
function KeyValMap() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            disabled:'=ngDisabled',
            titleText: '@?',
            keyPlaceholderText: '@?',
            valuePlaceholderText: '@?',
            noDataText: '@?',
            keyValMap: '='
        },
        controller: KeyValMapController,
        controllerAs: 'vm',
        templateUrl: kvMapTemplate
    };
}

/*@ngInject*/
function KeyValMapController($scope, $mdUtil) {

    let vm = this;

    vm.kvList = [];

    vm.removeKeyVal = removeKeyVal;
    vm.addKeyVal = addKeyVal;

    $scope.$watch('vm.keyValMap', () => {
        stopWatchKvList();
        vm.kvList.length = 0;
        if (vm.keyValMap) {
            for (var property in vm.keyValMap) {
                if (Object.prototype.hasOwnProperty.call(vm.keyValMap, property)) {
                    vm.kvList.push(
                        {
                            key: property + '',
                            value: vm.keyValMap[property]
                        }
                    );
                }
            }
        }
        $mdUtil.nextTick(() => {
            watchKvList();
        });
    });

    function watchKvList() {
        $scope.kvListWatcher = $scope.$watch('vm.kvList', () => {
            if (!vm.keyValMap) {
                return;
            }
            for (var property in vm.keyValMap) {
                if (Object.prototype.hasOwnProperty.call(vm.keyValMap, property)) {
                    delete vm.keyValMap[property];
                }
            }
            for (var i=0;i<vm.kvList.length;i++) {
                var entry = vm.kvList[i];
                vm.keyValMap[entry.key] = entry.value;
            }
        }, true);
    }

    function stopWatchKvList() {
        if ($scope.kvListWatcher) {
            $scope.kvListWatcher();
            $scope.kvListWatcher = null;
        }
    }


    function removeKeyVal(index) {
        if (index > -1) {
            vm.kvList.splice(index, 1);
        }
    }

    function addKeyVal() {
        if (!vm.kvList) {
            vm.kvList = [];
        }
        vm.kvList.push(
            {
                key: '',
                value: ''
            }
        );
    }
}
