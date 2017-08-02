/**
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

package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
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
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EntityGroupController extends BaseController {

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityGroup getEntityGroupById(@PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
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
            return checkNotNull(entityGroupService.saveEntityGroup(getCurrentUser().getTenantId(), entityGroup));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityGroup(@PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
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
            @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        try {
            checkParameter("groupType", strGroupType);
            EntityType groupType = EntityType.valueOf(strGroupType);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(entityGroupService.findEntityGroupsByType(tenantId, groupType).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{entityType}/{entityIds}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void addEntitiesToEntityGroup(@PathVariable("entityGroupId") String strEntityGroupId,
                                         @PathVariable("entityType") String strEntityType,
                                         @PathVariable("entityIds") String[] strEntityIds) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        checkParameter("entityType", strEntityType);
        checkArrayParameter("entityIds", strEntityIds);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to add entities to entity group: " +
                        "Addition to entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            EntityType entityType = EntityType.valueOf(strEntityType);
            if (entityGroup.getType() != entityType) {
                throw new ThingsboardException("Unable to add entities to entity group: " +
                        "Entity type can't be different from entity group type!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            List<EntityId> entityIds = new ArrayList<>();
            for (String strEntityId : strEntityIds) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
                checkEntityId(entityId);
                entityIds.add(entityId);
            }
            entityGroupService.addEntitiesToEntityGroup(entityGroupId, entityIds);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{entityType}/{entityIds}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void removeEntitiesFromEntityGroup(@PathVariable("entityGroupId") String strEntityGroupId,
                                              @PathVariable("entityType") String strEntityType,
                                              @PathVariable("entityIds") String[] strEntityIds) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        checkParameter("entityType", strEntityType);
        checkArrayParameter("entityIds", strEntityIds);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to remove entities from entity group: " +
                        "Removal from entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            EntityType entityType = EntityType.valueOf(strEntityType);
            if (entityGroup.getType() != entityType) {
                throw new ThingsboardException("Unable to remove entities from entity group: " +
                        "Entity type can't be different from entity group type!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            List<EntityId> entityIds = new ArrayList<>();
            for (String strEntityId : strEntityIds) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
                checkEntityId(entityId);
                entityIds.add(entityId);
            }
            entityGroupService.removeEntitiesFromEntityGroup(entityGroupId, entityIds);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityView getGroupEntity(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        checkParameter("entityType", strEntityType);
        checkParameter("entityId", strEntityId);

        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
            EntityType entityType = EntityType.valueOf(strEntityType);
            if (entityGroup.getType() != entityType) {
                throw new ThingsboardException("Unable to get entity for entity group: " +
                        "Entity type can't be different from entity group type!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
            checkEntityId(entityId);
            EntityView result = null;
            if (entityType == EntityType.USER) {
                result = userService.findGroupUser(entityGroupId, entityId);
            } else if (entityType == EntityType.CUSTOMER) {
                result = customerService.findGroupCustomer(entityGroupId, entityId);
            } else if (entityType == EntityType.ASSET) {
                result = assetService.findGroupAsset(entityGroupId, entityId);
            } else if (entityType == EntityType.DEVICE) {
                result = deviceService.findGroupDevice(entityGroupId, entityId);
            } else {
                throw new ThingsboardException("Unable to get entity for entity group: " +
                        "Unsupported entity type " + entityType + "!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{entityType}", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<EntityView> getEntities(
            @PathVariable("entityGroupId") String strEntityGroupId,
            @PathVariable("entityType") String strEntityType,
            @RequestParam int limit,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
            @RequestParam(required = false) String offset
    ) throws ThingsboardException {
        checkParameter("entityGroupId", strEntityGroupId);
        checkParameter("entityType", strEntityType);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId);
        EntityType entityType = EntityType.valueOf(strEntityType);
        if (entityGroup.getType() != entityType) {
            throw new ThingsboardException("Unable to get entities for entity group: " +
                    "Entity type can't be different from entity group type!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        try {
            TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, offset);
            ListenableFuture<TimePageData<EntityView>> asyncResult = null;
            if (entityType == EntityType.USER) {
                asyncResult = userService.findUsersByEntityGroupId(entityGroupId, pageLink);
            } else if (entityType == EntityType.CUSTOMER) {
                asyncResult = customerService.findCustomersByEntityGroupId(entityGroupId, pageLink);
            } else if (entityType == EntityType.ASSET) {
                asyncResult = assetService.findAssetsByEntityGroupId(entityGroupId, pageLink);
            } else if (entityType == EntityType.DEVICE) {
                asyncResult = deviceService.findDevicesByEntityGroupId(entityGroupId, pageLink);
            } else {
                throw new ThingsboardException("Unable to get entities for entity group: " +
                        "Unsupported entity type " + entityType + "!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            return checkNotNull(asyncResult.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
