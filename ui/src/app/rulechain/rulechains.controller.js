/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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

import addRuleChainTemplate from './add-rulechain.tpl.html';
import ruleChainCard from './rulechain-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleChainsController(ruleChainService, userService, importExport, $state,
                                             $stateParams, $filter, $translate, $mdDialog, types) {

    var ruleChainActionsList = [
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('rulechain.details') },
            details: function() { return $translate.instant('rulechain.rulechain-details') },
            icon: "edit"
        },
        {
            onAction: function ($event, item) {
                exportRuleChain($event, item);
            },
            name: function() { $translate.instant('action.export') },
            details: function() { return $translate.instant('rulechain.export') },
            icon: "file_download"
        },
        {
            onAction: function ($event, item) {
                setRootRuleChain($event, item);
            },
            name: function() { return $translate.instant('rulechain.set-root') },
            details: function() { return $translate.instant('rulechain.set-root') },
            icon: "flag",
            isEnabled: isNonRootRuleChain
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('rulechain.delete') },
            icon: "delete",
            isEnabled: isNonRootRuleChain
        }
    ];

    var ruleChainAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.create') },
            details: function() { return $translate.instant('rulechain.create-new-rulechain') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importRuleChain($event).then(
                    function(ruleChainImport) {
                        $state.go('home.ruleChains.importRuleChain', {ruleChainImport:ruleChainImport});
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('rulechain.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.ruleChainGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteRuleChainTitle,
        deleteItemContentFunc: deleteRuleChainText,
        deleteItemsTitleFunc: deleteRuleChainsTitle,
        deleteItemsActionTitleFunc: deleteRuleChainsActionTitle,
        deleteItemsContentFunc: deleteRuleChainsText,

        fetchItemsFunc: fetchRuleChains,
        saveItemFunc: saveRuleChain,
        clickItemFunc: openRuleChain,
        deleteItemFunc: deleteRuleChain,

        getItemTitleFunc: getRuleChainTitle,
        itemCardTemplateUrl: ruleChainCard,
        parentCtl: vm,

        actionsList: ruleChainActionsList,
        addItemActions: ruleChainAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addRuleChainTemplate,

        addItemText: function() { return $translate.instant('rulechain.add-rulechain-text') },
        noItemsText: function() { return $translate.instant('rulechain.no-rulechains-text') },
        itemDetailsText: function() { return $translate.instant('rulechain.rulechain-details') },
        isSelectionEnabled: isNonRootRuleChain
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.ruleChainGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.ruleChainGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.isRootRuleChain = isRootRuleChain;
    vm.isNonRootRuleChain = isNonRootRuleChain;

    vm.exportRuleChain = exportRuleChain;
    vm.setRootRuleChain = setRootRuleChain;

    function deleteRuleChainTitle(ruleChain) {
        return $translate.instant('rulechain.delete-rulechain-title', {ruleChainName: ruleChain.name});
    }

    function deleteRuleChainText() {
        return $translate.instant('rulechain.delete-rulechain-text');
    }

    function deleteRuleChainsTitle(selectedCount) {
        return $translate.instant('rulechain.delete-rulechains-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRuleChainsActionTitle(selectedCount) {
        return $translate.instant('rulechain.delete-rulechains-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRuleChainsText() {
        return $translate.instant('rulechain.delete-rulechains-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchRuleChains(pageLink) {
        return ruleChainService.getRuleChains(pageLink);
    }

    function saveRuleChain(ruleChain) {
        return ruleChainService.saveRuleChain(ruleChain);
    }

    function openRuleChain($event, ruleChain) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.ruleChains.ruleChain', {ruleChainId: ruleChain.id.id});
    }

    function deleteRuleChain(ruleChainId) {
        return ruleChainService.deleteRuleChain(ruleChainId);
    }

    function getRuleChainTitle(ruleChain) {
        return ruleChain ? ruleChain.name : '';
    }

    function isRootRuleChain(ruleChain) {
        return ruleChain && ruleChain.root;
    }

    function isNonRootRuleChain(ruleChain) {
        return ruleChain && !ruleChain.root;
    }

    function exportRuleChain($event, ruleChain) {
        $event.stopPropagation();
        importExport.exportRuleChain(ruleChain.id.id);
    }

    function setRootRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.set-root-rulechain-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.set-root-rulechain-text'))
            .ariaLabel($translate.instant('rulechain.set-root'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.setRootRuleChain(ruleChain.id.id).then(
                () => {
                    vm.grid.refreshList();
                }
            );
        });

    }
}
