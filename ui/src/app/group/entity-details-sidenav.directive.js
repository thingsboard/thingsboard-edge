/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import entityDetailsSidenavTemplate from './entity-details-sidenav.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityDetailsSidenav() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            entityGroupConfig: '=',
            isOpen: '=',
            entityType: '=',
            entityId: '=',
            onEntityUpdated: '&'
        },
        controller: EntityDetailsSidenavController,
        controllerAs: 'vm',
        templateUrl: entityDetailsSidenavTemplate
    };
}

/*@ngInject*/
function EntityDetailsSidenavController($scope, $window, types, userService, entityService, helpLinks) {

    var vm = this;

    vm.types = types;

    vm.translations = vm.types.entityTypeTranslations[vm.entityType];
    vm.resources = vm.types.entityTypeResources[vm.entityType];

    vm.entity = null;
    vm.editingEntity = null;
    vm.isEdit = false;
    vm.isReadOnly = false;

    vm.onToggleEditMode = onToggleEditMode;
    vm.onCloseDetails = onCloseDetails;
    vm.saveEntity = saveEntity;
    vm.entityHelpLinkId = entityHelpLinkId;
    vm.triggerResize = triggerResize;
    vm.isTenantAdmin = isTenantAdmin;

    $scope.$watch('vm.entityId', function(newVal, prevVal) {
        if (vm.entityId && !angular.equals(newVal, prevVal)) {
            reload();
        }
    });

    $scope.$on('reloadEntityDetails', () => {
        if (vm.entityId) {
            reload();
        }
    });

    function reload() {
        vm.isEdit = false;
        vm.detailsForm.$setPristine();
        loadEntity();
    }

    function loadEntity() {
        entityService.getEntity(vm.entityType, vm.entityId).then(
            function success(entity) {
                vm.entity = entity;
                vm.editingEntity = angular.copy(vm.entity);
            },
            function fail() {}
        );
    }

    function onToggleEditMode() {
        if (!vm.isEdit) {
            vm.detailsForm.$setPristine();
            vm.editingEntity = angular.copy(vm.entity);
        }
    }

    function onCloseDetails() {

    }

    function saveEntity() {
        entityService.saveEntity(vm.editingEntity).then(
            function success(entity) {
                vm.entity = entity;
                vm.detailsForm.$setPristine();
                vm.isEdit = false;
                vm.onEntityUpdated({entity: vm.entity});
            }
        );
    }

    function entityHelpLinkId() {
        if (vm.entityType == types.entityType.plugin && vm.entity) {
            return helpLinks.getPluginLink(vm.entity);
        } else {
            return vm.resources.helpId;
        }
    }

    function triggerResize() {
        var w = angular.element($window);
        w.triggerHandler('resize');
    }

    function isTenantAdmin() {
        return userService.getAuthority() == 'TENANT_ADMIN';
    }

}