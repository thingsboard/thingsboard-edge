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
export default function DashboardGroupConfig($q, $translate, $state, tbDialogs, utils, types, securityTypes, userPermissionsService, userService, importExport, dashboardService) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();

        var settings = utils.groupSettingsDefaults(types.entityType.dashboard, entityGroup.configuration.settings);

        var groupConfig = {

            tableTitle: entityGroup.name + ': ' + $translate.instant('dashboard.dashboards'),

            loadEntity: (entityId) => {return dashboardService.getDashboard(entityId)},
            saveEntity: (entity) => {return dashboardService.saveDashboard(entity)},
            deleteEntity: (entityId) => {return dashboardService.deleteDashboard(entityId)},

            addEnabled: () => {
                return settings.enableAdd;
            },

            detailsReadOnly: () => {
                return false;
            },
            deleteEnabled: () => {
                return settings.enableDelete;
            },
            entitiesDeleteEnabled: () => {
                return settings.enableDelete;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: entity.title});
            },
            deleteEntityContent: (/*entity*/) => {
                return $translate.instant('dashboard.delete-dashboard-text');
            },
            deleteEntitiesTitle: (count) => {
                return $translate.instant('dashboard.delete-dashboards-title', {count: count}, 'messageformat');
            },
            deleteEntitiesContent: (/*count*/) => {
                return $translate.instant('dashboard.delete-dashboards-text');
            }
        };

        groupConfig.onExportDashboard = (event, entity)  => {
            event.stopPropagation();
            importExport.exportDashboard(entity.id.id);
        };

        groupConfig.onOpenDashboard = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            $state.go('home.dashboardGroups.dashboardGroup.dashboard', {dashboardId: entity.id.id});
        };

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.widgetsBundle, securityTypes.operation.read) &&
            userPermissionsService.hasGenericPermission(securityTypes.resource.widgetType, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors = [
                {
                    name: $translate.instant('dashboard.open-dashboard'),
                    icon: 'dashboard',
                    isEnabled: () => {
                        return true;
                    },
                    onAction: ($event, entity) => {
                        groupConfig.onOpenDashboard($event, entity);
                    }
                }
            ];
        }

        groupConfig.groupActionDescriptors = [
        ];

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }
}