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
/* eslint-disable import/no-unresolved, import/default */

import entityGroupsTemplate from './entity-groups.tpl.html';
import entityGroupTemplate from './entity-group.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityGroupRoutes($stateProvider, types) {
    $stateProvider
        .state('home.deviceGroups', {
            url: '/deviceGroups',
            params: {'groupType': types.entityType.device, 'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: entityGroupsTemplate,
                    controller: 'EntityGroupsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'entity-group.device-groups'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "entity-group.device-groups"}'
            }
        })
        .state('home.deviceGroups.deviceGroup', {
            url: '/:entityGroupId',
            reloadOnSearch: false,
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: entityGroupTemplate,
                    controller: 'EntityGroupController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entityGroup:
                    /*@ngInject*/
                    function($stateParams, $q, entityGroupService, deviceGroupConfig) {
                        return constructGroupConfig($stateParams, $q, entityGroupService, deviceGroupConfig);
                    }
            },
            data: {
                searchEnabled: false,
                pageTitle: 'entity-group.device-group'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "{{ vm.entityGroup.name }}", "translate": "false"}'
            }
        })
        .state('home.assetGroups', {
            url: '/assetGroups',
            params: {'groupType': types.entityType.asset, 'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: entityGroupsTemplate,
                    controller: 'EntityGroupsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'entity-group.asset-groups'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "entity-group.asset-groups"}'
            }
        })
        .state('home.assetGroups.assetGroup', {
            url: '/:entityGroupId',
            reloadOnSearch: false,
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: entityGroupTemplate,
                    controller: 'EntityGroupController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                entityGroup:
                /*@ngInject*/
                    function($stateParams, $q, entityGroupService, assetGroupConfig) {
                        return constructGroupConfig($stateParams, $q, entityGroupService, assetGroupConfig);
                    }
            },
            data: {
                searchEnabled: false,
                pageTitle: 'entity-group.asset-group'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "{{ vm.entityGroup.name }}", "translate": "false"}'
            }
        });

    function constructGroupConfig($stateParams, $q, entityGroupService, entityGroupConfigFactory) {
        var deferred = $q.defer();
        entityGroupService.getEntityGroup($stateParams.entityGroupId).then(
            (entityGroup) => {
                entityGroupConfigFactory.createConfig($stateParams, entityGroup).then(
                    (entityGroupConfig) => {
                        entityGroup.entityGroupConfig = entityGroupConfig;
                        deferred.resolve(entityGroup);
                    },
                    () => {
                        deferred.reject();
                    }
                );
            },
            () => {
                deferred.reject();
            }
        );
        return deferred.promise;
    }
}
