/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EntityGroupController extends BaseController {

    public static final String ENTITY_GROUP_ID = "entityGroupId";

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityGroup getEntityGroupById(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            return checkEntityGroupId(entityGroupId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup", method = RequestMethod.POST)
    @ResponseBody
    public EntityGroup saveEntityGroup(@RequestBody EntityGroup entityGroup) throws ThingsboardException {
        try {
            checkEntityGroupType(entityGroup.getType());
            return checkNotNull(entityGroupService.saveEntityGroup(getCurrentUser().getTenantId(), entityGroup));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to remove entity group: " +
                        "Removal of entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            entityGroupService.deleteEntityGroup(entityGroupId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityGroups/{groupType}", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroup> getTenantEntityGroups(
            @ApiParam(value = "EntityGroup type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE") @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        try {
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(entityGroupService.findEntityGroupsByType(tenantId, groupType).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/addEntities", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void addEntitiesToEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
                                         @RequestBody String[] strEntityIds) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkArrayParameter("entityIds", strEntityIds);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to add entities to entity group: " +
                        "Addition to entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            List<EntityId> entityIds = new ArrayList<>();
            for (String strEntityId : strEntityIds) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(entityGroup.getType(), strEntityId);
                checkEntityId(entityId);
                entityIds.add(entityId);
            }
            entityGroupService.addEntitiesToEntityGroup(entityGroupId, entityIds);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/deleteEntities", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void removeEntitiesFromEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
                                              @RequestBody String[] strEntityIds) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkArrayParameter("entityIds", strEntityIds);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to remove entities from entity group: " +
                        "Removal from entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            List<EntityId> entityIds = new ArrayList<>();
            for (String strEntityId : strEntityIds) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(entityGroup.getType(), strEntityId);
                checkEntityId(entityId);
                entityIds.add(entityId);
            }
            entityGroupService.removeEntitiesFromEntityGroup(entityGroupId, entityIds);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityView getGroupEntity(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkParameter("entityId", strEntityId);

        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            EntityType entityType = entityGroup.getType();
            checkEntityGroupType(entityType);
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
            checkEntityId(entityId);
            EntityView result = null;
            if (entityType == EntityType.CUSTOMER) {
                result = customerService.findGroupCustomer(entityGroupId, entityId);
            } else if (entityType == EntityType.ASSET) {
                result = assetService.findGroupAsset(entityGroupId, entityId);
            } else if (entityType == EntityType.DEVICE) {
                result = deviceService.findGroupDevice(entityGroupId, entityId);
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/entities", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<EntityView> getEntities(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = "Page link limit", required = true, allowableValues = "range[1, infinity]") @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
        EntityType entityType = entityGroup.getType();
        checkEntityGroupType(entityType);
        try {
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            ListenableFuture<TimePageData<EntityView>> asyncResult = null;
            if (entityType == EntityType.CUSTOMER) {
                if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                            ThingsboardErrorCode.PERMISSION_DENIED);
                }
                asyncResult = customerService.findCustomersByEntityGroupId(entityGroupId, pageLink);
            } else if (entityType == EntityType.ASSET) {
                asyncResult = assetService.findAssetsByEntityGroupIdAndCustomerId(entityGroupId, getCurrentUser().getCustomerId(), pageLink);
            } else if (entityType == EntityType.DEVICE) {
                asyncResult = deviceService.findDevicesByEntityGroupIdAndCustomerId(entityGroupId, getCurrentUser().getCustomerId(), pageLink);
            }
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

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroups/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroupId> getEntityGroupsForEntity(
            @ApiParam(value = "Entity type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE") @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter("entityType", strEntityType);
        checkParameter("entityId", strEntityId);
        try {
            EntityType entityType = checkStrEntityGroupType("entityType", strEntityType);
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
            checkEntityId(entityId);
            return checkNotNull(entityGroupService.findEntityGroupsForEntity(entityId).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
