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
export default function EdgeGroupConfig($q, $translate, tbDialogs, utils, types, userService, edgeService,
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

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }
}