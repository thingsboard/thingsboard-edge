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

/*@ngInject*/
export default function CustomerGroupConfig($q, $translate, $state, tbDialogs, utils, types, userService, customerService) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();

        var authority = userService.getAuthority();

        var entityScope = 'tenant';
        if (authority === 'CUSTOMER_USER') {
            entityScope = 'customer_user';
        }

        var settings = utils.groupSettingsDefaults(types.entityType.customer, entityGroup.configuration.settings);

        var groupConfig = {

            entityScope: entityScope,

            tableTitle: entityGroup.name + ': ' + $translate.instant('customer.customers'),

            loadEntity: (entityId) => {return customerService.getCustomer(entityId)},
            saveEntity: (entity) => {return customerService.saveCustomer(entity)},
            deleteEntity: (entityId) => {return customerService.deleteCustomer(entityId)},

            addEnabled: () => {
                return settings.enableAdd;
            },

            detailsReadOnly: () => {
                return false;
            },
            manageUsersEnabled: () => {
                return settings.enableUsersManagement;
            },
            manageAssetsEnabled: () => {
                return settings.enableAssetsManagement;
            },
            manageDevicesEnabled: () => {
                return settings.enableDevicesManagement;
            },
            manageDashboardsEnabled: () => {
                return settings.enableDashboardsManagement;
            },
            deleteEnabled: () => {
                return settings.enableDelete;
            },
            entitiesDeleteEnabled: () => {
                return settings.enableDelete;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('customer.delete-customer-title', {customerTitle: entity.name});
            },
            deleteEntityContent: (/*entity*/) => {
                return $translate.instant('customer.delete-customer-text');
            },
            deleteEntitiesTitle: (count) => {
                return $translate.instant('customer.delete-customers-title', {count: count}, 'messageformat');
            },
            deleteEntitiesContent: (/*count*/) => {
                return $translate.instant('customer.delete-customers-text');
            }
        };

        groupConfig.onManageUsers = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.users', {customerId: entity.id.id});
        };

        groupConfig.onManageAssets = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.assets', {customerId: entity.id.id});
        };

        groupConfig.onManageDevices = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.devices', {customerId: entity.id.id});
        };

        groupConfig.onManageDashboards = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.dashboards', {customerId: entity.id.id});
        };

        groupConfig.actionCellDescriptors = [
            {
                name: $translate.instant('customer.manage-customer-users'),
                icon: 'account_circle',
                isEnabled: () => {
                    return settings.enableUsersManagement;
                },
                onAction: ($event, entity) => {
                    groupConfig.onManageUsers($event, entity);
                }
            },
            {
                name: $translate.instant('customer.manage-customer-assets'),
                icon: 'domain',
                isEnabled: () => {
                    return settings.enableAssetsManagement;
                },
                onAction: ($event, entity) => {
                    groupConfig.onManageAssets($event, entity);
                }
            },
            {
                name: $translate.instant('customer.manage-customer-devices'),
                icon: 'devices_other',
                isEnabled: () => {
                    return settings.enableDevicesManagement;
                },
                onAction: ($event, entity) => {
                    groupConfig.onManageDevices($event, entity);
                }
            },
            {
                name: $translate.instant('customer.manage-customer-dashboards'),
                icon: 'dashboard',
                isEnabled: () => {
                    return settings.enableDashboardsManagement;
                },
                onAction: ($event, entity) => {
                    groupConfig.onManageDashboards($event, entity);
                }
            }
        ];

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }

}
