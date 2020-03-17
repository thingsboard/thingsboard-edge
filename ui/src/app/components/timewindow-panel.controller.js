/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
/*@ngInject*/
export default function TimewindowPanelController(mdPanelRef, $scope, timeService, types, timewindow, historyOnly, aggregation, isEdit, onTimewindowUpdate) {

    var vm = this;

    vm._mdPanelRef = mdPanelRef;
    vm.timewindow = timewindow;
    vm.historyOnly = historyOnly;
    vm.aggregation = aggregation;
    vm.onTimewindowUpdate = onTimewindowUpdate;
    vm.aggregationTypes = types.aggregation;
    vm.showLimit = showLimit;
    vm.showRealtimeAggInterval = showRealtimeAggInterval;
    vm.showHistoryAggInterval = showHistoryAggInterval;
    vm.minRealtimeAggInterval = minRealtimeAggInterval;
    vm.maxRealtimeAggInterval = maxRealtimeAggInterval;
    vm.minHistoryAggInterval = minHistoryAggInterval;
    vm.maxHistoryAggInterval = maxHistoryAggInterval;
    vm.minDatapointsLimit = minDatapointsLimit;
    vm.maxDatapointsLimit = maxDatapointsLimit;
    vm.isEdit = isEdit;

    if (vm.historyOnly) {
        vm.timewindow.selectedTab = 1;
    }

    vm._mdPanelRef.config.onOpenComplete = function () {
        $scope.theForm.$setPristine();
    }

    $scope.$watch('vm.timewindow.selectedTab', function (newSelection, prevSelection) {
        if (newSelection !== prevSelection) {
            $scope.theForm.$setDirty();
        }
    });

    vm.cancel = function () {
        vm._mdPanelRef && vm._mdPanelRef.close();
    };

    vm.update = function () {
        vm._mdPanelRef && vm._mdPanelRef.close().then(function () {
            vm.onTimewindowUpdate && vm.onTimewindowUpdate(vm.timewindow);
        });
    };

    function showLimit() {
        return vm.timewindow.aggregation.type === vm.aggregationTypes.none.value;
    }

    function showRealtimeAggInterval() {
        return vm.timewindow.aggregation.type !== vm.aggregationTypes.none.value &&
               vm.timewindow.selectedTab === 0;
    }

    function showHistoryAggInterval() {
        return vm.timewindow.aggregation.type !== vm.aggregationTypes.none.value &&
            vm.timewindow.selectedTab === 1;
    }

    function minRealtimeAggInterval () {
        return timeService.minIntervalLimit(vm.timewindow.realtime.timewindowMs);
    }

    function maxRealtimeAggInterval () {
        return timeService.maxIntervalLimit(vm.timewindow.realtime.timewindowMs);
    }

    function minHistoryAggInterval () {
        return timeService.minIntervalLimit(currentHistoryTimewindow());
    }

    function maxHistoryAggInterval () {
        return timeService.maxIntervalLimit(currentHistoryTimewindow());
    }

    function minDatapointsLimit () {
        return timeService.getMinDatapointsLimit();
    }

    function maxDatapointsLimit () {
        return timeService.getMaxDatapointsLimit();
    }

    function currentHistoryTimewindow() {
        if (vm.timewindow.history.historyType === 0) {
            return vm.timewindow.history.timewindowMs;
        } else {
            return vm.timewindow.history.fixedTimewindow.endTimeMs -
                vm.timewindow.history.fixedTimewindow.startTimeMs;
        }
    }

}

