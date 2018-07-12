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

import addCustomerTemplate from './add-customer.tpl.html';
import customerCard from './customer-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CustomerController(customerService, $state, $stateParams, $translate, types) {

    var customerActionsList = [
        {
            onAction: function ($event, item) {
                openCustomerUsers($event, item);
            },
            name: function() { return $translate.instant('user.users') },
            details: function() { return $translate.instant('customer.manage-customer-users') },
            icon: "account_circle",
            isEnabled: function(customer) {
                return customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
            }
        },
        {
            onAction: function ($event, item) {
                openCustomerAssets($event, item);
            },
            name: function() { return $translate.instant('asset.assets') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-assets')
                } else {
                    return $translate.instant('customer.manage-customer-assets')
                }
            },
            icon: "domain"
        },
        {
            onAction: function ($event, item) {
                openCustomerDevices($event, item);
            },
            name: function() { return $translate.instant('device.devices') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-devices')
                } else {
                    return $translate.instant('customer.manage-customer-devices')
                }
            },
            icon: "devices_other"
        },
        {
            onAction: function ($event, item) {
                openCustomerDashboards($event, item);
            },
            name: function() { return $translate.instant('dashboard.dashboards') },
            details: function(customer) {
                if (customer && customer.additionalInfo && customer.additionalInfo.isPublic) {
                    return $translate.instant('customer.manage-public-dashboards')
                } else {
                    return $translate.instant('customer.manage-customer-dashboards')
                }
            },
            icon: "dashboard"
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('customer.delete') },
            icon: "delete",
            isEnabled: function(customer) {
                return customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
            }
        }
    ];

    var vm = this;

    vm.types = types;

    vm.customerGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteCustomerTitle,
        deleteItemContentFunc: deleteCustomerText,
        deleteItemsTitleFunc: deleteCustomersTitle,
        deleteItemsActionTitleFunc: deleteCustomersActionTitle,
        deleteItemsContentFunc: deleteCustomersText,

        fetchItemsFunc: fetchCustomers,
        saveItemFunc: saveCustomer,
        deleteItemFunc: deleteCustomer,

        getItemTitleFunc: getCustomerTitle,

        itemCardTemplateUrl: customerCard,

        actionsList: customerActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addCustomerTemplate,

        addItemText: function() { return $translate.instant('customer.add-customer-text') },
        noItemsText: function() { return $translate.instant('customer.no-customers-text') },
        itemDetailsText: function(customer) {
            if (customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic)) {
                return $translate.instant('customer.customer-details')
            } else {
                return '';
            }
        },
        isSelectionEnabled: function (customer) {
            return customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
        },
        isDetailsReadOnly: function (customer) {
            return customer && customer.additionalInfo && customer.additionalInfo.isPublic;
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.customerGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.customerGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.openCustomerUsers = openCustomerUsers;
    vm.openCustomerAssets = openCustomerAssets;
    vm.openCustomerDevices = openCustomerDevices;
    vm.openCustomerDashboards = openCustomerDashboards;

    function deleteCustomerTitle(customer) {
        return $translate.instant('customer.delete-customer-title', {customerTitle: customer.title});
    }

    function deleteCustomerText() {
        return $translate.instant('customer.delete-customer-text');
    }

    function deleteCustomersTitle(selectedCount) {
        return $translate.instant('customer.delete-customers-title', {count: selectedCount}, 'messageformat');
    }

    function deleteCustomersActionTitle(selectedCount) {
        return $translate.instant('customer.delete-customers-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteCustomersText() {
        return $translate.instant('customer.delete-customers-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchCustomers(pageLink) {
        return customerService.getCustomers(pageLink);
    }

    function saveCustomer(customer) {
        return customerService.saveCustomer(customer);
    }

    function deleteCustomer(customerId) {
        return customerService.deleteCustomer(customerId);
    }

    function getCustomerTitle(customer) {
        return customer ? customer.title : '';
    }

    function openCustomerUsers($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.users', {customerId: customer.id.id});
    }

    function openCustomerAssets($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.assets', {customerId: customer.id.id});
    }

    function openCustomerDevices($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.devices', {customerId: customer.id.id});
    }

    function openCustomerDashboards($event, customer) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.customers.dashboards', {customerId: customer.id.id});
    }
}
