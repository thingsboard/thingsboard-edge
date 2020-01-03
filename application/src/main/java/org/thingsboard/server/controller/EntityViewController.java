/**
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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.EntityGroupController.ENTITY_GROUP_ID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class EntityViewController extends BaseController {

    public static final String ENTITY_VIEW_ID = "entityViewId";

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
            entityView.setTenantId(getCurrentUser().getTenantId());

            Operation operation = entityView.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.CREATE
                    && getCurrentUser().getAuthority() == Authority.CUSTOMER_USER &&
                    (entityView.getCustomerId() == null || entityView.getCustomerId().isNullUid())) {
                entityView.setCustomerId(getCurrentUser().getCustomerId());
            }

            EntityGroupId entityGroupId = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            }

            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, operation,
                    entityView.getId(), entityView, entityGroupId);

            EntityView savedEntityView = checkNotNull(entityViewService.saveEntityView(entityView));

            if (entityGroupId != null && operation == Operation.CREATE) {
                entityGroupService.addEntityToEntityGroup(getTenantId(), entityGroupId, savedEntityView.getId());
            }

            List<ListenableFuture<List<Void>>> futures = new ArrayList<>();
            if (savedEntityView.getKeys() != null && savedEntityView.getKeys().getAttributes() != null) {
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs(), getCurrentUser()));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs(), getCurrentUser()));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh(), getCurrentUser()));
            }
            for (ListenableFuture<List<Void>> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Failed to copy attributes to entity view", e);
                }
            }

            logEntityAction(savedEntityView.getId(), savedEntityView, null,
                    entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedEntityView;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_VIEW), entityView, null,
                    entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
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
            });
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) throws ThingsboardException {
        logEntityAction(user, (UUIDBased & EntityId) entityId, null, null, ActionType.ATTRIBUTES_UPDATED, toException(e),
                scope, attributes);
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityView(@PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws ThingsboardException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            EntityView entityView = checkEntityViewId(entityViewId, Operation.DELETE);
            entityViewService.deleteEntityView(getTenantId(), entityViewId);
            logEntityAction(entityViewId, entityView, entityView.getCustomerId(),
                    ActionType.DELETED, null, strEntityViewId);
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
    @RequestMapping(value = "/customer/{customerId}/entityViews", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<EntityView> getCustomerEntityViews(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
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
    @RequestMapping(value = "/tenant/entityViews", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<EntityView> getTenantEntityViews(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);

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
    @RequestMapping(value = "/user/entityViews", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<EntityView> getUserEntityViews(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            List<Predicate<EntityView>> filters = new ArrayList<>();
            if (type != null && type.trim().length() > 0) {
                filters.add((entityView -> entityView.getType().equals(type)));
            }
            return getGroupEntitiesByPageLink(getCurrentUser(), EntityType.ENTITY_VIEW, Operation.READ, entityId -> new EntityViewId(entityId.getId()),
                    (entityIds) -> {
                        try {
                            return entityViewService.findEntityViewsByTenantIdAndIdsAsync(getTenantId(), entityIds).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    filters,
                    pageLink);
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
    @RequestMapping(value = "/entityGroup/{entityGroupId}/entityViews", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<EntityView> getEntityViewsByEntityGroupId(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Page link limit", required = true, allowableValues = "range[1, infinity]") @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        EntityType entityType = entityGroup.getType();
        checkEntityGroupType(entityType);
        try {
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            ListenableFuture<TimePageData<EntityView>> asyncResult = entityViewService.findEntityViewEntitiesByEntityGroupId(getTenantId(), entityGroupId, pageLink);
            checkNotNull(asyncResult);
            if (asyncResult != null) {
                return checkNotNull(asyncResult.get());
            } else {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
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
