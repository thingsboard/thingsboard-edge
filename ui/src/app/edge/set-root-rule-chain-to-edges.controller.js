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

/* eslint-disable no-undef,no-unused-vars */

/*@ngInject*/
export default function SetRootRuleChainToEdgesController(ruleChainService, edgeService, $mdDialog, $q, edgeIds, ruleChains) {

    var vm = this;

    /*vm.ruleChains = ruleChains;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.isRuleChainSelected = isRuleChainSelected;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchRuleChainTextUpdated = searchRuleChainTextUpdated;
    vm.toggleRuleChainSelection = toggleRuleChainSelection;

    vm.theRuleChains = {
        getItemAtIndex: function (index) {
            if (index > vm.ruleChains.data.length) {
                vm.theRuleChains.fetchMoreItems_(index);
                return null;
            }
            var item = vm.ruleChains.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.ruleChains.hasNext) {
                return vm.ruleChains.data.length + vm.ruleChains.nextPageLink.limit;
            } else {
                return vm.ruleChains.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.ruleChains.hasNext && !vm.ruleChains.pending) {
                vm.ruleChains.pending = true;
                ruleChainService.getEdgesRuleChains(vm.ruleChains.nextPageLink).then(
                    function success(ruleChains) {
                        vm.ruleChains.data = vm.ruleChains.data.concat(ruleChains.data);
                        vm.ruleChains.nextPageLink = ruleChains.nextPageLink;
                        vm.ruleChains.hasNext = ruleChains.hasNext;
                        if (vm.ruleChains.hasNext) {
                            vm.ruleChains.nextPageLink.limit = vm.ruleChains.pageSize;
                        }
                        vm.ruleChains.pending = false;
                    },
                    function fail() {
                        vm.ruleChains.hasNext = false;
                        vm.ruleChains.pending = false;
                    });
            }
        }
    };

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        var assignTasks = [];
        for (var i=0;i<edgeIds.length;i++) {
            assignTasks.push(ruleChainService.assignRuleChainToEdge(edgeIds[i], vm.ruleChains.selection.id.id));
        }
        $q.all(assignTasks).then(function () {
            var setRootTasks = [];
            for (var j=0;j<edgeIds.length;j++) {
                setRootTasks.push(edgeService.setRootRuleChain(edgeIds[j], vm.ruleChains.selection.id.id));
            }
            $q.all(setRootTasks).then(function () {
                $mdDialog.hide();
            });
        });
    }

    function noData() {
        return vm.ruleChains.data.length == 0 && !vm.ruleChains.hasNext;
    }

    function hasData() {
        return vm.ruleChains.data.length > 0;
    }

    function toggleRuleChainSelection($event, ruleChain) {
        $event.stopPropagation();
        if (vm.isRuleChainSelected(ruleChain)) {
            vm.ruleChains.selection = null;
        } else {
            vm.ruleChains.selection = ruleChain;
        }
    }

    function isRuleChainSelected(ruleChain) {
        return vm.ruleChains.selection != null && ruleChain &&
            ruleChain.id.id === vm.ruleChains.selection.id.id;
    }

    function searchRuleChainTextUpdated() {
        vm.ruleChains = {
            pageSize: vm.ruleChains.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.ruleChains.pageSize,
                textSearch: vm.searchText
            },
            selection: null,
            hasNext: true,
            pending: false
        };
    }*/
}
