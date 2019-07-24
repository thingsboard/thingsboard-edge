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
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.entity', [thingsboardTypes])
    .factory('entityService', EntityService)
    .name;

/*@ngInject*/
function EntityService($http, $q, $filter, $translate, $log, userService, deviceService,
                       assetService, tenantService, customerService,
                       ruleChainService, dashboardService, entityGroupService,
                       converterService, integrationService, schedulerEventService, blobEntityService,
                       entityRelationService, attributeService, entityViewService, roleService, userPermissionsService,
                       securityTypes, types, utils) {
    var service = {
        getEntity: getEntity,
        saveEntity: saveEntity,
        saveGroupEntity: saveGroupEntity,
        getEntities: getEntities,
        getEntitiesByNameFilter: getEntitiesByNameFilter,
        resolveAlias: resolveAlias,
        resolveAliasFilter: resolveAliasFilter,
        checkEntityAlias: checkEntityAlias,
        filterAliasByEntityTypes: filterAliasByEntityTypes,
        getAliasFilterTypesByEntityTypes: getAliasFilterTypesByEntityTypes,
        prepareAllowedEntityTypesList: prepareAllowedEntityTypesList,
        getEntityKeys: getEntityKeys,
        createDatasourcesFromSubscriptionsInfo: createDatasourcesFromSubscriptionsInfo,
        createAlarmSourceFromSubscriptionInfo: createAlarmSourceFromSubscriptionInfo,
        getRelatedEntities: getRelatedEntities,
        saveRelatedEntity: saveRelatedEntity,
        saveEntityParameters: saveEntityParameters,
        getRelatedEntity: getRelatedEntity,
        deleteRelatedEntity: deleteRelatedEntity,
        moveEntity: moveEntity,
        copyEntity: copyEntity,
        getEntityGroupEntities: getEntityGroupEntities
    };

    return service;

    function getEntityPromise(entityType, entityId, config) {
        var promise;
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.getDevice(entityId, true, config);
                break;
            case types.entityType.asset:
                promise = assetService.getAsset(entityId, true, config);
                break;
            case types.entityType.entityView:
                promise = entityViewService.getEntityView(entityId, true, config);
                break;
            case types.entityType.tenant:
                promise = tenantService.getTenant(entityId, config);
                break;
            case types.entityType.customer:
                promise = customerService.getCustomer(entityId, config);
                break;
            case types.entityType.dashboard:
                if (config && config.loadEntityDetails) {
                    promise = dashboardService.getDashboard(entityId, config);
                } else {
                    promise = dashboardService.getDashboardInfo(entityId, config);
                }
                break;
            case types.entityType.user:
                promise = userService.getUser(entityId, true, config);
                break;
            case types.entityType.rulechain:
                promise = ruleChainService.getRuleChain(entityId, config);
                break;
            case types.entityType.alarm:
                $log.error('Get Alarm Entity is not implemented!');
                break;
            case types.entityType.converter:
                promise = converterService.getConverter(entityId, config);
                break;
            case types.entityType.integration:
                promise = integrationService.getIntegration(entityId, config);
                break;
            case types.entityType.schedulerEvent:
                promise = schedulerEventService.getSchedulerEventInfo(entityId, config);
                break;
            case types.entityType.blobEntity:
                promise = blobEntityService.getBlobEntityInfo(entityId, config);
                break;
            case types.entityType.role:
                promise = roleService.getRole(entityId, true, config);
                break;
        }
        return promise;
    }

    function saveEntityPromise(entity, config) {
        var promise;
        var entityType = entity.id.entityType;
        if (!entity.id.id) {
            delete entity.id;
        }
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.saveDevice(entity);
                break;
            case types.entityType.asset:
                promise = assetService.saveAsset(entity, true, config);
                break;
            case types.entityType.entityView:
                promise = entityViewService.saveEntityView(entity);
                break;
            case types.entityType.tenant:
                promise = tenantService.saveTenant(entity);
                break;
            case types.entityType.customer:
                promise = customerService.saveCustomer(entity);
                break;
            case types.entityType.dashboard:
                promise = dashboardService.saveDashboard(entity);
                break;
            case types.entityType.user:
                promise = userService.saveUser(entity);
                break;
            case types.entityType.rulechain:
                promise = ruleChainService.saveRuleChain(entity);
                break;
            case types.entityType.alarm:
                $log.error('Save Alarm Entity is not implemented!');
                break;
            case types.entityType.converter:
                promise = converterService.saveConverter(entity);
                break;
            case types.entityType.integration:
                promise = integrationService.saveIntegration(entity);
                break;
            case types.entityType.schedulerEvent:
                promise = schedulerEventService.saveSchedulerEvent(entity);
                break;
            case types.entityType.role:
                promise = roleService.saveRole(entity);
                break;
        }
        return promise;
    }

    function getEntity(entityType, entityId, config) {
        var deferred = $q.defer();
        var promise = getEntityPromise(entityType, entityId, config);
        if (promise) {
            promise.then(
                function success(result) {
                    deferred.resolve(result);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function saveEntity(entity, config, entityGroupId) {
        var deferred = $q.defer();
        var promise = saveEntityPromise(entity, config, entityGroupId);
        if (promise) {
            promise.then(
                function success(result) {
                    deferred.resolve(result);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function saveGroupEntityPromise(entity, entityGroupId, config) {
        var promise;
        var entityType = entity.id.entityType;
        if (!entity.id.id) {
            delete entity.id;
        }
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.saveDevice(entity, entityGroupId);
                break;
            case types.entityType.asset:
                promise = assetService.saveAsset(entity, false, config, entityGroupId);
                break;
            case types.entityType.entityView:
                promise = entityViewService.saveEntityView(entity, entityGroupId);
                break;
            case types.entityType.customer:
                promise = customerService.saveCustomer(entity, entityGroupId);
                break;
            case types.entityType.dashboard:
                promise = dashboardService.saveDashboard(entity, entityGroupId);
                break;
            case types.entityType.user:
                promise = userService.saveUser(entity, false, entityGroupId);
                break;
        }
        return promise;
    }

    function saveGroupEntity(entity, entityGroupId, config) {
        var deferred = $q.defer();
        var promise = saveGroupEntityPromise(entity, entityGroupId, config);
        if (promise) {
            promise.then(
                function success(result) {
                    deferred.resolve(result);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    /*function getEntitiesByIdsPromise(fetchEntityFunction, entityIds) {
        var tasks = [];
        var deferred = $q.defer();
        for (var i=0;i<entityIds.length;i++) {
            tasks.push(fetchEntityFunction(entityIds[i]));
        }
        $q.all(tasks).then(
            function success(entities) {
                if (entities) {
                    entities.sort(function (entity1, entity2) {
                        var id1 = entity1.id.id;
                        var id2 = entity2.id.id;
                        var index1 = entityIds.indexOf(id1);
                        var index2 = entityIds.indexOf(id2);
                        return index1 - index2;
                    });
                    deferred.resolve(entities);
                } else {
                    deferred.resolve([]);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }*/

    function getEntitiesPromise(entityType, entityIds, config) {
        var promise;
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.getDevices(entityIds, config);
                break;
            case types.entityType.asset:
                promise = assetService.getAssets(entityIds, config);
                break;
            case types.entityType.entityView:
                promise = entityViewService.getEntityViews(entityIds, config);
                break;
            case types.entityType.tenant:
                promise = tenantService.getTenantsByIds(entityIds, config);
                break;
            case types.entityType.customer:
                promise = customerService.getCustomersByIds(entityIds, config);
                break;
            case types.entityType.dashboard:
                promise = dashboardService.getDashboards(entityIds, config);
                break;
            case types.entityType.user:
                promise = userService.getUsers(entityIds, config);
                break;
            case types.entityType.alarm:
                $log.error('Get Alarm Entity is not implemented!');
                break;
            case types.entityType.entityGroup:
                promise = entityGroupService.getEntityGroupsByIds(entityIds, config);
                break;
            case types.entityType.converter:
                promise = entityGroupService.getConvertersByIds(entityIds, config);
                break;
            case types.entityType.integration:
                promise = integrationService.getIntegrationsByIds(entityIds, config);
                break;
            case types.entityType.schedulerEvent:
                promise = schedulerEventService.getSchedulerEventsByIds(entityIds, config);
                break;
            case types.entityType.blobEntity:
                promise = blobEntityService.getBlobEntitiesByIds(entityIds, config);
                break;
            case types.entityType.role:
                promise = roleService.getRolesByIds(entityIds, config);
                break;
        }
        return promise;
    }

    function getEntities(entityType, entityIds, config) {
        var deferred = $q.defer();
        var promise = getEntitiesPromise(entityType, entityIds, config);
        if (promise) {
            promise.then(
                function success(result) {
                    deferred.resolve(result);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function getSingleTenantByPageLinkPromise(pageLink, config) {
        var user = userService.getCurrentUser();
        var tenantId = user.tenantId;
        var deferred = $q.defer();
        tenantService.getTenant(tenantId, config).then(
            function success(tenant) {
                var tenantName = tenant.name;
                var result = {
                    data: [],
                    nextPageLink: pageLink,
                    hasNext: false
                };
                if (tenantName.toLowerCase().startsWith(pageLink.textSearch)) {
                    result.data.push(tenant);
                }
                deferred.resolve(result);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getEntitiesByPageLinkPromise(entityType, pageLink, config, subType) {
        var promise;
        var user = userService.getCurrentUser();
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.getUserDevices(pageLink, config, subType);
                break;
            case types.entityType.asset:
                promise = assetService.getUserAssets(pageLink, config, subType);
                break;
            case types.entityType.entityView:
                promise = entityViewService.getUserEntityViews(pageLink, config, subType);
                break;
            case types.entityType.tenant:
                if (user.authority === 'TENANT_ADMIN') {
                    promise = getSingleTenantByPageLinkPromise(pageLink, config);
                } else {
                    promise = tenantService.getTenants(pageLink, config);
                }
                break;
            case types.entityType.customer:
                promise = customerService.getUserCustomers(pageLink, config);
                break;
            case types.entityType.rulechain:
                promise = ruleChainService.getRuleChains(pageLink, config);
                break;
            case types.entityType.dashboard:
                promise = dashboardService.getUserDashboards(null, null, pageLink, config);
                break;
            case types.entityType.user:
                promise = userService.getUserUsers(pageLink, config);
                break;
            case types.entityType.alarm:
                $log.error('Get Alarm Entities is not implemented!');
                break;
            case types.entityType.entityGroup:
                promise = entityGroupService.getEntityGroupsByPageLink(pageLink, subType, true, config);
                break;
            case types.entityType.converter:
                promise = converterService.getConverters(pageLink, config);
                break;
            case types.entityType.integration:
                promise = integrationService.getIntegrations(pageLink, config);
                break;
            case types.entityType.schedulerEvent:
                var deferred = $q.defer();
                schedulerEventService.getSchedulerEvents(null, false, config).then(
                    (schedulerEvents) => {
                        utils.filterSearchTextEntities(schedulerEvents, 'name', pageLink, deferred);
                    },
                    () => {
                        deferred.reject();
                    }
                );
                promise = deferred.promise;
                break;
            case types.entityType.blobEntity:
                deferred = $q.defer();
                blobEntityService.getBlobEntities({limit: 2147483647}, null, false, config).then(
                    (blobEntitiesData) => {
                        utils.filterSearchTextEntities(blobEntitiesData.data, 'name', pageLink, deferred);
                    },
                    () => {
                       deferred.reject();
                    }
                );
                promise = deferred.promise;
                break;
            case types.entityType.role:
                promise = roleService.getRoles(pageLink, config, subType);
                break;
        }
        return promise;
    }

    function getEntitiesByPageLink(entityType, pageLink, config, subType, data, deferred) {
        var promise = getEntitiesByPageLinkPromise(entityType, pageLink, config, subType);
        if (promise) {
            promise.then(
                function success(result) {
                    data = data.concat(result.data);
                    if (result.hasNext) {
                        pageLink = result.nextPageLink;
                        getEntitiesByPageLink(entityType, pageLink, config, subType, data, deferred);
                    } else {
                        if (data && data.length > 0) {
                            deferred.resolve(data);
                        } else {
                            deferred.resolve(null);
                        }
                    }
                },
                function fail() {
                    deferred.resolve(null);
                }
            );
        } else {
            deferred.resolve(null);
        }
    }

    function getEntitiesByNameFilter(entityType, entityNameFilter, limit, config, subType) {
        var deferred = $q.defer();
        var pageLink = {limit: limit, textSearch: entityNameFilter};
        if (limit == -1) { // all
            var data = [];
            pageLink.limit = 100;
            getEntitiesByPageLink(entityType, pageLink, config, subType, data, deferred);
        } else {
            var promise = getEntitiesByPageLinkPromise(entityType, pageLink, config, subType);
            if (promise) {
                promise.then(
                    function success(result) {
                        if (result.data && result.data.length > 0) {
                            deferred.resolve(result.data);
                        } else {
                            deferred.resolve(null);
                        }
                    },
                    function fail() {
                        deferred.resolve(null);
                    }
                );
            } else {
                deferred.resolve(null);
            }
        }
        return deferred.promise;
    }

    function getEntityGroupEntitiesByPageLink(entityGroupId, pageLink, config, data, deferred) {
        var promise = entityGroupService.getEntityGroupEntities(entityGroupId, pageLink, true, config);
        if (promise) {
            promise.then(
                function success(result) {
                    data = data.concat(result.data);
                    if (result.hasNext) {
                        pageLink = result.nextPageLink;
                        getEntityGroupEntitiesByPageLink(entityGroupId, pageLink, config, data, deferred);
                    } else {
                        if (data && data.length > 0) {
                            deferred.resolve(data);
                        } else {
                            deferred.resolve(null);
                        }
                    }
                },
                function fail() {
                    deferred.resolve(null);
                }
            );
        } else {
            deferred.resolve(null);
        }
    }

    function getEntityGroupEntities(entityGroupId, limit, config) {
        var deferred = $q.defer();
        var pageLink = {limit: limit};
        if (limit == -1) { // all
            var data = [];
            pageLink.limit = 100;
            getEntityGroupEntitiesByPageLink(entityGroupId, pageLink, config, data, deferred);
        } else {
            var promise = entityGroupService.getEntityGroupEntities(entityGroupId, pageLink, true, config);
            if (promise) {
                promise.then(
                    function success(result) {
                        if (result.data && result.data.length > 0) {
                            deferred.resolve(result.data);
                        } else {
                            deferred.resolve(null);
                        }
                    },
                    function fail() {
                        deferred.resolve(null);
                    }
                );
            } else {
                deferred.resolve(null);
            }
        }
        return deferred.promise;
    }

    function entityToEntityInfo(entity) {
        return {
            origEntity: entity,
            name: entity.name,
            label: entity.label?entity.label:"",
            entityType: entity.id.entityType,
            id: entity.id.id,
            entityDescription: entity.additionalInfo?entity.additionalInfo.description:""
        };
    }

    function entityRelationInfoToEntityInfo(entityRelationInfo, direction) {
        var deferred = $q.defer();
        var entityId = direction == types.entitySearchDirection.from ? entityRelationInfo.to : entityRelationInfo.from;
        getEntity(entityId.entityType, entityId.id, {ignoreLoading: true}).then(
            function success(entity) {
                deferred.resolve(entityToEntityInfo(entity));
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function entitiesToEntitiesInfo(entities) {
        var entitiesInfo = [];
        if (entities) {
            for (var d = 0; d < entities.length; d++) {
                entitiesInfo.push(entityToEntityInfo(entities[d]));
            }
        }
        return entitiesInfo;
    }

    function entityRelationInfosToEntitiesInfo(entityRelations, direction) {
        var deferred = $q.defer();
        var entitiesInfoTaks = [];
        if (entityRelations) {
            for (var d = 0; d < entityRelations.length; d++) {
                entitiesInfoTaks.push(entityRelationInfoToEntityInfo(entityRelations[d], direction));
            }
        }
        $q.all(entitiesInfoTaks).then(
            function success(entitiesInfo) {
                deferred.resolve(entitiesInfo);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }


    function resolveAlias(entityAlias, stateParams) {
        var deferred = $q.defer();
        var filter = entityAlias.filter;
        resolveAliasFilter(filter, stateParams, -1, false).then(
            function (result) {
                var aliasInfo = {
                    alias: entityAlias.alias,
                    stateEntity: result.stateEntity,
                    entityParamName: result.entityParamName,
                    resolveMultiple: filter.resolveMultiple
                };
                aliasInfo.resolvedEntities = result.entities;
                aliasInfo.currentEntity = null;
                if (aliasInfo.resolvedEntities.length) {
                    aliasInfo.currentEntity = aliasInfo.resolvedEntities[0];
                }
                deferred.resolve(aliasInfo);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function resolveAliasEntityId(entityType, id) {
        var entityId = {
            entityType: entityType,
            id: id
        };
        if (entityType == types.aliasEntityType.current_customer) {
            var user = userService.getCurrentUser();
            entityId.entityType = types.entityType.customer;
            if (user.authority === 'CUSTOMER_USER') {
                entityId.id = user.customerId;
            }
        }
        return entityId;
    }

    function getStateEntityId(filter, stateParams) {
        var entityId = null;
        if (stateParams) {
            if (filter.stateEntityParamName && filter.stateEntityParamName.length) {
                if (stateParams[filter.stateEntityParamName]) {
                    entityId = stateParams[filter.stateEntityParamName].entityId;
                }
            } else {
                entityId = stateParams.entityId;
            }
        }
        if (!entityId) {
            if (filter.type == types.aliasFilterType.entityGroup.value && filter.defaultStateEntityGroup) {
                entityId = {
                    entityType: types.entityType.entityGroup,
                    id: filter.defaultStateEntityGroup
                }
            } else {
                entityId = filter.defaultStateEntity;
            }
        }
        if (entityId) {
            entityId = resolveAliasEntityId(entityId.entityType, entityId.id);
        }
        return entityId;
    }

    function resolveAliasFilter(filter, stateParams, maxItems, failOnEmpty) {
        var deferred = $q.defer();
        var result = {
            entities: [],
            stateEntity: false
        };
        if (filter.stateEntityParamName && filter.stateEntityParamName.length) {
            result.entityParamName = filter.stateEntityParamName;
        }
        var stateEntityId = getStateEntityId(filter, stateParams);
        switch (filter.type) {
            case types.aliasFilterType.singleEntity.value:
                var aliasEntityId = resolveAliasEntityId(filter.singleEntity.entityType, filter.singleEntity.id);
                getEntity(aliasEntityId.entityType, aliasEntityId.id, {ignoreLoading: true, ignoreErrors: true}).then(
                    function success(entity) {
                        result.entities = entitiesToEntitiesInfo([entity]);
                        deferred.resolve(result);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.entityGroup.value:
                result.stateEntity = filter.groupStateEntity;
                var entityGroup;
                if (result.stateEntity && stateEntityId) {
                    entityGroup = stateEntityId.id;
                } else if (!result.stateEntity) {
                    entityGroup = filter.entityGroup;
                }
                getEntityGroupEntities(entityGroup, maxItems, {ignoreLoading: true, ignoreErrors: true}).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.entityList.value:
                getEntities(filter.entityType, filter.entityList, {ignoreLoading: true, ignoreErrors: true}).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.entityName.value:
                getEntitiesByNameFilter(filter.entityType, filter.entityNameFilter, maxItems, {ignoreLoading: true, ignoreErrors: true}).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;

            case types.aliasFilterType.entityGroupList.value:
                getEntities(types.entityType.entityGroup, filter.entityGroupList, {ignoreLoading: true, ignoreErrors: true}).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.entityGroupName.value:
                getEntitiesByNameFilter(types.entityType.entityGroup, filter.entityGroupNameFilter, maxItems, {ignoreLoading: true, ignoreErrors: true}, filter.groupType).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.stateEntity.value:
                result.stateEntity = true;
                if (stateEntityId) {
                    getEntity(stateEntityId.entityType, stateEntityId.id, {ignoreLoading: true, ignoreErrors: true}).then(
                        function success(entity) {
                            result.entities = entitiesToEntitiesInfo([entity]);
                            deferred.resolve(result);
                        },
                        function fail() {
                            deferred.resolve(result);
                        }
                    );
                } else {
                    deferred.resolve(result);
                }
                break;
            case types.aliasFilterType.assetType.value:
                getEntitiesByNameFilter(types.entityType.asset, filter.assetNameFilter, maxItems, {ignoreLoading: true, ignoreErrors: true}, filter.assetType).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.deviceType.value:
                getEntitiesByNameFilter(types.entityType.device, filter.deviceNameFilter, maxItems, {ignoreLoading: true, ignoreErrors: true}, filter.deviceType).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.entityViewType.value:
                getEntitiesByNameFilter(types.entityType.entityView, filter.entityViewNameFilter, maxItems, {ignoreLoading: true, ignoreErrors: true}, filter.entityViewType).then(
                    function success(entities) {
                        if (entities && entities.length || !failOnEmpty) {
                            result.entities = entitiesToEntitiesInfo(entities);
                            deferred.resolve(result);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                break;
            case types.aliasFilterType.relationsQuery.value:
                result.stateEntity = filter.rootStateEntity;
                var rootEntityType;
                var rootEntityId;
                if (result.stateEntity && stateEntityId) {
                    rootEntityType = stateEntityId.entityType;
                    rootEntityId = stateEntityId.id;
                } else if (!result.stateEntity) {
                    rootEntityType = filter.rootEntity.entityType;
                    rootEntityId = filter.rootEntity.id;
                }
                if (rootEntityType && rootEntityId) {
                    var relationQueryRootEntityId = resolveAliasEntityId(rootEntityType, rootEntityId);
                    var searchQuery = {
                        parameters: {
                            rootId: relationQueryRootEntityId.id,
                            rootType: relationQueryRootEntityId.entityType,
                            direction: filter.direction
                        },
                        filters: filter.filters
                    };
                    searchQuery.parameters.maxLevel = filter.maxLevel && filter.maxLevel > 0 ? filter.maxLevel : -1;
                    entityRelationService.findInfoByQuery(searchQuery, {ignoreLoading: true, ignoreErrors: true}).then(
                        function success(allRelations) {
                            if (allRelations && allRelations.length || !failOnEmpty) {
                                if (angular.isDefined(maxItems) && maxItems > 0 && allRelations) {
                                    var limit = Math.min(allRelations.length, maxItems);
                                    allRelations.length = limit;
                                }
                                entityRelationInfosToEntitiesInfo(allRelations, filter.direction).then(
                                    function success(entities) {
                                        result.entities = entities;
                                        deferred.resolve(result);
                                    },
                                    function fail() {
                                        deferred.reject();
                                    }
                                );
                            } else {
                                deferred.reject();
                            }
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                } else {
                    deferred.resolve(result);
                }
                break;
            case types.aliasFilterType.assetSearchQuery.value:
            case types.aliasFilterType.deviceSearchQuery.value:
            case types.aliasFilterType.entityViewSearchQuery.value:
                result.stateEntity = filter.rootStateEntity;
                if (result.stateEntity && stateEntityId) {
                    rootEntityType = stateEntityId.entityType;
                    rootEntityId = stateEntityId.id;
                } else if (!result.stateEntity) {
                    rootEntityType = filter.rootEntity.entityType;
                    rootEntityId = filter.rootEntity.id;
                }
                if (rootEntityType && rootEntityId) {
                    var searchQueryRootEntityId = resolveAliasEntityId(rootEntityType, rootEntityId);
                    searchQuery = {
                        parameters: {
                            rootId: searchQueryRootEntityId.id,
                            rootType: searchQueryRootEntityId.entityType,
                            direction: filter.direction
                        },
                        relationType: filter.relationType
                    };
                    searchQuery.parameters.maxLevel = filter.maxLevel && filter.maxLevel > 0 ? filter.maxLevel : -1;
                    var findByQueryPromise;
                    if (filter.type == types.aliasFilterType.assetSearchQuery.value) {
                        searchQuery.assetTypes = filter.assetTypes;
                        findByQueryPromise = assetService.findByQuery(searchQuery, false, {ignoreLoading: true, ignoreErrors: true});
                    } else if (filter.type == types.aliasFilterType.deviceSearchQuery.value) {
                        searchQuery.deviceTypes = filter.deviceTypes;
                        findByQueryPromise = deviceService.findByQuery(searchQuery, false, {ignoreLoading: true, ignoreErrors: true});
                    } else if (filter.type == types.aliasFilterType.entityViewSearchQuery.value) {
                        searchQuery.entityViewTypes = filter.entityViewTypes;
                        findByQueryPromise = entityViewService.findByQuery(searchQuery, false, {ignoreLoading: true, ignoreErrors: true});
                    }
                    findByQueryPromise.then(
                        function success(entities) {
                            if (entities && entities.length || !failOnEmpty) {
                                if (angular.isDefined(maxItems) && maxItems > 0 && entities) {
                                    var limit = Math.min(entities.length, maxItems);
                                    entities.length = limit;
                                }
                                result.entities = entitiesToEntitiesInfo(entities);
                                deferred.resolve(result);
                            } else {
                                deferred.reject();
                            }
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                } else {
                    deferred.resolve(result);
                }
                break;
        }
        return deferred.promise;
    }

    function filterAliasByEntityTypes(entityAlias, entityTypes) {
        var filter = entityAlias.filter;
        if (filterAliasFilterTypeByEntityTypes(filter.type, entityTypes)) {
            switch (filter.type) {
                case types.aliasFilterType.singleEntity.value:
                    return entityTypes.indexOf(filter.singleEntity.entityType) > -1 ? true : false;
                case types.aliasFilterType.entityGroup.value:
                    return entityTypes.indexOf(filter.groupType) > -1 ? true : false;
                case types.aliasFilterType.entityList.value:
                    return entityTypes.indexOf(filter.entityType) > -1 ? true : false;
                case types.aliasFilterType.entityName.value:
                    return entityTypes.indexOf(filter.entityType) > -1 ? true : false;
                case types.aliasFilterType.entityGroupList.value:
                    return entityTypes.indexOf(types.entityType.entityGroup) > -1 ? true : false;
                case types.aliasFilterType.entityGroupName.value:
                    return entityTypes.indexOf(types.entityType.entityGroup) > -1 ? true : false;
                case types.aliasFilterType.stateEntity.value:
                    return true;
                case types.aliasFilterType.assetType.value:
                    return entityTypes.indexOf(types.entityType.asset)  > -1 ? true : false;
                case types.aliasFilterType.deviceType.value:
                    return entityTypes.indexOf(types.entityType.device)  > -1 ? true : false;
                case types.aliasFilterType.entityViewType.value:
                    return entityTypes.indexOf(types.entityType.entityView)  > -1 ? true : false;
                case types.aliasFilterType.relationsQuery.value:
                    if (filter.filters && filter.filters.length) {
                        var match = false;
                        for (var f=0;f<filter.filters.length;f++) {
                            var relationFilter = filter.filters[f];
                            if (relationFilter.entityTypes && relationFilter.entityTypes.length) {
                                for (var et=0;et<relationFilter.entityTypes.length;et++) {
                                    if (entityTypes.indexOf(relationFilter.entityTypes[et]) > -1) {
                                        match = true;
                                        break;
                                    }
                                }
                            } else {
                                match = true;
                                break;
                            }
                        }
                        return match;
                    } else {
                        return true;
                    }
                case types.aliasFilterType.assetSearchQuery.value:
                    return entityTypes.indexOf(types.entityType.asset)  > -1 ? true : false;
                case types.aliasFilterType.deviceSearchQuery.value:
                    return entityTypes.indexOf(types.entityType.device)  > -1 ? true : false;
                case types.aliasFilterType.entityViewSearchQuery.value:
                    return entityTypes.indexOf(types.entityType.entityView)  > -1 ? true : false;
            }
        }
        return false;
    }

    function filterAliasFilterTypeByEntityType(aliasFilterType, entityType) {
        switch (aliasFilterType) {
            case types.aliasFilterType.singleEntity.value:
                return true;
            case types.aliasFilterType.entityGroup.value:
                return true;
            case types.aliasFilterType.entityList.value:
                return true;
            case types.aliasFilterType.entityName.value:
                return true;
            case types.aliasFilterType.entityGroupList.value:
                return entityType === types.entityType.entityGroup;
            case types.aliasFilterType.entityGroupName.value:
                return entityType === types.entityType.entityGroup;
            case types.aliasFilterType.stateEntity.value:
                return true;
            case types.aliasFilterType.assetType.value:
                return entityType === types.entityType.asset;
            case types.aliasFilterType.deviceType.value:
                return entityType === types.entityType.device;
            case types.aliasFilterType.entityViewType.value:
                return entityType === types.entityType.entityView;
            case types.aliasFilterType.relationsQuery.value:
                return true;
            case types.aliasFilterType.assetSearchQuery.value:
                return entityType === types.entityType.asset;
            case types.aliasFilterType.deviceSearchQuery.value:
                return entityType === types.entityType.device;
            case types.aliasFilterType.entityViewSearchQuery.value:
                return entityType === types.entityType.entityView;
        }
        return false;
    }

    function filterAliasFilterTypeByEntityTypes(aliasFilterType, entityTypes) {
        if (!entityTypes || !entityTypes.length) {
            return true;
        }
        var valid = false;
        entityTypes.forEach(function(entityType) {
            valid = valid || filterAliasFilterTypeByEntityType(aliasFilterType, entityType);
        });
        return valid;
    }

    function getAliasFilterTypesByEntityTypes(entityTypes) {
        var allAliasFilterTypes = types.aliasFilterType;
        if (!entityTypes || !entityTypes.length) {
            return allAliasFilterTypes;
        }
        var result = {};
        for (var type in allAliasFilterTypes) {
            var aliasFilterType = allAliasFilterTypes[type];
            if (filterAliasFilterTypeByEntityTypes(aliasFilterType.value, entityTypes)) {
                result[type] = aliasFilterType;
            }
        }
        return result;
    }

    function prepareAllowedEntityTypesList(allowedEntityTypes, useAliasEntityTypes, operation) {
        var authority = userService.getAuthority();
        var entityTypes = {};
        switch(authority) {
            case 'SYS_ADMIN':
                entityTypes.tenant = types.entityType.tenant;
                break;
            case 'TENANT_ADMIN':
                entityTypes.device = types.entityType.device;
                entityTypes.asset = types.entityType.asset;
                entityTypes.entityView = types.entityType.entityView;
                entityTypes.tenant = types.entityType.tenant;
                entityTypes.customer = types.entityType.customer;
                entityTypes.dashboard = types.entityType.dashboard;
                entityTypes.user = types.entityType.user;
                entityTypes.converter = types.entityType.converter;
                entityTypes.integration = types.entityType.integration;
                entityTypes.schedulerEvent = types.entityType.schedulerEvent;
                entityTypes.blobEntity = types.entityType.blobEntity;
                entityTypes.role = types.entityType.role;
                if (useAliasEntityTypes) {
                    entityTypes.current_customer = types.aliasEntityType.current_customer;
                }
                break;
            case 'CUSTOMER_USER':
                entityTypes.device = types.entityType.device;
                entityTypes.asset = types.entityType.asset;
                entityTypes.entityView = types.entityType.entityView;
                entityTypes.customer = types.entityType.customer;
                entityTypes.dashboard = types.entityType.dashboard;
                entityTypes.user = types.entityType.user;
                entityTypes.schedulerEvent = types.entityType.schedulerEvent;
                entityTypes.blobEntity = types.entityType.blobEntity;
                if (useAliasEntityTypes) {
                    entityTypes.current_customer = types.aliasEntityType.current_customer;
                }
                break;
        }

        if (allowedEntityTypes) {
            for (var entityType in entityTypes) {
                if (allowedEntityTypes.indexOf(entityTypes[entityType]) === -1) {
                    delete entityTypes[entityType];
                }
            }
        }
        if (operation) {
            for (entityType in entityTypes) {
                var resource = securityTypes.resourceByEntityType[entityTypes[entityType]];
                if (resource) {
                    if (!userPermissionsService.hasGenericPermission(resource, securityTypes.operation[operation])) {
                        delete entityTypes[entityType];
                    }
                }
            }
        }
        return entityTypes;
    }


    function checkEntityAlias(entityAlias) {
        var deferred = $q.defer();
        resolveAliasFilter(entityAlias.filter, null, 1, true).then(
            function success(result) {
                if (result.stateEntity) {
                    deferred.resolve(true);
                } else {
                    var entities = result.entities;
                    if (entities && entities.length) {
                        deferred.resolve(true);
                    } else {
                        deferred.resolve(false);
                    }
                }
            },
            function fail() {
                deferred.resolve(false);
            }
        );
        return deferred.promise;
    }

    function getEntityKeys(entityType, entityId, query, type, config) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/keys/';
        if (type === types.dataKeyType.timeseries) {
            url += 'timeseries';
        } else if (type === types.dataKeyType.attribute) {
            url += 'attributes';
        }
        $http.get(url, config).then(function success(response) {
            var result = [];
            if (response.data) {
                if (query) {
                    var dataKeys = response.data;
                    var lowercaseQuery = angular.lowercase(query);
                    for (var i=0; i<dataKeys.length;i++) {
                        if (angular.lowercase(dataKeys[i]).indexOf(lowercaseQuery) === 0) {
                            result.push(dataKeys[i]);
                        }
                    }
                } else {
                    result = response.data;
                }
            }
            deferred.resolve(result);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function createDatasourcesFromSubscriptionsInfo(subscriptionsInfo) {
        var deferred = $q.defer();
        var datasources = [];
        processSubscriptionsInfo(0, subscriptionsInfo, datasources, deferred);
        return deferred.promise;
    }

    function createAlarmSourceFromSubscriptionInfo(subscriptionInfo) {
        var deferred = $q.defer();
        var datasources = [];
        if (subscriptionInfo.entityId && subscriptionInfo.entityType) {
            getEntity(subscriptionInfo.entityType, subscriptionInfo.entityId, {ignoreLoading: true}).then(
                function success(entity) {
                    createDatasourceFromSubscription(subscriptionInfo, datasources, entity);
                    var alarmSource = datasources[0];
                    deferred.resolve(alarmSource);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred) {
        if (index < subscriptionsInfo.length) {
            var subscriptionInfo = validateSubscriptionInfo(subscriptionsInfo[index]);
            if (subscriptionInfo.type === types.datasourceType.entity) {
                if (subscriptionInfo.entityId) {
                    if (subscriptionInfo.entityName) {
                        var entity = {
                            id: {id: subscriptionInfo.entityId, entityType: subscriptionInfo.entityType},
                            name: subscriptionInfo.entityName
                        };
                        createDatasourceFromSubscription(subscriptionInfo, datasources, entity);
                        index++;
                        processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                    } else {
                        getEntity(subscriptionInfo.entityType, subscriptionInfo.entityId, {ignoreLoading: true, ignoreErrors: true}).then(
                            function success(entity) {
                                createDatasourceFromSubscription(subscriptionInfo, datasources, entity);
                                index++;
                                processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                            },
                            function fail() {
                                index++;
                                processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                            }
                        );
                    }
                } else if (subscriptionInfo.entityName || subscriptionInfo.entityNamePrefix
                    || subscriptionInfo.entityIds) {
                    var promise;
                    if (subscriptionInfo.entityName) {
                        promise = getEntitiesByNameFilter(subscriptionInfo.entityType, subscriptionInfo.entityName, 1, {ignoreLoading: true, ignoreErrors: true});
                    } else if (subscriptionInfo.entityNamePrefix) {
                        promise = getEntitiesByNameFilter(subscriptionInfo.entityType, subscriptionInfo.entityNamePrefix, 100, {ignoreLoading: true, ignoreErrors: true});
                    } else if (subscriptionInfo.entityIds) {
                        promise = getEntities(subscriptionInfo.entityType, subscriptionInfo.entityIds, {ignoreLoading: true, ignoreErrors: true});
                    }
                    promise.then(
                        function success(entities) {
                            if (entities && entities.length > 0) {
                                for (var i = 0; i < entities.length; i++) {
                                    var entity = entities[i];
                                    createDatasourceFromSubscription(subscriptionInfo, datasources, entity);
                                }
                            }
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        },
                        function fail() {
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        }
                    )
                } else {
                    index++;
                    processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                }
            } else if (subscriptionInfo.type === types.datasourceType.function) {
                createDatasourceFromSubscription(subscriptionInfo, datasources);
                index++;
                processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
            }
        } else {
            deferred.resolve(datasources);
        }
    }

    function validateSubscriptionInfo(subscriptionInfo) {
        if (subscriptionInfo.type === 'device') {
            subscriptionInfo.type = types.datasourceType.entity;
            subscriptionInfo.entityType = types.entityType.device;
            if (subscriptionInfo.deviceId) {
                subscriptionInfo.entityId = subscriptionInfo.deviceId;
            } else if (subscriptionInfo.deviceName) {
                subscriptionInfo.entityName = subscriptionInfo.deviceName;
            } else if (subscriptionInfo.deviceNamePrefix) {
                subscriptionInfo.entityNamePrefix = subscriptionInfo.deviceNamePrefix;
            } else if (subscriptionInfo.deviceIds) {
                subscriptionInfo.entityIds = subscriptionInfo.deviceIds;
            }
        }
        return subscriptionInfo;
    }

    function createDatasourceFromSubscription(subscriptionInfo, datasources, entity) {
        var datasource;
        if (subscriptionInfo.type === types.datasourceType.entity) {
            datasource = {
                type: subscriptionInfo.type,
                entityName: entity.name ? entity.name : entity.title,
                name: entity.name ? entity.name : entity.title,
                entityType: subscriptionInfo.entityType,
                entityId: entity.id.id,
                dataKeys: []
            }
        } else if (subscriptionInfo.type === types.datasourceType.function) {
            datasource = {
                type: subscriptionInfo.type,
                name: subscriptionInfo.name || types.datasourceType.function,
                dataKeys: []
            }
        }
        datasources.push(datasource);
        if (subscriptionInfo.timeseries) {
            createDatasourceKeys(subscriptionInfo.timeseries, types.dataKeyType.timeseries, datasource, datasources);
        }
        if (subscriptionInfo.attributes) {
            createDatasourceKeys(subscriptionInfo.attributes, types.dataKeyType.attribute, datasource, datasources);
        }
        if (subscriptionInfo.functions) {
            createDatasourceKeys(subscriptionInfo.functions, types.dataKeyType.function, datasource, datasources);
        }
        if (subscriptionInfo.alarmFields) {
            createDatasourceKeys(subscriptionInfo.alarmFields, types.dataKeyType.alarm, datasource, datasources);
        }
    }

    function createDatasourceKeys(keyInfos, type, datasource, datasources) {
        for (var i=0;i<keyInfos.length;i++) {
            var keyInfo = keyInfos[i];
            var dataKey = utils.createKey(keyInfo, type, datasources);
            datasource.dataKeys.push(dataKey);
        }
    }

    function getRelatedEntities(rootEntityId, entityType, entitySubTypes, maxLevel, keys, typeTranslatePrefix, relationType, direction) {
        var deferred = $q.defer();

        var entitySearchQuery = constructRelatedEntitiesSearchQuery(rootEntityId, entityType, entitySubTypes, maxLevel, relationType, direction);
        if (!entitySearchQuery) {
            deferred.reject();
        } else {
            var findByQueryPromise;
            if (entityType == types.entityType.asset) {
                findByQueryPromise = assetService.findByQuery(entitySearchQuery, true, {ignoreLoading: true});
            } else if (entityType == types.entityType.device) {
                findByQueryPromise = deviceService.findByQuery(entitySearchQuery, true, {ignoreLoading: true});
            }
            findByQueryPromise.then(
                function success(entities) {
                    var entitiesTasks = [];
                    for (var i=0;i<entities.length;i++) {
                        var entity = entities[i];
                        var entityPromise = constructEntity(entity, keys, typeTranslatePrefix);
                        entitiesTasks.push(entityPromise);
                    }
                    $q.all(entitiesTasks).then(
                        function success(entities) {
                            deferred.resolve(entities);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                },
                function fail() {
                    deferred.reject();
                }
            );
        }
        return deferred.promise;
    }

    function saveRelatedEntity(relatedEntity, parentEntityId, keys, relation, direction) {
        var deferred = $q.defer();
        if (!direction) {
            direction = types.entitySearchDirection.from;
        }
        if (relatedEntity.id.id) {
            updateRelatedEntity(relatedEntity, keys, deferred, relation, direction);
        } else {
            addRelatedEntity(relatedEntity, parentEntityId, keys, deferred, relation, direction);
        }
        return deferred.promise;
    }

    function saveEntityRelation(entityType, entityId, entityRelation, config) {
        const deferred = $q.defer();
        let attributesType = Object.keys(types.attributesScope);
        let allPromise = [];
        let promise = "";
        if (entityRelation.accessToken !== "") {
            promise = deviceService.getDeviceCredentials(entityId.id, null, config).then(function (response) {
                response.credentialsId = entityRelation.accessToken;
                response.credentialsType = "ACCESS_TOKEN";
                response.credentialsValue = null;
                return deviceService.saveDeviceCredentials(response, config).catch(function () {
                    return "error";
                });
            });
            allPromise.push(promise)
        }
        for (let i = 0; i < attributesType.length; i++) {
            let attribute = attributesType[i];
            if (entityRelation.attributes[attribute] && entityRelation.attributes[attribute].length !== 0) {
                promise = attributeService.saveEntityAttributes(entityType, entityId.id, types.attributesScope[attribute].value, entityRelation.attributes[attribute], config).catch(function () {
                    return "error";
                });
                allPromise.push(promise);
            }
        }
        if (entityRelation.timeseries.length !== 0) {
            promise = attributeService.saveEntityTimeseries(entityType, entityId.id, "time", entityRelation.timeseries, config).catch(function(){
                return "error";
            });
            allPromise.push(promise);
        }
        $q.all(allPromise).then(function success(response) {
            let isResponseHasError = false;
            for(let i = 0; i < response.length; i++){
                if(response[i] === "error"){
                    isResponseHasError = true;
                    break;
                }
            }
            isResponseHasError ? deferred.reject() : deferred.resolve();
        });
        return deferred.promise;
    }

    function saveEntityParameters(customerId, entityType, entityGroupId, entityParameters, update, config) {
        config = config || {};
        const deferred = $q.defer();
        let statisticalInfo = {};
        let newEntity = {
            name: entityParameters.name,
            type: entityParameters.type,
            customerId: customerId
        };
        let promise;
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.saveDevice(newEntity, entityGroupId, config);
                break;
            case types.entityType.asset:
                promise = assetService.saveAsset(newEntity, true, config, entityGroupId);
                break;
        }

        promise.then(function success(response) {
            saveEntityRelation(entityType, response.id, entityParameters, config).then(function success() {
                statisticalInfo.create = {
                    entity: 1
                };
                deferred.resolve(statisticalInfo);
            }, function fail() {
                statisticalInfo.error = {
                    entity: 1
                };
                deferred.resolve(statisticalInfo);
            });
        }, function fail() {
            if (update) {
                let findIdEntity;
                switch (entityType) {
                    case types.entityType.device:
                        findIdEntity = deviceService.findByName(entityParameters.name, config);
                        break;
                    case types.entityType.asset:
                        findIdEntity = assetService.findByName(entityParameters.name, config);
                        break;
                }
                findIdEntity.then(function success(response) {
                    saveEntityRelation(entityType, response.id, entityParameters, config).then(function success() {
                        statisticalInfo.update = {
                            entity: 1
                        };
                        deferred.resolve(statisticalInfo);
                    }, function fail() {
                        statisticalInfo.error = {
                            entity: 1
                        };
                        deferred.resolve(statisticalInfo);
                    });
                }, function fail() {
                    statisticalInfo.error = {
                        entity: 1
                    };
                    deferred.resolve(statisticalInfo);
                });
            } else {
                statisticalInfo.error = {
                    entity: 1
                };
                deferred.resolve(statisticalInfo);
            }
        });
        return deferred.promise;
    }

    function getRelatedEntity(entityId, keys, typeTranslatePrefix) {
        var deferred = $q.defer();
        getEntityPromise(entityId.entityType, entityId.id, {ignoreLoading: true}).then(
            function success(entity) {
                constructEntity(entity, keys, typeTranslatePrefix).then(
                    function success(relatedEntity) {
                        deferred.resolve(relatedEntity);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function deleteEntityPromise(entityId) {
        if (entityId.entityType == types.entityType.asset) {
            return assetService.deleteAsset(entityId.id);
        } else if (entityId.entityType == types.entityType.device) {
            return deviceService.deleteDevice(entityId.id);
        } else if (entityId.entityType == types.entityType.entityView) {
            return entityViewService.deleteEntityView(entityId.id);
        } else if (entityId.entityType == types.entityType.role) {
            return roleService.deleteRole(entityId.id);
        }
    }

    function deleteRelatedEntity(entityId, deleteRelatedEntityTypes) {
        var deferred = $q.defer();
        if (deleteRelatedEntityTypes) {
            var deleteRelatedEntitiesTasks = [];
            entityRelationService.findByFrom(entityId.id, entityId.entityType).then(
                function success(entityRelations) {
                    for (var i=0;i<entityRelations.length;i++) {
                        var entityRelation = entityRelations[i];
                        var relationEntityId = entityRelation.to;
                        if (deleteRelatedEntityTypes.length == 0 || deleteRelatedEntityTypes.indexOf(relationEntityId.entityType) > -1) {
                            var deleteRelatedEntityPromise = deleteRelatedEntity(relationEntityId, deleteRelatedEntityTypes);
                            deleteRelatedEntitiesTasks.push(deleteRelatedEntityPromise);
                        }
                    }
                    deleteRelatedEntitiesTasks.push(deleteEntityPromise(entityId));
                    $q.all(deleteRelatedEntitiesTasks).then(
                        function success() {
                            deferred.resolve();
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                },
                function fail() {
                    deferred.reject();
                }
            )
        } else {
            deleteEntityPromise(entityId).then(
                function success() {
                    deferred.resolve();
                },
                function fail() {
                    deferred.reject();
                }
            );
        }
        return deferred.promise;
    }

    function moveEntity(entityId, prevParentId, targetParentId) {
        var deferred = $q.defer();
        entityRelationService.deleteRelation(prevParentId.id, prevParentId.entityType,
            types.entityRelationType.contains, entityId.id, entityId.entityType).then(
            function success() {
                var relation = {
                    from: targetParentId,
                    to: entityId,
                    type: types.entityRelationType.contains
                };
                entityRelationService.saveRelation(relation).then(
                    function success() {
                        deferred.resolve();
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function copyEntity(entity, targetParentId, keys) {
        var deferred = $q.defer();
        if (!entity.id && !entity.id.id) {
            deferred.reject();
        } else {
            getRelatedEntity(entity.id, keys).then(
                function success(relatedEntity) {
                    delete relatedEntity.id.id;
                    relatedEntity.name = entity.name;
                    saveRelatedEntity(relatedEntity, targetParentId, keys).then(
                        function success(savedEntity) {
                            deferred.resolve(savedEntity);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                },
                function fail() {
                    deferred.reject();
                }
            );
        }
        return deferred.promise;
    }

    function addRelatedEntity(relatedEntity, parentEntityId, keys, deferred, relation, direction) {
        var entity = {};
        entity.id = relatedEntity.id;
        entity.name = relatedEntity.name;
        entity.type = relatedEntity.type;
        saveEntityPromise(entity).then(
            function success(entity) {
                relatedEntity.id = entity.id;
                if (!relation) {
                    relation = {
                        type: types.entityRelationType.contains
                    };
                }

                if (direction == types.entitySearchDirection.from) {
                    relation.from = parentEntityId;
                    relation.to = relatedEntity.id;
                } else {
                    relation.from = relatedEntity.id;
                    relation.to = parentEntityId;
                }

                entityRelationService.saveRelation(relation).then(
                    function success() {
                        updateEntity(entity, relatedEntity, keys, deferred, relation);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
    }

    function updateRelatedEntity(relatedEntity, keys, deferred, relation, direction) {
        getEntityPromise(relatedEntity.id.entityType, relatedEntity.id.id, {ignoreLoading: true}).then(
            function success(entity) {
                if (relation) {
                    if (direction == types.entitySearchDirection.from) {
                        relation.to = relatedEntity.id;
                    } else {
                        relation.from = relatedEntity.id;
                    }
                    entityRelationService.saveRelation(relation).then(
                        function success() {
                            updateEntity(entity, relatedEntity, keys, deferred);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                } else {
                    updateEntity(entity, relatedEntity, keys, deferred);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
    }

    function updateEntity(entity, relatedEntity, keys, deferred) {
        if (!angular.equals(entity.name, relatedEntity.name) || !angular.equals(entity.type, relatedEntity.type)) {
            entity.name = relatedEntity.name;
            entity.type = relatedEntity.type;
            saveEntityPromise(entity).then(
                function success (entity) {
                    updateEntityAttributes(entity, relatedEntity, keys, deferred);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            updateEntityAttributes(entity, relatedEntity, keys, deferred);
        }
    }

    function updateEntityAttributes(entity, relatedEntity, keys, deferred) {
        var attributes = [];
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            attributes.push({key: key, value: relatedEntity[key]});
        }
        attributeService.saveEntityAttributes(entity.id.entityType, entity.id.id, types.attributesScope.server.value, attributes)
            .then(
                function success() {
                    deferred.resolve(relatedEntity);
                },
                function fail() {
                    deferred.reject();
                }
            );
    }

    function constructRelatedEntitiesSearchQuery(rootEntityId, entityType, entitySubTypes, maxLevel, relationType, direction) {

        var searchQuery = {
            parameters: {
                rootId: rootEntityId.id,
                rootType: rootEntityId.entityType,
                direction: direction
            },
            relationType: relationType
        };
        if (!direction) {
            searchQuery.parameters.direction = types.entitySearchDirection.from;
        }
        if (!relationType) {
            searchQuery.relationType = types.entityRelationType.contains;
        }

        if (maxLevel) {
            searchQuery.parameters.maxLevel = maxLevel;
        } else {
            searchQuery.parameters.maxLevel = 1;
        }

        if (entityType == types.entityType.asset) {
            searchQuery.assetTypes = entitySubTypes;
        } else if (entityType == types.entityType.device) {
            searchQuery.deviceTypes = entitySubTypes;
        } else if (entityType == types.entityType.entityView) {
            searchQuery.entityViewTypes = entitySubTypes;
        } else if (entityType == types.entityType.role) {
            searchQuery.roleTypes = entitySubTypes;
        } else {
            return null; //Not supported
        }

        return searchQuery;
    }

    function constructEntity(entity, keys, typeTranslatePrefix) {
        var deferred = $q.defer();
        if (typeTranslatePrefix) {
            entity.typeName = $translate.instant(typeTranslatePrefix+'.'+entity.type);
        } else {
            entity.typeName = entity.type;
        }
        attributeService.getEntityAttributesValues(entity.id.entityType, entity.id.id,
            types.attributesScope.server.value, keys.join(','),
            {ignoreLoading: true}).then(
            function success(attributes) {
                if (attributes && attributes.length > 0) {
                    for (var i=0;i<keys.length;i++) {
                        var key = keys[i];
                        entity[key] = getAttributeValue(attributes, key);
                    }
                }
                deferred.resolve(entity);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAttributeValue(attributes, key) {
        var foundAttributes = $filter('filter')(attributes, {key: key}, true);
        if (foundAttributes.length > 0) {
            return foundAttributes[0].value;
        } else {
            return null;
        }
    }

}
