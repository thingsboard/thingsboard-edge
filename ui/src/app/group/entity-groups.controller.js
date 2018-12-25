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
/* eslint-disable import/no-unresolved, import/default */

import addEntityGroupTemplate from './add-entity-group.tpl.html';
import entityGroupCard from './entity-group-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function EntityGroupCardController() {

    var vm = this; //eslint-disable-line

}


/*@ngInject*/
export function EntityGroupsController($rootScope, $state, utils, entityGroupService, customerService, $stateParams,
                                      $q, $translate, types, securityTypes, userPermissionsService) {

    var vm = this;

    vm.customerId = $stateParams.customerId;
    if (vm.customerId && $stateParams.childGroupType) {
        vm.groupType = $stateParams.childGroupType;
    } else {
        vm.groupType = $stateParams.groupType;
    }

    vm.types = types;

    vm.groupResource = securityTypes.groupResourceByGroupType[vm.groupType];

    var entityGroupActionsList = [
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('entity-group.details') },
            details: function() { return $translate.instant('entity-group.entity-group-details') },
            icon: "edit"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('entity-group.delete') },
            icon: "delete",
            isEnabled: function(entityGroup) {
                return !entityGroup.groupAll && userPermissionsService.hasEntityGroupPermission(securityTypes.operation.delete, entityGroup);
            }
        }

    ];

    vm.actionSources = {
        'actionCellButton': {
            name: 'widget-action.action-cell-button',
            multiple: true
        },
        'rowClick': {
            name: 'widget-action.row-click',
            multiple: false
        }
    };

    vm.entityGroupGridConfig = {

        resource: vm.groupResource,

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteEntityGroupTitle,
        deleteItemContentFunc: deleteEntityGroupText,
        deleteItemsTitleFunc: deleteEntityGroupsTitle,
        deleteItemsActionTitleFunc: deleteEntityGroupsActionTitle,
        deleteItemsContentFunc: deleteEntityGroupsText,

        fetchItemsFunc: fetchEntityGroups,
        saveItemFunc: saveEntityGroup,
        deleteItemFunc: deleteEntityGroup,

        clickItemFunc: openEntityGroup,

        getItemTitleFunc: getEntityGroupTitle,

        itemCardController: 'EntityGroupCardController',
        itemCardTemplateUrl: entityGroupCard,
        parentCtl: vm,

        actionsList: entityGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addEntityGroupTemplate,

        addItemText: function() { return $translate.instant('entity-group.add-entity-group-text') },
        noItemsText: function() { return $translate.instant('entity-group.no-entity-groups-text') },
        itemDetailsText: function() { return $translate.instant('entity-group.entity-group-details') },
        isDetailsReadOnly: function(entityGroup) {
            return !userPermissionsService.hasEntityGroupPermission(securityTypes.operation.write, entityGroup);
        },
        isSelectionEnabled: function(entityGroup) {
            return !entityGroup.groupAll && userPermissionsService.hasEntityGroupPermission(securityTypes.operation.delete, entityGroup);
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.deviceGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.deviceGridConfig.topIndex = $stateParams.topIndex;
    }

    function deleteEntityGroupTitle(entityGroup) {
        return $translate.instant('entity-group.delete-entity-group-title', {entityGroupName: entityGroup.name});
    }

    function deleteEntityGroupText() {
        return $translate.instant('entity-group.delete-entity-group-text');
    }

    function deleteEntityGroupsTitle(selectedCount) {
        return $translate.instant('entity-group.delete-entity-groups-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEntityGroupsActionTitle(selectedCount) {
        return $translate.instant('entity-group.delete-entity-groups-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEntityGroupsText () {
        return $translate.instant('entity-group.delete-entity-groups-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getEntityGroupTitle(entityGroup) {
        return entityGroup ? utils.customTranslation(entityGroup.name, entityGroup.name) : '';
    }

    function fetchEntityGroups(pageLink) {
        var deferred = $q.defer();
        var fetchPromise;
        if (vm.customerId) {
            fetchPromise = entityGroupService.getCustomerEntityGroups(vm.customerId, vm.groupType);
        } else {
            fetchPromise = entityGroupService.getEntityGroups(vm.groupType);
        }
        fetchPromise.then(
            function success(entityGroups) {
                utils.filterSearchTextEntities(entityGroups, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function saveEntityGroup(entityGroup) {
        var deferred = $q.defer();
        entityGroup.type = vm.groupType;
        if (vm.customerId) {
            entityGroup.ownerId = {
                entityType: types.entityType.customer,
                id: vm.customerId
            };
        }
        entityGroupService.saveEntityGroup(entityGroup).then(
            function success(entityGroup) {
                deferred.resolve(entityGroup);
                if (!vm.customerId) {
                    $rootScope.$broadcast(vm.groupType + 'changed');
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function deleteEntityGroup(entityGroupId) {
        var deferred = $q.defer();
        entityGroupService.deleteEntityGroup(entityGroupId).then(
            function success() {
                deferred.resolve();
                $rootScope.$broadcast(vm.groupType+'changed');
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function openEntityGroup($event, entityGroup) {
        if ($event) {
            $event.stopPropagation();
        }
        var targetStatePrefix = 'home.';
        if (vm.customerId) {
            targetStatePrefix = 'home.customerGroups.customerGroup.';
        }
        var targetState;
        if (entityGroup.type == types.entityType.asset) {
            targetState = 'assetGroups.assetGroup';
        } else if (entityGroup.type == types.entityType.device) {
            targetState = 'deviceGroups.deviceGroup';
        } else if (entityGroup.type == types.entityType.customer) {
            targetState = 'customerGroups.customerGroup';
        } else if (entityGroup.type == types.entityType.user) {
            targetState = 'userGroups.userGroup';
        } else if (entityGroup.type == types.entityType.entityView) {
            targetState = 'entityViewGroups.entityViewGroup';
        } else if (entityGroup.type == types.entityType.dashboard) {
            targetState = 'dashboardGroups.dashboardGroup';
        }
        if (targetState) {
            targetState = targetStatePrefix + targetState;
            if (vm.customerId) {
                $state.go(targetState, {childEntityGroupId: entityGroup.id.id});
            } else {
                $state.go(targetState, {entityGroupId: entityGroup.id.id});
            }
        }
    }
}
