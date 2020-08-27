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
export default function RuleChainsController(ruleChainService, userService, edgeService, importExport, $state,
                                             $stateParams, $filter, $translate, $mdDialog, $document, $q, types, securityTypes, utils, userPermissionsService) {

    var vm = this;
    var edgeId = $stateParams.edgeId;

    vm.ruleChainsScope = $state.$current.data.ruleChainsType;

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

    var ruleChainGroupActionsList = [];

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

    vm.types = types;

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

        getItemTitleFunc: getRuleChainTitle,
        itemCardTemplateUrl: ruleChainCard,
        parentCtl: vm,

        actionsList: ruleChainActionsList,
        addItemActions: ruleChainAddItemActionsList,
        groupActionsList: ruleChainGroupActionsList,

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

    initController();

    function initController() {
        var fetchRuleChainsFunction = null;
        var deleteRuleChainFunction = null;

        if (edgeId) {
            vm.edgeRuleChainsTitle = $translate.instant('edge.rulechains');
            edgeService.getEdge(edgeId).then(
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

            ruleChainGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('rulechain.delete-rulechains') },
                    details: deleteRuleChainsActionTitle,
                    icon: "delete"
                }
            );

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
                    setDefaultEdgeRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.set-default-edge') },
                details: function() { return $translate.instant('rulechain.set-default-edge') },
                icon: "bookmark_outline",
                isEnabled: isNonDefaultEdgeRuleChain
            });

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    removeDefaultEdgeRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.remove-default-edge') },
                details: function() { return $translate.instant('rulechain.remove-default-edge') },
                icon: "bookmark",
                isEnabled: isDefaultEdgeRuleChain
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

            ruleChainActionsList.push({
                onAction: function ($event, item) {
                    setDefaultRootEdgeRuleChain($event, item);
                },
                name: function() { return $translate.instant('rulechain.set-default-root-edge') },
                details: function() { return $translate.instant('rulechain.set-default-root-edge') },
                icon: "flag",
                isEnabled: isNonRootRuleChain
            });

            ruleChainGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('rulechain.delete-rulechains') },
                    details: deleteRuleChainsActionTitle,
                    icon: "delete"
                }
            );

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
                            $state.go('home.ruleChains.importRuleChain', {ruleChainImport:ruleChainImport, ruleChainType: types.ruleChainType.edge});
                        }
                    );
                },
                name: function() { return $translate.instant('action.import') },
                details: function() { return $translate.instant('rulechain.import') },
                icon: "file_upload"
            });

        } else if (vm.ruleChainsScope === 'edge') {
            fetchRuleChainsFunction = function (pageLink) {
                return ruleChainService.getEdgeRuleChains(edgeId, pageLink);
            };
            deleteRuleChainFunction = function (ruleChainId) {
                return ruleChainService.unassignRuleChainFromEdge(edgeId, ruleChainId);
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
                        unassignFromEdge($event, item, edgeId);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('rulechain.unassign-from-edge') },
                    icon: "assignment_return",
                    isEnabled: isNonRootRuleChain
                }
            );

            ruleChainGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        unassignRuleChainsFromEdge($event, items, edgeId);
                    },
                    name: function() { return $translate.instant('rulechain.unassign-rulechains') },
                    details: function(selectedCount) {
                        return $translate.instant('rulechain.unassign-rulechains-from-edge-action-title', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_return"
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
            vm.ruleChainGridConfig.addItemActions = [];
        }

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
        ruleChainService.getDefaultEdgeRuleChains(null).then(
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
        if (vm.ruleChainsScope === 'edge') {
            $state.go('home.edgeGroups.edgeGroup.ruleChains.ruleChain', {ruleChainId: ruleChain.id.id, edgeId: vm.edge.id.id});
        } else if (vm.ruleChainsScope === 'edges') {
            $state.go('home.ruleChains.edge.ruleChain', {ruleChainId: ruleChain.id.id});
        } else {
            $state.go('home.ruleChains.core.ruleChain', {ruleChainId: ruleChain.id.id});
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
                edgeService.setRootRuleChain(vm.edge.id.id, ruleChain.id.id).then(
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

    function setDefaultEdgeRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.set-default-edge-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.set-default-edge-text'))
            .ariaLabel($translate.instant('rulechain.set-default-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.addDefaultEdgeRuleChain(ruleChain.id.id).then(
                    () => {
                        vm.grid.refreshList();
                    }
                );
        });
    }

    function removeDefaultEdgeRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.remove-default-edge-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.remove-default-edge-text'))
            .ariaLabel($translate.instant('rulechain.remove-default-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.removeDefaultEdgeRuleChain(ruleChain.id.id).then(
                () => {
                    vm.grid.refreshList();
                }
            );
        });
    }

    function setDefaultRootEdgeRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.set-default-root-edge-rulechain-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.set-default-root-edge-rulechain-text'))
            .ariaLabel($translate.instant('rulechain.set-root-rulechain-text'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.setDefaultRootEdgeRuleChain(ruleChain.id.id).then(
                () => {
                    vm.grid.refreshList();
                }
            );
        });
    }

    function unassignRuleChainsFromEdge($event, items, edgeId) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.unassign-rulechains-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('rulechain.unassign-rulechains-from-edge-text'))
            .ariaLabel($translate.instant('rulechain.unassign-rulechains'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(ruleChainService.unassignRuleChainFromEdge(edgeId, id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
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
                    locals: {edgeId: edgeId, ruleChains: ruleChains},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    vm.grid.refreshList();
                }, function () {
                });
            },
            function fail() {
            });
    }

    function unassignFromEdge($event, ruleChain, edgeId) {
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
            ruleChainService.unassignRuleChainFromEdge(edgeId, ruleChain.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }
}
