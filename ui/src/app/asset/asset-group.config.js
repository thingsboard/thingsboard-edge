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
export default function AssetGroupConfig($q, $translate, utils, assetService) {

    var service = {
        createConfig: createConfig
    }

    return service;

    function createConfig(params, entityGroup) {
        var deferred = $q.defer();
        var groupConfig = {
            tableTitle: entityGroup.name + ': ' + $translate.instant('asset.assets'),

            loadEntity: (entityId) => {return assetService.getAsset(entityId)},
            saveEntity: (entity) => {return assetService.saveAsset(entity)},
            deleteEntity: (entityId) => {return assetService.deleteAsset(entityId)},

            addEnabled: () => {
                return true;
            },

            detailsReadOnly: () => {
                return false;
            },
            deleteEnabled: () => {
                return true;
            },
            entitiesDeleteEnabled: () => {
                return true;
            },
            deleteEntityTitle: (entity) => {
                return $translate.instant('asset.delete-asset-title', {name: entity.name});
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

        utils.groupConfigDefaults(groupConfig);

        deferred.resolve(groupConfig);
        return deferred.promise;
    }
}