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

import addTenantTemplate from './add-tenant.tpl.html';
import tenantCard from './tenant-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function TenantController(tenantService, $state, $stateParams, $translate, types) {

    var tenantActionsList = [
        {
            onAction: function ($event, item) {
                openTenantUsers($event, item);
            },
            name: function() { return $translate.instant('tenant.admins') },
            details: function() { return $translate.instant('tenant.manage-tenant-admins') },
            icon: "account_circle"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('tenant.delete') },
            icon: "delete"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.tenantGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteTenantTitle,
        deleteItemContentFunc: deleteTenantText,
        deleteItemsTitleFunc: deleteTenantsTitle,
        deleteItemsActionTitleFunc: deleteTenantsActionTitle,
        deleteItemsContentFunc: deleteTenantsText,

        fetchItemsFunc: fetchTenants,
        saveItemFunc: saveTenant,
        deleteItemFunc: deleteTenant,

        getItemTitleFunc: getTenantTitle,

        itemCardTemplateUrl: tenantCard,

        actionsList: tenantActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addTenantTemplate,

        addItemText: function() { return $translate.instant('tenant.add-tenant-text') },
        noItemsText: function() { return $translate.instant('tenant.no-tenants-text') },
        itemDetailsText: function() { return $translate.instant('tenant.tenant-details') }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.tenantGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.tenantGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.openTenantUsers = openTenantUsers;

    function deleteTenantTitle(tenant) {
        return $translate.instant('tenant.delete-tenant-title', {tenantTitle: tenant.title});
    }

    function deleteTenantText() {
        return $translate.instant('tenant.delete-tenant-text');
    }

    function deleteTenantsTitle(selectedCount) {
        return $translate.instant('tenant.delete-tenants-title', {count: selectedCount}, 'messageformat');
    }

    function deleteTenantsActionTitle(selectedCount) {
        return $translate.instant('tenant.delete-tenants-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteTenantsText() {
        return $translate.instant('tenant.delete-tenants-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchTenants(pageLink) {
        return tenantService.getTenants(pageLink);
    }

    function saveTenant(tenant) {
        return tenantService.saveTenant(tenant);
    }

    function deleteTenant(tenantId) {
        return tenantService.deleteTenant(tenantId);
    }

    function getTenantTitle(tenant) {
        return tenant ? tenant.title : '';
    }

    function openTenantUsers($event, tenant) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.tenants.users', {tenantId: tenant.id.id});
    }
}
