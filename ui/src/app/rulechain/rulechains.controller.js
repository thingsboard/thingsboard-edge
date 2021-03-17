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
/* eslint-disable import/no-unresolved, import/default */

import addRuleChainTemplate from './add-rulechain.tpl.html';
import ruleChainCard from './rulechain-card.tpl.html';
import addRuleChainsToEdgeTemplate from "./add-rulechains-to-edge.tpl.html";

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleChainsController(ruleChainService, userService, importExport, $state, $stateParams, $filter, $translate, $mdDialog, types,
                                             $document, $q, securityTypes, utils, userPermissionsService, edgeService) {

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

    vm.edgeId = $stateParams.edgeId;

    vm.types = types;

    vm.ruleChainsScope = $state.$current.data.ruleChainsType;

    vm.ruleChainGridConfig = {

        resource: securityTypes.resource.ruleChain,

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
        unassignFromEdgeItemFunc: unassignRuleChainsFromEdge,

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
        isSelectionEnabled: function(ruleChain) {
            return isNonRootRuleChain(ruleChain) &&
                userPermissionsService.hasGenericPermission(securityTypes.resource.ruleChain, securityTypes.operation.delete);
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.ruleChainGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.ruleChainGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.isRootRuleChain = isRootRuleChain;
    vm.isNonRootRuleChain = isNonRootRuleChain;
    vm.isDefaultEdgeRuleChain = isDefaultEdgeRuleChain;

    vm.exportRuleChain = exportRuleChain;
    vm.setRootRuleChain = setRootRuleChain;
    vm.setAutoAssignToEdgeRuleChain = setAutoAssignToEdgeRuleChain;
    vm.unsetAutoAssignToEdgeRuleChain = unsetAutoAssignToEdgeRuleChain;
    vm.unassignFromEdge = unassignFromEdge;

    initController();

    function initController() {
        var fetchRuleChainsFunction = null;
        var deleteRuleChainFunction = null;
        var unassignEnabled = false;

        if (vm.edgeId) {
            edgeService.getEdge(vm.edgeId, true).then(
                function success(edge) {
                    vm.edge = edge;
                }
            );
        }

        if (vm.ruleChainsScope === 'tenant') {
            fetchRuleChainsFunction = function (pageLink) {
                return fetchRuleChains(pageLink, types.ruleChainType.core);
            };
            deleteRuleChainFunction = function (ruleChainId) {
                return deleteRuleChain(ruleChainId);
            };

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    setRootRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.set-root') },
                details: function() { return $translate.instant('rulechain.set-root') },
                icon: "flag",
                isEnabled: function(ruleChain) {
                    return isNonRootRuleChain(ruleChain) &&
                        userPermissionsService.hasGenericPermission(securityTypes.resource.ruleChain, securityTypes.operation.write);
                }
            });

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    vm.grid.deleteItem($event, item);
                },
                name: function() { return $translate.instant('action.delete') },
                details: function() { return $translate.instant('rulechain.delete') },
                icon: "delete",
                isEnabled: function(ruleChain) {
                    return isNonRootRuleChain(ruleChain) &&
                        userPermissionsService.hasGenericPermission(securityTypes.resource.ruleChain, securityTypes.operation.delete);
                }
            });

            vm.ruleChainGridConfig.addItemActions = [];
            vm.ruleChainGridConfig.addItemActions.push({
                onAction: function ($event) {
                    vm.grid.addItem($event);
                },
                name: function() { return $translate.instant('action.create') },
                details: function() { return $translate.instant('rulechain.create-new-rulechain') },
                icon: "insert_drive_file"
            });
            vm.ruleChainGridConfig.addItemActions.push({
                onAction: function ($event) {
                    importExport.importRuleChain($event, types.ruleChainType.core).then(
                        function(ruleChainImport) {
                            $state.go('home.ruleChains.importRuleChain', {ruleChainImport:ruleChainImport, ruleChainType: types.ruleChainType.core});
                        }
                    );
                },
                name: function() { return $translate.instant('action.import') },
                details: function() { return $translate.instant('rulechain.import') },
                icon: "file_upload"
            });

        } else if (vm.ruleChainsScope === 'edges') {
            fetchRuleChainsFunction = function (pageLink) {
                return fetchRuleChains(pageLink, types.ruleChainType.edge);
            };
            deleteRuleChainFunction = function (ruleChainId) {
                return deleteRuleChain(ruleChainId);
            };

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    setAutoAssignToEdgeRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.set-auto-assign-to-edge') },
                details: function() { return $translate.instant('rulechain.set-auto-assign-to-edge') },
                icon: "bookmark_outline",
                isEnabled: isNonDefaultEdgeRuleChain
            });

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    unsetAutoAssignToEdgeRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.unset-auto-assign-to-edge') },
                details: function() { return $translate.instant('rulechain.unset-auto-assign-to-edge') },
                icon: "bookmark",
                isEnabled: isDefaultEdgeRuleChain
            });

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    setEdgeTemplateRootRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.set-edge-template-root-rulechain') },
                details: function() { return $translate.instant('rulechain.set-edge-template-root-rulechain') },
                icon: "flag",
                isEnabled: isNonRootRuleChain
            });

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    vm.grid.deleteItem($event, item);
                },
                name: function() { return $translate.instant('action.delete') },
                details: function() { return $translate.instant('rulechain.delete') },
                icon: "delete",
                isEnabled: isNonRootRuleChain
            });

            vm.ruleChainGridConfig.addItemActions = [];
            vm.ruleChainGridConfig.addItemActions.push({
                onAction: function ($event) {
                    vm.grid.addItem($event);
                },
                name: function() { return $translate.instant('action.create') },
                details: function() { return $translate.instant('rulechain.create-new-rulechain') },
                icon: "insert_drive_file"
            });
            vm.ruleChainGridConfig.addItemActions.push({
                onAction: function ($event) {
                    importExport.importRuleChain($event, types.ruleChainType.edge).then(
                        function(ruleChainImport) {
                            $state.go('home.edges.ruleChains.importRuleChain', {ruleChainImport:ruleChainImport, ruleChainType: types.ruleChainType.edge});
                        }
                    );
                },
                name: function() { return $translate.instant('action.import') },
                details: function() { return $translate.instant('rulechain.import') },
                icon: "file_upload"
            });

        } else if (vm.ruleChainsScope === 'edge') {
            unassignEnabled = true;

            vm.ruleChainGridConfig.unassignFromEdgeItemsTitleFunc = function (selectedCount) {
                return $translate.instant('rulechain.unassign-rulechains-title', {count: selectedCount}, 'messageformat');
            };

            vm.ruleChainGridConfig.unassignFromEdgeItemsActionTitleFunc = function (selectedCount) {
                return $translate.instant('rulechain.unassign-rulechains-from-edge-action-title', {count: selectedCount}, 'messageformat');
            };

            vm.ruleChainGridConfig.unassignFromEdgeItemsContentFunc = function () {
                return $translate.instant('rulechain.unassign-rulechains-from-edge-text');
            };

            fetchRuleChainsFunction = function (pageLink) {
                return ruleChainService.getEdgeRuleChains(vm.edgeId, pageLink);
            };
            deleteRuleChainFunction = function (ruleChainId) {
                return ruleChainService.unassignRuleChainFromEdge(vm.edgeId, ruleChainId);
            };

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    setRootRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.set-root') },
                details: function() { return $translate.instant('rulechain.set-root') },
                icon: "flag",
                isEnabled: isNonRootRuleChain
            });

            ruleChainActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromEdge($event, item);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('edge.unassign-from-edge') },
                    icon: "assignment_return",
                    isEnabled: isNonRootRuleChain
                }
            );

            vm.ruleChainGridConfig.addItemAction = {
                onAction: function ($event) {
                    addRuleChainsToEdge($event);
                },
                name: function() { return $translate.instant('rulechain.assign-rulechains') },
                details: function() { return $translate.instant('rulechain.assign-new-rulechain') },
                icon: "add"
            }

            vm.ruleChainGridConfig.addItemActions = []; //TODO deaflynx : is this needed?

        }

        vm.ruleChainGridConfig.unassignEnabled = unassignEnabled;

        vm.ruleChainGridConfig.fetchItemsFunc = fetchRuleChainsFunction;
        vm.ruleChainGridConfig.deleteItemFunc = deleteRuleChainFunction;
    }

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

    function mapRuleChainsWithDefaultEdges(ruleChains) {
        var deferred = $q.defer();
        ruleChainService.getAutoAssignToEdgeRuleChains(null).then(
            function success(response) {
                let defaultEdgeRuleChainIds = [];
                response.map(function (ruleChain) {
                    defaultEdgeRuleChainIds.push(ruleChain.id.id)
                });
                const data = ruleChains.data;
                data.map(function (ruleChain) {
                    ruleChain.isDefault = defaultEdgeRuleChainIds.some(id => ruleChain.id.id.includes(id));
                    return ruleChain;
                });
                ruleChains.data = data;

                deferred.resolve(ruleChains);
            }, function fail() {
                deferred.reject();
            }
        )
        return deferred.promise;
    }

    function fetchRuleChains(pageLink, type) {
        if (vm.ruleChainsScope === 'tenant') {
            return ruleChainService.getRuleChains(pageLink, null, type);
        } else if (vm.ruleChainsScope === 'edges') {
            var deferred = $q.defer();
            ruleChainService.getRuleChains(pageLink, null, type).then(
                function success(ruleChains) {
                    mapRuleChainsWithDefaultEdges(ruleChains).then(
                        function success(response) {
                            deferred.resolve(response);
                        }, function fail() {
                            deferred.reject();
                        }
                    );
                }, function fail() {
                    deferred.reject();
                }
            );
            return deferred.promise;
        }
    }

    function saveRuleChain(ruleChain) {
        if (angular.isUndefined(ruleChain.type)) {
            if (vm.ruleChainsScope === 'edges') {
                ruleChain.type = types.ruleChainType.edge;
            } else {
                ruleChain.type = types.ruleChainType.core;
            }
        }
        return ruleChainService.saveRuleChain(ruleChain);
    }

    function openRuleChain($event, ruleChain) {
        if ($event) {
            $event.stopPropagation();
        }
        var ruleChainParams = {ruleChainId: ruleChain.id.id};
        if (vm.ruleChainsScope === 'edge') {
            $state.go('home.edgeGroups.edgeGroup.ruleChains.ruleChain', {ruleChainId: ruleChain.id.id, edgeId: vm.edgeId});
        } else if (vm.ruleChainsScope === 'edges') {
            $state.go('home.edges.ruleChains.ruleChain', ruleChainParams);
        } else {
            $state.go('home.ruleChains.ruleChain', ruleChainParams);
        }
    }

    function deleteRuleChain(ruleChainId) {
        return ruleChainService.deleteRuleChain(ruleChainId);
    }

    function getRuleChainTitle(ruleChain) {
        return ruleChain ? utils.customTranslation(ruleChain.name, ruleChain.name) : '';
    }

    function isRootRuleChain(ruleChain) {
        if (angular.isDefined(vm.edge) && vm.edge != null) {
            return angular.isDefined(vm.edge.rootRuleChainId) && vm.edge.rootRuleChainId != null && vm.edge.rootRuleChainId.id === ruleChain.id.id;
        } else {
            return ruleChain && ruleChain.root;
        }
    }

    function isNonRootRuleChain(ruleChain) {
        if (angular.isDefined(vm.edge) && vm.edge != null) {
            return angular.isDefined(vm.edge.rootRuleChainId) && vm.edge.rootRuleChainId != null && vm.edge.rootRuleChainId.id !== ruleChain.id.id;
        } else {
            return ruleChain && !ruleChain.root;
        }
    }

    function isDefaultEdgeRuleChain(ruleChain) {
        return angular.isDefined(ruleChain) && !ruleChain.root && ruleChain.isDefault;
    }

    function isNonDefaultEdgeRuleChain(ruleChain) {
        return angular.isDefined(ruleChain) && !ruleChain.root && !ruleChain.isDefault;
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
            if (angular.isDefined(vm.edge) && vm.edge != null) {
                edgeService.setRootRuleChain(vm.edgeId, ruleChain.id.id).then(
                    (edge) => {
                        vm.edge = edge;
                        vm.grid.refreshList();
                    }
                );
            } else {
                ruleChainService.setRootRuleChain(ruleChain.id.id).then(
                    () => {
                        vm.grid.refreshList();
                    }
                );
            }
        });
    }

    function setAutoAssignToEdgeRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.set-auto-assign-to-edge-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.set-auto-assign-to-edge-text'))
            .ariaLabel($translate.instant('rulechain.set-auto-assign-to-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.setAutoAssignToEdgeRuleChain(ruleChain.id.id).then(
                    () => {
                        vm.grid.refreshList();
                    }
                );
        });
    }

    function unsetAutoAssignToEdgeRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.unset-auto-assign-to-edge-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.unset-auto-assign-to-edge-text'))
            .ariaLabel($translate.instant('rulechain.unset-auto-assign-to-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.unsetAutoAssignToEdgeRuleChain(ruleChain.id.id).then(
                () => {
                    vm.grid.refreshList();
                }
            );
        });
    }

    function setEdgeTemplateRootRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.set-edge-template-root-rulechain-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.set-edge-template-root-rulechain-text'))
            .ariaLabel($translate.instant('rulechain.set-root-rulechain-text'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.setEdgeTemplateRootRuleChain(ruleChain.id.id).then(
                () => {
                    vm.grid.refreshList();
                }
            );
        });
    }

    function unassignRuleChainsFromEdge(ruleChainId) {
        var deferred = $q.defer();
        ruleChainService.unassignRuleChainFromEdge(vm.edgeId, ruleChainId).then(
            function success() {
                deferred.resolve();
                if ($stateParams.hierarchyView && $stateParams.hierarchyCallbacks.refreshEntityGroups) {
                    $stateParams.hierarchyCallbacks.refreshEntityGroups($stateParams.internalId);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function addRuleChainsToEdge($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        ruleChainService.getEdgesRuleChains({limit: pageSize, textSearch: ''}).then(
            function success(_ruleChains) {
                var ruleChains = {
                    pageSize: pageSize,
                    data: _ruleChains.data,
                    nextPageLink: _ruleChains.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _ruleChains.hasNext,
                    pending: false
                };
                if (ruleChains.hasNext) {
                    ruleChains.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddRuleChainsToEdgeController',
                    controllerAs: 'vm',
                    templateUrl: addRuleChainsToEdgeTemplate,
                    locals: {edgeId: vm.edgeId, ruleChains: ruleChains},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    edgeService.findMissingToRelatedRuleChains(vm.edgeId).then(
                        function success(missingRuleChains) {
                            if (missingRuleChains && Object.keys(missingRuleChains).length > 0) {
                                let formattedMissingRuleChains = [];
                                for (const missingRuleChain of Object.keys(missingRuleChains)) {
                                    const arrayOfMissingRuleChains = missingRuleChains[missingRuleChain];
                                    const tmp = "- '" + missingRuleChain + "': '" + arrayOfMissingRuleChains.join("', ") + "'";
                                    formattedMissingRuleChains.push(tmp);
                                }
                                var alert = $mdDialog.alert()
                                    .parent(angular.element($document[0].body))
                                    .clickOutsideToClose(true)
                                    .title($translate.instant('edge.missing-related-rule-chains-title'))
                                    .htmlContent($translate.instant('edge.missing-related-rule-chains-text', {missingRuleChains: formattedMissingRuleChains.join("<br>")}))
                                    .ok($translate.instant('action.close'));
                                alert._options.fullscreen = true;
                                $mdDialog.show(alert).then(
                                    function () {
                                        vm.grid.refreshList();
                                    }
                                );
                            } else {
                                vm.grid.refreshList();
                            }
                        }
                    );
                });
            },
            function fail() {
            });
    }

    function unassignFromEdge($event, ruleChain) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('rulechain.unassign-rulechain-title', {ruleChainTitle: ruleChain.name});
        var content = $translate.instant('rulechain.unassign-rulechain-from-edge-text');
        var label = $translate.instant('rulechain.unassign-rulechain');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.unassignRuleChainFromEdge(vm.edgeId, ruleChain.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }
}
