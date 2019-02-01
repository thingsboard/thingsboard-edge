/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import publicDashboardLinkDialogTemplate from './public-dashboard-link-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/*@ngInject*/
export default function DashboardGroupConfig($q, $translate, $state, $window, $document, $mdDialog,
                                             tbDialogs, utils, types, securityTypes, userPermissionsService, userService, importExport, dashboardService) {

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

        groupConfig.onImportDashboard = (event)  => {
            var entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
            importExport.importDashboard(event, entityGroupId).then(
                function() {
                    groupConfig.onEntityAdded();
                }
            );
        };

        groupConfig.onOpenDashboard = (event, entity) => {
            if (event) {
                event.stopPropagation();
            }
            if (entityGroup.parentEntityGroup) {
                var stateParams = {dashboardId: entity.id.id};
                if (params.hierarchyView) {
                    stateParams.customerId = params.customerId;
                    stateParams.entityGroupId = params.entityGroupId;
                    stateParams.groupType = params.groupType;
                    stateParams.childEntityGroupId = params.childEntityGroupId;
                    stateParams.childGroupType = params.childGroupType;
                    var href = $state.href('home.customerGroups.customerGroup.dashboardGroups.dashboardGroup.dashboard', stateParams, {absolute: true});
                    $window.open(href, '_blank');
                } else {
                    $state.go('home.customerGroups.customerGroup.dashboardGroups.dashboardGroup.dashboard', stateParams);
                }
            } else {
                $state.go('home.dashboardGroups.dashboardGroup.dashboard', {dashboardId: entity.id.id});
            }
        };

        groupConfig.actionCellDescriptors = [];

        if (entityGroup.additionalInfo && entityGroup.additionalInfo.isPublic) {
            groupConfig.actionCellDescriptors.push(
                {
                    name: $translate.instant('dashboard.public-dashboard-link'),
                    icon: 'link',
                    isEnabled: () => {
                        return true;
                    },
                    onAction: ($event, entity) => {
                        openPublicDashboardLinkDialog($event, entity, entityGroup);
                    }
                }
            );
        }

        if (userPermissionsService.hasGenericPermission(securityTypes.resource.widgetsBundle, securityTypes.operation.read) &&
            userPermissionsService.hasGenericPermission(securityTypes.resource.widgetType, securityTypes.operation.read)) {
            groupConfig.actionCellDescriptors.push(
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
            );
        }

        groupConfig.actionCellDescriptors.push(
            {
                name: $translate.instant('dashboard.export'),
                icon: 'file_download',
                isEnabled: () => {
                    return true;
                },
                onAction: ($event, entity) => {
                    groupConfig.onExportDashboard($event, entity);
                }
            }
        );

        groupConfig.groupActionDescriptors = [
        ];

        groupConfig.headerActionDescriptors = [
        ];

        if (userPermissionsService.hasGroupEntityPermission(securityTypes.operation.create, entityGroup)) {
            groupConfig.headerActionDescriptors.push(
                {
                    name: $translate.instant('dashboard.import'),
                    icon: 'file_upload',
                    isEnabled: () => {
                        return groupConfig.addEnabled();
                    },
                    onAction: ($event) => {
                        groupConfig.onImportDashboard($event);
                    }
                }
            );
        }

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }

    function openPublicDashboardLinkDialog($event, dashboard, entityGroup) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: 'PublicDashboardLinkDialogController',
            controllerAs: 'vm',
            templateUrl: publicDashboardLinkDialogTemplate,
            locals: {dashboard: dashboard, entityGroup: entityGroup},
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then(function () {});
    }
}
