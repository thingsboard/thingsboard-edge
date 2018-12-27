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
/*@ngInject*/
export default function CustomerGroupConfig($q, $translate, $state, tbDialogs, utils, types, securityTypes, userPermissionsService, userService, customerService) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();

        var settings = utils.groupSettingsDefaults(types.entityType.customer, entityGroup.configuration.settings);

        var groupConfig = {

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
            manageCustomersEnabled: () => {
                return settings.enableCustomersManagement;
            },
            manageAssetsEnabled: () => {
                return settings.enableAssetsManagement;
            },
            manageDevicesEnabled: () => {
                return settings.enableDevicesManagement;
            },
            manageEntityViewsEnabled: () => {
                return settings.enableEntityViewsManagement;
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
            $state.go('home.customerGroups.customerGroup.userGroups', {customerId: entity.id.id});
        };

        groupConfig.onManageCustomers = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.customerGroups', {customerId: entity.id.id});
        };

        groupConfig.onManageAssets = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.assetGroups', {customerId: entity.id.id});
        };

        groupConfig.onManageDevices = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.deviceGroups', {customerId: entity.id.id});
        };

        groupConfig.onManageEntityViews = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.entityViewGroups', {customerId: entity.id.id});
        };

        groupConfig.onManageDashboards = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.customerGroups.customerGroup.dashboardGroups', {customerId: entity.id.id});
        };

        groupConfig.actionCellDescriptors = [];

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.userGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('customer.manage-customer-user-groups'),
                    icon: 'account_circle',
                    isEnabled: () => {
                        return settings.enableUsersManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageUsers($event, entity);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.customerGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('customer.manage-customer-groups'),
                    icon: 'supervisor_account',
                    isEnabled: () => {
                        return settings.enableCustomersManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageCustomers($event, entity);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.assetGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('customer.manage-customer-asset-groups'),
                    icon: 'domain',
                    isEnabled: () => {
                        return settings.enableAssetsManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageAssets($event, entity);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.deviceGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('customer.manage-customer-device-groups'),
                    icon: 'devices_other',
                    isEnabled: () => {
                        return settings.enableDevicesManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageDevices($event, entity);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.entityViewGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('customer.manage-customer-entity-view-groups'),
                    icon: 'view_quilt',
                    isEnabled: () => {
                        return settings.enableEntityViewsManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageEntityViews($event, entity);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.dashboardGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('customer.manage-customer-dashboard-groups'),
                    icon: 'dashboard',
                    isEnabled: () => {
                        return settings.enableDashboardsManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageDashboards($event, entity);
                    }
                }
            );
        }

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }

}
