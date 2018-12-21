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
export default function DeviceGroupConfig($q, $translate, tbDialogs, utils, types, securityTypes, userPermissionsService, userService, deviceService) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();

        var settings = utils.groupSettingsDefaults(types.entityType.device, entityGroup.configuration.settings);

        var groupConfig = {

            tableTitle: entityGroup.name + ': ' + $translate.instant('device.devices'),

            loadEntity: (entityId) => {return deviceService.getDevice(entityId)},
            saveEntity: (entity) => {return deviceService.saveDevice(entity)},
            deleteEntity: (entityId) => {return deviceService.deleteDevice(entityId)},

            addEnabled: () => {
                return settings.enableAdd;
            },

            detailsReadOnly: () => {
                return false;
            },
            assignmentEnabled: () => {
                return settings.enableAssignment;
            },
            manageCredentialsEnabled: () => {
                return settings.enableCredentialsManagement;
            },
            deleteEnabled: () => {
                return settings.enableDelete;
            },
            entitiesDeleteEnabled: () => {
                return settings.enableDelete;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('device.delete-device-title', {deviceName: entity.name});
            },
            deleteEntityContent: (/*entity*/) => {
                return $translate.instant('device.delete-device-text');
            },
            deleteEntitiesTitle: (count) => {
                return $translate.instant('device.delete-devices-title', {count: count}, 'messageformat');
            },
            deleteEntitiesContent: (/*count*/) => {
                return $translate.instant('device.delete-devices-text');
            }
        };

        groupConfig.onManageCredentials = (event, entity, isReadOnly) => {
            tbDialogs.manageDeviceCredentials(event, entity, isReadOnly);
        };

        /*groupConfig.onAssignToCustomer = (event, entity) => {
            tbDialogs.assignDevicesToCustomer(event, [entity.id.id]).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.onUnassignFromCustomer = (event, entity, isPublic) => {
            tbDialogs.unassignDeviceFromCustomer(event, entity, isPublic).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.onMakePublic = (event, entity) => {
            tbDialogs.makeDevicePublic(event, entity).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };*/

        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.readCredentials, entityGroup) &&
            !userPermissionsService.hasGroupEntityPermission(securityTypes.operation.writeCredentials, entityGroup)) {
            groupConfig.actionCellDescriptors = [
                {
                    name: $translate.instant('device.view-credentials'),
                    icon: 'security',
                    isEnabled: () => {
                        return settings.enableCredentialsManagement;
                    },
                    onAction: ($event, entity) => {
                        tbDialogs.manageDeviceCredentials($event, entity, true);
                    }
                }
            ];
        }

        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.writeCredentials, entityGroup)) {
            groupConfig.actionCellDescriptors = [
                {
                    name: $translate.instant('device.manage-credentials'),
                    icon: 'security',
                    isEnabled: () => {
                        return settings.enableCredentialsManagement;
                    },
                    onAction: ($event, entity) => {
                        tbDialogs.manageDeviceCredentials($event, entity, false);
                    }
                }
            ];
        }

/*        groupConfig.groupActionDescriptors = [
            {
                name: $translate.instant('device.assign-devices'),
                icon: "assignment_ind",
                isEnabled: () => {
                    return settings.enableAssignment;
                },
                onAction: (event, entities) => {
                    var deviceIds = [];
                    entities.forEach((entity) => {
                        deviceIds.push(entity.id.id);
                    });
                    tbDialogs.assignDevicesToCustomer(event, deviceIds).then(
                        () => { groupConfig.onEntitiesUpdated(deviceIds, true); }
                    );
                },
            },
            {
                name: $translate.instant('device.unassign-devices'),
                icon: "assignment_return",
                isEnabled: () => {
                    return settings.enableAssignment;
                },
                onAction: (event, entities) => {
                    var deviceIds = [];
                    entities.forEach((entity) => {
                        deviceIds.push(entity.id.id);
                    });
                    tbDialogs.unassignDevicesFromCustomer(event, deviceIds).then(
                        () => { groupConfig.onEntitiesUpdated(deviceIds, true); }
                    );
                },
            }
        ];*/

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }

}