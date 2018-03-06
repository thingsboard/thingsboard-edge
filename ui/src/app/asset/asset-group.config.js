/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
export default function AssetGroupConfig($q, $translate, tbDialogs, utils, types, userService, assetService) {

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

        var settings = utils.groupSettingsDefaults(types.entityType.asset, entityGroup.configuration.settings);

        var groupConfig = {

            entityScope: entityScope,

            tableTitle: entityGroup.name + ': ' + $translate.instant('asset.assets'),

            loadEntity: (entityId) => {return assetService.getAsset(entityId)},
            saveEntity: (entity) => {return assetService.saveAsset(entity)},
            deleteEntity: (entityId) => {return assetService.deleteAsset(entityId)},

            addEnabled: () => {
                return settings.enableAdd;
            },

            detailsReadOnly: () => {
                return false;
            },
            assignmentEnabled: () => {
                return settings.enableAssignment;
            },
            deleteEnabled: () => {
                return settings.enableDelete;
            },
            entitiesDeleteEnabled: () => {
                return settings.enableDelete;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('asset.delete-asset-title', {assetName: entity.name});
            },
            deleteEntityContent: (/*entity*/) => {
                return $translate.instant('asset.delete-asset-text');
            },
            deleteEntitiesTitle: (count) => {
                return $translate.instant('asset.delete-assets-title', {count: count}, 'messageformat');
            },
            deleteEntitiesContent: (/*count*/) => {
                return $translate.instant('asset.delete-assets-text');
            }
        };

        groupConfig.onAssignToCustomer = (event, entity) => {
            tbDialogs.assignAssetsToCustomer(event, [entity.id.id]).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.onUnassignFromCustomer = (event, entity, isPublic) => {
            tbDialogs.unassignAssetFromCustomer(event, entity, isPublic).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.onMakePublic = (event, entity) => {
            tbDialogs.makeAssetPublic(event, entity).then(
                () => { groupConfig.onEntityUpdated(entity.id.id, true); }
            );
        };

        groupConfig.groupActionDescriptors = [
            {
                name: $translate.instant('asset.assign-assets'),
                icon: "assignment_ind",
                isEnabled: () => {
                    return settings.enableAssignment;
                },
                onAction: (event, entities) => {
                    var assetIds = [];
                    entities.forEach((entity) => {
                        assetIds.push(entity.id.id);
                    });
                    tbDialogs.assignAssetsToCustomer(event, assetIds).then(
                        () => { groupConfig.onEntitiesUpdated(assetIds, true); }
                    );
                },
            },
            {
                name: $translate.instant('asset.unassign-assets'),
                icon: "assignment_return",
                isEnabled: () => {
                    return settings.enableAssignment;
                },
                onAction: (event, entities) => {
                    var assetIds = [];
                    entities.forEach((entity) => {
                        assetIds.push(entity.id.id);
                    });
                    tbDialogs.unassignAssetsFromCustomer(event, assetIds).then(
                        () => { groupConfig.onEntitiesUpdated(assetIds, true); }
                    );
                },
            }
        ];

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }
}