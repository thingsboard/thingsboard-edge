/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.thingsboard.server.controller.EntityGroupController.ENTITY_GROUP_ID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class EntityViewController extends BaseController {

    public static final String ENTITY_VIEW_ID = "entityViewId";

    @Autowired
    private TimeseriesService tsService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityView getEntityViewById(@PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            return checkEntityViewId(new EntityViewId(toUUID(strEntityViewId)), Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView", method = RequestMethod.POST)
    @ResponseBody
    public EntityView saveEntityView(@RequestBody EntityView entityView,
                                     @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId) throws ThingsboardException {
        try {
            List<ListenableFuture<?>> futures = new ArrayList<>();

            if (entityView.getId() != null) {
                EntityView existingEntityView = checkEntityViewId(entityView.getId(), Operation.WRITE);
                if (existingEntityView.getKeys() != null) {
                    if (existingEntityView.getKeys().getAttributes() != null) {
                        futures.add(deleteAttributesFromEntityView(existingEntityView, DataConstants.CLIENT_SCOPE, existingEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                        futures.add(deleteAttributesFromEntityView(existingEntityView, DataConstants.SERVER_SCOPE, existingEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                        futures.add(deleteAttributesFromEntityView(existingEntityView, DataConstants.SHARED_SCOPE, existingEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                    }
                }
                List<String> tsKeys = existingEntityView.getKeys() != null && existingEntityView.getKeys().getTimeseries() != null ?
                        existingEntityView.getKeys().getTimeseries() : Collections.emptyList();
                futures.add(deleteLatestFromEntityView(existingEntityView, tsKeys, getCurrentUser()));
            }

            EntityView savedEntityView = saveGroupEntity(entityView, strEntityGroupId, entityViewService::saveEntityView);
            if (savedEntityView.getKeys() != null) {
                if (savedEntityView.getKeys().getAttributes() != null) {
                    futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                    futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs(), getCurrentUser()));
                    futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh(), getCurrentUser()));
                }
                futures.add(copyLatestFromEntityToEntityView(savedEntityView, getCurrentUser()));
            }
            for (ListenableFuture<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to copy attributes to entity view", e);
                }
            }
            return savedEntityView;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private ListenableFuture<Void> deleteLatestFromEntityView(EntityView entityView, List<String> keys, SecurityUser user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteLatest(entityView.getTenantId(), entityId, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logTimeseriesDeleted(user, entityId, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(user, entityId, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            tsSubService.deleteAllLatest(entityView.getTenantId(), entityId, new FutureCallback<Collection<String>>() {
                @Override
                public void onSuccess(@Nullable Collection<String> keys) {
                    try {
                        logTimeseriesDeleted(user, entityId, new ArrayList<>(keys), null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(user, entityId, Collections.emptyList(), t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        }
        return resultFuture;
    }

    private ListenableFuture<Void> deleteAttributesFromEntityView(EntityView entityView, String scope, List<String> keys, SecurityUser user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteAndNotify(entityView.getTenantId(), entityId, scope, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logAttributesDeleted(user, entityId, scope, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logAttributesDeleted(user, entityId, scope, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            resultFuture.set(null);
        }
        return resultFuture;
    }

    private ListenableFuture<List<Void>> copyLatestFromEntityToEntityView(EntityView entityView, SecurityUser user) {
        EntityViewId entityId = entityView.getId();
        List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                entityView.getKeys().getTimeseries() : Collections.emptyList();
        long startTs = entityView.getStartTimeMs();
        long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
        ListenableFuture<List<String>> keysFuture;
        if (keys.isEmpty()) {
            keysFuture = Futures.transform(tsService.findAllLatest(user.getTenantId(),
                    entityView.getEntityId()), latest -> latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList()), MoreExecutors.directExecutor());
        } else {
            keysFuture = Futures.immediateFuture(keys);
        }
        ListenableFuture<List<TsKvEntry>> latestFuture = Futures.transformAsync(keysFuture, fetchKeys -> {
            List<ReadTsKvQuery> queries = fetchKeys.stream().filter(key -> !isBlank(key)).map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 1, "DESC")).collect(Collectors.toList());
            if (!queries.isEmpty()) {
                return tsService.findAll(user.getTenantId(), entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(latestFuture, latestValues -> {
            if (latestValues != null && !latestValues.isEmpty()) {
                tsSubService.saveLatestAndNotify(entityView.getTenantId(), entityId, latestValues, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                });
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<List<Void>> copyAttributesFromEntityToEntityView(EntityView entityView, String scope, Collection<String> keys, SecurityUser user) throws ThingsboardException {
        EntityViewId entityId = entityView.getId();
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> getAttrFuture = attributesService.find(getTenantId(), entityView.getEntityId(), scope, keys);
            return Futures.transform(getAttrFuture, attributeKvEntries -> {
                List<AttributeKvEntry> attributes;
                if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                    attributes =
                            attributeKvEntries.stream()
                                    .filter(attributeKvEntry -> {
                                        long startTime = entityView.getStartTimeMs();
                                        long endTime = entityView.getEndTimeMs();
                                        long lastUpdateTs = attributeKvEntry.getLastUpdateTs();
                                        return startTime == 0 && endTime == 0 ||
                                                (endTime == 0 && startTime < lastUpdateTs) ||
                                                (startTime == 0 && endTime > lastUpdateTs)
                                                ? true : startTime < lastUpdateTs && endTime > lastUpdateTs;
                                    }).collect(Collectors.toList());
                    tsSubService.saveAndNotify(entityView.getTenantId(), entityId, scope, attributes, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            try {
                                logAttributesUpdated(user, entityId, scope, attributes, null);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            try {
                                logAttributesUpdated(user, entityId, scope, attributes, t);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }
                    });
                }
                return null;
            }, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) throws ThingsboardException {
        logEntityAction(user, entityId, null, null, ActionType.ATTRIBUTES_UPDATED, toException(e),
                scope, attributes);
    }

    private void logAttributesDeleted(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) throws ThingsboardException {
        logEntityAction(user, entityId, null, null, ActionType.ATTRIBUTES_DELETED, toException(e),
                scope, keys);
    }

    private void logTimeseriesDeleted(SecurityUser user, EntityId entityId, List<String> keys, Throwable e) throws ThingsboardException {
        logEntityAction(user, entityId, null, null, ActionType.TIMESERIES_DELETED, toException(e),
                keys);
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityView(@PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            EntityView entityView = checkEntityViewId(entityViewId, Operation.DELETE);

            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), entityViewId);

            entityViewService.deleteEntityView(getTenantId(), entityViewId);
            logEntityAction(entityViewId, entityView, entityView.getCustomerId(),
                    ActionType.DELETED, null, strEntityViewId);

            sendDeleteNotificationMsg(getTenantId(), entityViewId, relatedEdgeIds);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW),
                    null,
                    null,
                    ActionType.DELETED, e, strEntityViewId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViews", params = {"entityViewName"}, method = RequestMethod.GET)
    @ResponseBody
    public EntityView getTenantEntityView(
            @RequestParam String entityViewName) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getCustomerEntityViews(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId, customerId, pageLink, type));
            } else {
                return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getTenantEntityViews(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewByTenantIdAndType(tenantId, pageLink, type));
            } else {
                return checkNotNull(entityViewService.findEntityViewByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getUserEntityViews(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            SecurityUser currentUser = getCurrentUser();
            MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
            return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.ENTITY_VIEW,
                    Operation.READ, type, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityViews", params = {"entityViewIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<EntityView> getEntityViewsByIds(
            @RequestParam("entityViewIds") String[] strEntityViewIds) throws ThingsboardException {
        checkArrayParameter("entityViewIds", strEntityViewIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<EntityViewId> entityViewIds = new ArrayList<>();
            for (String strEntityViewId : strEntityViewIds) {
                entityViewIds.add(new EntityViewId(toUUID(strEntityViewId)));
            }
            List<EntityView> entityViews = checkNotNull(entityViewService.findEntityViewsByTenantIdAndIdsAsync(tenantId, entityViewIds).get());
            return filterEntityViewsByReadPermission(entityViews);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityViews", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityView> findByQuery(@RequestBody EntityViewSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEntityViewTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<EntityView> entityViews = checkNotNull(entityViewService.findEntityViewsByQuery(getTenantId(), query).get());
            return filterEntityViewsByReadPermission(entityViews);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getEntityViewsByEntityGroupId(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Page size", required = true, allowableValues = "range[1, infinity]") @RequestParam int pageSize,
            @ApiParam(value = "Page", required = true, allowableValues = "range[0, infinity]") @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        EntityType entityType = entityGroup.getType();
        checkEntityGroupType(entityType);
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(entityViewService.findEntityViewsByEntityGroupId(entityGroupId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<EntityView> filterEntityViewsByReadPermission(List<EntityView> entityViews) {
        return entityViews.stream().filter(entityView -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getEntityViewTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> entityViewTypes = entityViewService.findEntityViewTypesByTenantId(tenantId);
            return checkNotNull(entityViewTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
