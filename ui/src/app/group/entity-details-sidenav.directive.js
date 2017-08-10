/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
function EntityDetailsSidenavController($scope, $window, types, entityService, helpLinks) {

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

}