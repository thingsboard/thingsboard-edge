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
/*@ngInject*/
export default function EdgeGroupConfig($q, $translate, $state, $window, tbDialogs, utils, types, userService, edgeService,
                                        importExport, userPermissionsService, securityTypes) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();

        var settings = utils.groupSettingsDefaults(types.entityType.edge, entityGroup.configuration.settings);

        var groupConfig = {

            tableTitle: entityGroup.name + ': ' + $translate.instant('edge.edges'),

            loadEntity: (entityId) => {return edgeService.getEdge(entityId)},
            saveEntity: (entity) => {return edgeService.saveEdge(entity)},
            deleteEntity: (entityId) => {return edgeService.deleteEdge(entityId)},

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
            manageEntityViewsEnabled: () => {
                return settings.enableEntityViewsManagement;
            },
            manageDashboardsEnabled: () => {
                return settings.enableDashboardsManagement;
            },
            manageSchedulerEventsEnabled: () => {
                return settings.enableSchedulerEventsManagement;
            },
            manageRuleChainsEnabled: () => {
                return !manageRuleChainsEnabled();
            },
            deleteEnabled: () => {
                return settings.enableDelete;
            },
            entitiesDeleteEnabled: () => {
                return settings.enableDelete;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('edge.delete-edge-title', {edgeName: entity.name});
            },
            deleteEntityContent: (/*entity*/) => {
                return $translate.instant('edge.delete-edge-text');
            },
            deleteEntitiesTitle: (count) => {
                return $translate.instant('edge.delete-edges-title', {count: count}, 'messageformat');
            },
            deleteEntitiesContent: (/*count*/) => {
                return $translate.instant('edge.delete-edges-text');
            }
        };

        /*groupConfig.onAssignToCustomer = (event, entity) => {
            tbDialogs.assignEntityViewsToCustomer(event, [entity.id.id]).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.onUnassignFromCustomer = (event, entity, isPublic) => {
            tbDialogs.unassignEntityViewFromCustomer(event, entity, isPublic).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.onMakePublic = (event, entity) => {
            tbDialogs.makeEntityViewPublic(event, entity).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };*/

        /* groupConfig.groupActionDescriptors = [
            {
                name: $translate.instant('entity-view.assign-entity-views'),
                icon: "assignment_ind",
                isEnabled: () => {
                    return settings.enableAssignment;
                },
                onAction: (event, entities) => {
                    var entityViewIds = [];
                    entities.forEach((entity) => {
                        entityViewIds.push(entity.id.id);
                    });
                    tbDialogs.assignEntityViewsToCustomer(event, entityViewIds).then(
                        () => { groupConfig.onEntitiesUpdated(entityViewIds, true); }
                    );
                },
            },
            {
                name: $translate.instant('entity-view.unassign-entity-views'),
                icon: "assignment_return",
                isEnabled: () => {
                    return settings.enableAssignment;
                },
                onAction: (event, entities) => {
                    var entityViewIds = [];
                    entities.forEach((entity) => {
                        entityViewIds.push(entity.id.id);
                    });
                    tbDialogs.unassignEntityViewsFromCustomer(event, entityViewIds).then(
                        () => { groupConfig.onEntitiesUpdated(entityViewIds, true); }
                    );
                },
            }
        ];*/

        groupConfig.onManageDevices = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.deviceGroups', createStateParams(entity, types.entityType.device));
            } else {
                $state.go('home.edgeGroups.edgeGroup.deviceGroups', {edgeId: entity.id.id});
            }
        };

        groupConfig.onManageUsers = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.userGroups', createStateParams(entity, types.entityType.user));
            } else {
                $state.go('home.edgeGroups.edgeGroup.userGroups', {edgeId: entity.id.id});
            }
        };

        groupConfig.onManageAssets = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.assetGroups', createStateParams(entity, types.entityType.asset));
            } else {
                $state.go('home.edgeGroups.edgeGroup.assetGroups', {edgeId: entity.id.id});
            }
        };

        groupConfig.onManageEntityViews = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.entityViewGroups', createStateParams(entity, types.entityType.entityView));
            } else {
                $state.go('home.edgeGroups.edgeGroup.entityViewGroups', {edgeId: entity.id.id});
            }
        };

        groupConfig.onManageDashboards = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.dashboardGroups', createStateParams(entity, types.entityType.dashboard));
            } else {
                $state.go('home.edgeGroups.edgeGroup.dashboardGroups', {edgeId: entity.id.id});
            }
        };

        groupConfig.onManageRuleChains = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.ruleChains', createStateParams(entity, types.entityType.rulechain));
            } else {
                $state.go('home.edgeGroups.edgeGroup.ruleChains', {edgeId: entity.id.id});
            }
        };

        groupConfig.onManageSchedulerEvents = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if ((params.hierarchyView && params.hierarchyCallbacks.edgeGroupsSelected) || params.groupType === types.entityType.customer) {
                $state.go('home.customerGroups.customerGroup.edgeGroups.edgeGroup.schedulerEvents', createStateParams(entity, types.entityType.schedulerEvent));
            } else {
                $state.go('home.edgeGroups.edgeGroup.schedulerEvents', {edgeId: entity.id.id});
            }

        };

        groupConfig.onImportEdges = (event)  => {
            var entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
            var customerId = null;
            if (entityGroup.ownerId.entityType === types.entityType.customer) {
                customerId = entityGroup.ownerId;
            }
            importExport.importEntities(event, customerId, types.entityType.edge, entityGroupId).then(
                function() {
                    groupConfig.onEntityAdded();
                });
        };

        groupConfig.headerActionDescriptors = [
        ];

        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.create, entityGroup)) {
            groupConfig.headerActionDescriptors.push(
                {
                    name: $translate.instant('edge.import'),
                    icon: 'file_upload',
                    isEnabled: () => {
                        return groupConfig.addEnabled();
                    },
                    onAction: ($event) => {
                        groupConfig.onImportEdges($event);
                    }
                }
            );
        }

        groupConfig.actionCellDescriptors = [];

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.userGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('edge.manage-edge-user-groups'),
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

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.assetGroup, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('edge.manage-edge-asset-groups'),
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
                    name: $translate.instant('edge.manage-edge-device-groups'),
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
                    name: $translate.instant('edge.manage-edge-entity-view-groups'),
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
                    name: $translate.instant('edge.manage-edge-dashboard-groups'),
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

        if (manageRuleChainsEnabled()) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('edge.manage-edge-rule-chains'),
                    icon: 'settings_ethernet',
                    isEnabled: () => {
                        return true;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageRuleChains($event, entity);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.schedulerEvent, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('edge.manage-edge-scheduler-events'),
                    icon: 'schedule',
                    isEnabled: () => {
                        return settings.enableSchedulerEventsManagement;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onManageSchedulerEvents($event, entity);
                    }
                }
            );
        }

        function createStateParams(entity, targetGroupType) {
            return {
                entityGroupId:params.entityGroupId,
                edgeId: entity.id.id,
                customerId: params.customerId,
                childEntityGroupId: params.childEntityGroupId,
                entityGroupScope: params.entityGroupScope,
                targetGroupType: targetGroupType
            };
        }

        function manageRuleChainsEnabled() {
            return userPermissionsService.hasGenericPermission(securityTypes.resource.edge, securityTypes.operation.write);
        }

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }
}
