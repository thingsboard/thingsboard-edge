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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityGroupController extends BaseController {

    public static final String ENTITY_GROUP_ID = "entityGroupId";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final int DEFAULT_ENTITY_GROUP_LIMIT = 100;

    @Autowired
    private OwnersCacheService ownersCacheService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityGroupInfo getEntityGroupById(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            return toEntityGroupInfo(checkEntityGroupId(entityGroupId, Operation.READ));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{ownerType}/{ownerId}/{groupType}/{groupName}", method = RequestMethod.GET)
    @ResponseBody
    public EntityGroupInfo getEntityGroupByOwnerAndNameAndType(@PathVariable("ownerType") String strOwnerType,
                                                               @PathVariable("ownerId") String strOwnerId,
                                                               @ApiParam(value = "EntityGroup type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType,
                                                               @PathVariable("groupName") String groupName) throws ThingsboardException {
        checkParameter("ownerId", strOwnerId);
        checkParameter("ownerType", strOwnerType);
        checkParameter("groupName", groupName);
        EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
        try {
            EntityId ownerId = EntityIdFactory.getByTypeAndId(strOwnerType, strOwnerId);
            checkEntityId(ownerId, Operation.READ);
            SecurityUser currentUser = getCurrentUser();
            Optional<EntityGroup> entityGroupOptional = entityGroupService.findOwnerEntityGroup(currentUser.getTenantId(), ownerId, groupType, groupName);
            if (entityGroupOptional.isPresent()) {
                accessControlService.checkEntityGroupPermission(getCurrentUser(), Operation.READ, entityGroupOptional.get());
                return toEntityGroupInfo(entityGroupOptional.get());
            } else {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup", method = RequestMethod.POST)
    @ResponseBody
    public EntityGroupInfo saveEntityGroup(@RequestBody EntityGroup entityGroup) throws ThingsboardException {
        try {
            checkEntityGroupType(entityGroup.getType());

            Operation operation = entityGroup.getId() == null ? Operation.CREATE : Operation.WRITE;

            EntityId parentEntityId = entityGroup.getOwnerId();
            if (operation == Operation.CREATE) {
                if (parentEntityId == null || parentEntityId.isNullUid()) {
                    parentEntityId = getCurrentUser().getOwnerId();
                } else {
                    if (!ownersCacheService.fetchOwners(getTenantId(), parentEntityId).contains(getCurrentUser().getOwnerId())) {
                        throw new ThingsboardException("Unable to create entity group: " +
                                "Invalid entity group ownerId!", ThingsboardErrorCode.PERMISSION_DENIED);
                    }
                }
            } else {
                validateEntityId(parentEntityId, "Incorrect entity group ownerId " + parentEntityId);
            }

            accessControlService.checkEntityGroupPermission(getCurrentUser(), operation, entityGroup);

            EntityGroup savedEntityGroup = checkNotNull(entityGroupService.saveEntityGroup(getTenantId(), parentEntityId, entityGroup));

            logEntityAction(savedEntityGroup.getId(), savedEntityGroup,
                    null,
                    entityGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (entityGroup.getId() != null) {
                sendEntityNotificationMsg(getTenantId(), savedEntityGroup.getId(),
                        EdgeEventActionType.UPDATED);
            }

            return toEntityGroupInfo(savedEntityGroup);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.ENTITY_GROUP), entityGroup,
                    null, entityGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.DELETE);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to remove entity group: " +
                        "Removal of entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }

            List<GroupPermissionInfo> groupPermissions = new ArrayList<>();
            groupPermissions.addAll(groupPermissionService.findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(getTenantId(), entityGroupId).get());
            if (entityGroup.getType() == EntityType.USER) {
                groupPermissions.addAll(groupPermissionService.findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(getTenantId(), entityGroupId).get());
            }

            for (GroupPermission groupPermission : groupPermissions) {
                userPermissionsService.onGroupPermissionDeleted(groupPermission);
            }

            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(getTenantId(), entityGroupId, entityGroup.getType());

            entityGroupService.deleteEntityGroup(getTenantId(), entityGroupId);

            logEntityAction(entityGroupId, entityGroup,
                    null,
                    ActionType.DELETED, null, strEntityGroupId);

            sendDeleteNotificationMsg(getTenantId(), entityGroupId, relatedEdgeIds);
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ENTITY_GROUP),
                    null,
                    null,
                    ActionType.DELETED, e, strEntityGroupId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroups/{groupType}", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroupInfo> getEntityGroupsByType(
            @ApiParam(value = "EntityGroup type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        try {
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
            MergedGroupTypePermissionInfo groupTypePermissionInfo = getCurrentUser().getUserPermissions().getReadGroupPermissions().get(groupType);
            if (groupTypePermissionInfo.isHasGenericRead() || !groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
                List<EntityGroup> groups = new ArrayList<>();
                if (groupTypePermissionInfo.isHasGenericRead()) {
                    EntityId parentEntityId = getCurrentUser().isTenantAdmin() ? getCurrentUser().getTenantId() : getCurrentUser().getCustomerId();
                    groups.addAll(entityGroupService.findEntityGroupsByType(getTenantId(), parentEntityId, groupType).get());
                }
                if (!groupTypePermissionInfo.getEntityGroupIds().isEmpty()) {
                    List<EntityGroupId> existingIds = groups.stream().map(EntityGroup::getId).collect(Collectors.toList());
                    List<EntityGroupId> groupIds = groupTypePermissionInfo.getEntityGroupIds().stream().filter(entityGroupId ->
                            !existingIds.contains(entityGroupId)
                    ).collect(Collectors.toList());
                    if (!groupIds.isEmpty()) {
                        groups.addAll(entityGroupService.findEntityGroupByIdsAsync(getTenantId(), groupIds).get());
                    }
                }
                groups.sort(Comparator.comparingLong(EntityGroup::getCreatedTime));
                return toEntityGroupsInfo(groups);
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroups/{ownerType}/{ownerId}/{groupType}", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroupInfo> getEntityGroupsByOwnerAndType(
            @PathVariable("ownerType") String strOwnerType,
            @PathVariable("ownerId") String strOwnerId,
            @ApiParam(value = "EntityGroup type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        checkParameter("ownerId", strOwnerId);
        checkParameter("ownerType", strOwnerType);
        try {
            EntityId ownerId = EntityIdFactory.getByTypeAndId(strOwnerType, strOwnerId);
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
            checkEntityId(ownerId, Operation.READ);
            MergedGroupTypePermissionInfo groupTypePermissionInfo = getCurrentUser().getUserPermissions().getReadGroupPermissions().get(groupType);
            if (groupTypePermissionInfo.isHasGenericRead()) {
                return toEntityGroupsInfo(entityGroupService.findEntityGroupsByType(getTenantId(), ownerId, groupType).get());
            } else {
                throw permissionDenied();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/all/{ownerType}/{ownerId}/{groupType}", method = RequestMethod.GET)
    @ResponseBody
    public EntityGroupInfo getEntityGroupAllByOwnerAndType(
            @PathVariable("ownerType") String strOwnerType,
            @PathVariable("ownerId") String strOwnerId,
            @ApiParam(value = "EntityGroup type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        checkParameter("ownerId", strOwnerId);
        checkParameter("ownerType", strOwnerType);
        try {
            EntityId ownerId = EntityIdFactory.getByTypeAndId(strOwnerType, strOwnerId);
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
            checkEntityId(ownerId, Operation.READ);
            Optional<EntityGroup> entityGroup = entityGroupService.findEntityGroupByTypeAndName(getTenantId(), ownerId,
                    groupType, EntityGroup.GROUP_ALL_NAME).get();
            if (entityGroup.isPresent()) {
                accessControlService.checkEntityGroupPermission(getCurrentUser(), Operation.READ, entityGroup.get());
                return toEntityGroupInfo(entityGroup.get());
            } else {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<EntityGroupInfo> toEntityGroupsInfo(List<EntityGroup> entityGroups) throws ThingsboardException {
        List<EntityGroupInfo> entityGroupsInfo = new ArrayList<>(entityGroups.size());
        for (EntityGroup entityGroup : entityGroups) {
            entityGroupsInfo.add(toEntityGroupInfo(entityGroup));
        }
        return entityGroupsInfo;
    }

    private EntityGroupInfo toEntityGroupInfo(EntityGroup entityGroup) throws ThingsboardException {
        EntityGroupInfo entityGroupInfo = new EntityGroupInfo(entityGroup);
        entityGroupInfo.setOwnerIds(ownersCacheService.getOwners(getTenantId(), entityGroup.getId(), entityGroup));
        return entityGroupInfo;
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/addEntities", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void addEntitiesToEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
                                         @RequestBody String[] strEntityIds) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkArrayParameter("entityIds", strEntityIds);
        EntityGroup entityGroup = null;
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            entityGroup = checkEntityGroupId(entityGroupId, Operation.ADD_TO_GROUP);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to add entities to entity group: " +
                        "Addition to entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            List<EntityId> entityIds = new ArrayList<>();
            for (String strEntityId : strEntityIds) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(entityGroup.getType(), strEntityId);
                checkEntityId(entityId, Operation.READ);
                entityIds.add(entityId);
            }
            entityGroupService.addEntitiesToEntityGroup(getTenantId(), entityGroupId, entityIds);
            if (entityGroup.getType() == EntityType.USER) {
                for (EntityId entityId : entityIds) {
                    userPermissionsService.onUserUpdatedOrRemoved(userService.findUserById(getTenantId(), new UserId(entityId.getId())));
                }
            } else if (entityGroup.getType() == EntityType.DEVICE) {
                DeviceGroupOtaPackage fw =
                        deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.FIRMWARE);
                DeviceGroupOtaPackage sw =
                        deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.SOFTWARE);
                if (fw != null || sw != null) {
                    List<DeviceId> deviceIds = entityIds.stream().map(id -> new DeviceId(id.getId())).collect(Collectors.toList());
                    otaPackageStateService.update(getTenantId(), deviceIds, fw != null, sw != null);
                }
            }

            for (EntityId entityId : entityIds) {
                logEntityAction((UUIDBased & EntityId) entityId, null,
                        null,
                        ActionType.ADDED_TO_ENTITY_GROUP, null, entityId.toString(), strEntityGroupId, entityGroup.getName());
                sendGroupEntityNotificationMsg(getTenantId(), entityId, EdgeEventActionType.ADDED_TO_ENTITY_GROUP, entityGroupId);
            }
        } catch (Exception e) {
            if (entityGroup != null) {
                EntityType entityType = entityGroup.getType();
                String groupName = entityGroup.getName();
                for (String strEntityId : strEntityIds) {
                    logEntityAction(emptyId(entityType), null,
                            null,
                            ActionType.ADDED_TO_ENTITY_GROUP, e, strEntityId, strEntityGroupId, groupName);
                }
            }
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/deleteEntities", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void removeEntitiesFromEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
                                              @RequestBody String[] strEntityIds) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkArrayParameter("entityIds", strEntityIds);
        EntityGroup entityGroup = null;
        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            entityGroup = checkEntityGroupId(entityGroupId, Operation.REMOVE_FROM_GROUP);
            if (entityGroup.isGroupAll()) {
                throw new ThingsboardException("Unable to remove entities from entity group: " +
                        "Removal from entity group 'All' is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
            }
            List<EntityId> entityIds = new ArrayList<>();
            for (String strEntityId : strEntityIds) {
                EntityId entityId = EntityIdFactory.getByTypeAndId(entityGroup.getType(), strEntityId);
                checkEntityId(entityId, Operation.READ);
                entityIds.add(entityId);
            }
            entityGroupService.removeEntitiesFromEntityGroup(getTenantId(), entityGroupId, entityIds);
            if (entityGroup.getType() == EntityType.USER) {
                for (EntityId entityId : entityIds) {
                    userPermissionsService.onUserUpdatedOrRemoved(userService.findUserById(getTenantId(), new UserId(entityId.getId())));
                }
            } else if (entityGroup.getType() == EntityType.DEVICE) {
                DeviceGroupOtaPackage fw =
                        deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.FIRMWARE);
                DeviceGroupOtaPackage sw =
                        deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.SOFTWARE);
                if (fw != null || sw != null) {
                    List<DeviceId> deviceIds = entityIds.stream().map(id -> new DeviceId(id.getId())).collect(Collectors.toList());
                    otaPackageStateService.update(getTenantId(), deviceIds, fw != null, sw != null);
                }
            }

            for (EntityId entityId : entityIds) {
                logEntityAction((UUIDBased & EntityId) entityId, null,
                        null,
                        ActionType.REMOVED_FROM_ENTITY_GROUP, null, entityId.toString(), strEntityGroupId, entityGroup.getName());
                sendGroupEntityNotificationMsg(getTenantId(), entityId,
                        EdgeEventActionType.REMOVED_FROM_ENTITY_GROUP, entityGroupId);
            }
        } catch (Exception e) {
            if (entityGroup != null) {
                EntityType entityType = entityGroup.getType();
                String groupName = entityGroup.getName();
                for (String strEntityId : strEntityIds) {
                    logEntityAction(emptyId(entityType), null,
                            null,
                            ActionType.REMOVED_FROM_ENTITY_GROUP, e, strEntityId, strEntityGroupId, groupName);
                }
            }
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public ShortEntityView getGroupEntity(
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkParameter("entityId", strEntityId);

        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
            EntityType entityType = entityGroup.getType();
            checkEntityGroupType(entityType);
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
            checkEntityId(entityId, Operation.READ);
            SecurityUser currentUser = getCurrentUser();
            MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
            ShortEntityView result = entityGroupService.findGroupEntity(getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, entityGroupId, entityId);
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/entities", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<ShortEntityView> getEntities(
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
            SecurityUser currentUser = getCurrentUser();
            MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
            return checkNotNull(entityGroupService.findGroupEntities(getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, entityGroupId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroups/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroupId> getEntityGroupsForEntity(
            @ApiParam(value = "Entity type", required = true, allowableValues = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("entityType") String strEntityType,
            @PathVariable("entityId") String strEntityId) throws ThingsboardException {
        checkParameter("entityType", strEntityType);
        checkParameter("entityId", strEntityId);
        try {
            EntityType entityType = checkStrEntityGroupType("entityType", strEntityType);
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
            checkEntityId(entityId, Operation.READ);
            return checkNotNull(entityGroupService.findEntityGroupsForEntity(getTenantId(), entityId).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroups", params = {"entityGroupIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroup> getEntityGroupsByIds(
            @RequestParam("entityGroupIds") String[] strEntityGroupIds) throws ThingsboardException {
        checkArrayParameter("entityGroupIds", strEntityGroupIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<EntityGroupId> entityGroupIds = new ArrayList<>();
            for (String strEntityGroupId : strEntityGroupIds) {
                entityGroupIds.add(new EntityGroupId(toUUID(strEntityGroupId)));
            }
            List<EntityGroup> entityGroups = checkNotNull(entityGroupService.findEntityGroupByIdsAsync(tenantId, entityGroupIds).get());
            return filterEntityGroupsByReadPermission(entityGroups);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/owners", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<ContactBased<?>> getOwners(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            List<ContactBased<?>> owners = new ArrayList<>();
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                if (accessControlService.hasPermission(getCurrentUser(), Resource.TENANT, Operation.READ)) {
                    owners.add(tenantService.findTenantById(getCurrentUser().getTenantId()));
                }
            }
            if (accessControlService.hasPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ)) {
                if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                    owners.addAll(customerService.findCustomersByTenantId(getTenantId(), pageLink)
                            .getData().stream().filter(customer -> !customer.isPublic()).collect(Collectors.toList()));
                } else {
                    Set<EntityId> ownerIds = ownersCacheService.getChildOwners(getTenantId(), getCurrentUser().getOwnerId());
                    if (!ownerIds.isEmpty()) {
                        List<CustomerId> customerIds = new ArrayList<>();
                        for (EntityId ownerId : ownerIds) {
                            customerIds.add(new CustomerId(ownerId.getId()));
                        }
                        owners.addAll(customerService.findCustomersByTenantIdAndIdsAsync(getTenantId(), customerIds).get()
                                .stream().filter(customer -> !customer.isPublic()).collect(Collectors.toList()));
                    }
                }
            }
            owners = owners.stream().sorted(entityComparator).filter(new EntityPageLinkFilter(pageLink)).collect(Collectors.toList());
            return toPageData(owners, pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/makePublic", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void makeEntityGroupPublic(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setPublic(true);
        groupPermission.setTenantId(getTenantId());

        EntityGroup entityGroup = null;

        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            entityGroup = checkEntityGroupId(entityGroupId, Operation.WRITE);
            if (!getCurrentUser().getOwnerId().equals(entityGroup.getOwnerId())) {
                throw permissionDenied();
            }
            checkPublicEntityGroupType(entityGroup.getType());

            if (entityGroup.isPublic()) {
                throw new ThingsboardException("Entity group is already public!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            EntityGroup publicUsers = customerService.findOrCreatePublicUserGroup(getTenantId(), getCurrentUser().getOwnerId());
            Role publicUserEntityGroupRole = customerService.findOrCreatePublicUserEntityGroupRole(getTenantId(), getCurrentUser().getOwnerId());

            groupPermission.setRoleId(publicUserEntityGroupRole.getId());
            groupPermission.setUserGroupId(publicUsers.getId());
            groupPermission.setEntityGroupId(entityGroup.getId());
            groupPermission.setEntityGroupType(entityGroup.getType());

            JsonNode additionalInfo = entityGroup.getAdditionalInfo();
            if (additionalInfo == null || additionalInfo instanceof NullNode) {
                additionalInfo = mapper.createObjectNode();
            }
            ((ObjectNode) additionalInfo).put("isPublic", true);
            ((ObjectNode) additionalInfo).put("publicCustomerId", publicUsers.getOwnerId().getId().toString());
            entityGroup.setAdditionalInfo(additionalInfo);

            GroupPermission savedGroupPermission = groupPermissionService.saveGroupPermission(getTenantId(), groupPermission);
            entityGroupService.saveEntityGroup(getTenantId(), entityGroup.getOwnerId(), entityGroup);
            userPermissionsService.onGroupPermissionUpdated(savedGroupPermission);

            logEntityAction(entityGroupId, null,
                    null,
                    ActionType.MADE_PUBLIC, null, strEntityGroupId, entityGroup.getName());

        } catch (Exception e) {
            if (entityGroup != null) {
                logEntityAction(entityGroup.getId(), null,
                        null,
                        ActionType.MADE_PUBLIC, e, strEntityGroupId, entityGroup.getName());
            }
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/makePrivate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void makeEntityGroupPrivate(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);

        EntityGroup entityGroup = null;

        try {
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            entityGroup = checkEntityGroupId(entityGroupId, Operation.WRITE);
            if (!getCurrentUser().getOwnerId().equals(entityGroup.getOwnerId())) {
                throw permissionDenied();
            }
            checkPublicEntityGroupType(entityGroup.getType());

            if (!entityGroup.isPublic()) {
                throw new ThingsboardException("Entity group is not public!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }

            Optional<GroupPermission> groupPermission = groupPermissionService.findPublicGroupPermissionByTenantIdAndEntityGroupId(getTenantId(), entityGroup.getId());
            if (groupPermission.isPresent()) {
                groupPermissionService.deleteGroupPermission(getTenantId(), groupPermission.get().getId());
                userPermissionsService.onGroupPermissionDeleted(groupPermission.get());
            }

            JsonNode additionalInfo = entityGroup.getAdditionalInfo();
            if (additionalInfo == null) {
                additionalInfo = mapper.createObjectNode();
            }
            ((ObjectNode) additionalInfo).put("isPublic", false);
            ((ObjectNode) additionalInfo).put("publicCustomerId", "");
            entityGroup.setAdditionalInfo(additionalInfo);

            entityGroupService.saveEntityGroup(getTenantId(), entityGroup.getOwnerId(), entityGroup);

            logEntityAction(entityGroupId, null,
                    null,
                    ActionType.MADE_PRIVATE, null, strEntityGroupId, entityGroup.getName());

        } catch (Exception e) {
            if (entityGroup != null) {
                logEntityAction(entityGroup.getId(), null,
                        null,
                        ActionType.MADE_PRIVATE, e, strEntityGroupId, entityGroup.getName());
            }
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/share", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void shareEntityGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
                                 @RequestBody ShareGroupRequest shareGroupRequest) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroup entityGroup;
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.CREATE);
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            entityGroup = checkEntityGroupId(entityGroupId, Operation.WRITE);
            checkSharableEntityGroupType(entityGroup.getType());

            EntityGroup userGroup;
            if (shareGroupRequest.isAllUserGroup()) {
                Optional<EntityGroup> userGroupOptional = entityGroupService.findEntityGroupByTypeAndName(getTenantId(), shareGroupRequest.getOwnerId(),
                        EntityType.USER, EntityGroup.GROUP_ALL_NAME).get();
                if (userGroupOptional.isPresent()) {
                    userGroup = userGroupOptional.get();
                } else {
                    throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
                }
            } else {
                userGroup = entityGroupService.findEntityGroupById(getTenantId(), shareGroupRequest.getUserGroupId());
            }
            accessControlService.checkEntityGroupPermission(getCurrentUser(), Operation.WRITE, userGroup);

            List<RoleId> roleIds;
            if (shareGroupRequest.getRoleIds() != null && !shareGroupRequest.getRoleIds().isEmpty()) {
                roleIds = shareGroupRequest.getRoleIds();
            } else {
                Role role;
                if (shareGroupRequest.isReadElseWrite()) {
                    role = roleService.findOrCreateReadOnlyEntityGroupRole(getTenantId(), getCurrentUser().getCustomerId());
                } else {
                    role = roleService.findOrCreateWriteEntityGroupRole(getTenantId(), getCurrentUser().getCustomerId());
                }
                roleIds = Collections.singletonList(role.getId());
            }

            for (RoleId roleId : roleIds) {
                GroupPermission groupPermission = new GroupPermission();
                groupPermission.setTenantId(getTenantId());
                groupPermission.setEntityGroupId(entityGroup.getId());
                groupPermission.setEntityGroupType(entityGroup.getType());
                groupPermission.setRoleId(roleId);
                groupPermission.setUserGroupId(userGroup.getId());

                GroupPermission savedGroupPermission = checkNotNull(groupPermissionService.saveGroupPermission(getTenantId(), groupPermission));
                userPermissionsService.onGroupPermissionUpdated(savedGroupPermission);
                logEntityAction(savedGroupPermission.getId(), savedGroupPermission, null,
                        ActionType.ADDED, null);
            }

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.GROUP_PERMISSION), null, null,
                    ActionType.ADDED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/{userGroupId}/{roleId}/share", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void shareEntityGroupToChildOwnerUserGroup(@PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
                                                      @PathVariable("userGroupId") String strUserGroupId,
                                                      @PathVariable("roleId") String strRoleId) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        checkParameter("userGroupId", strUserGroupId);
        checkParameter("roleId", strRoleId);
        try {
            EntityGroupId userGroupId = new EntityGroupId(toUUID(strUserGroupId));
            EntityGroup userGroup = entityGroupService.findEntityGroupById(getTenantId(), userGroupId);
            if (userGroup == null) {
                throw new ThingsboardException("User group with requested id: " + userGroupId + " wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            Set<EntityId> userGroupOwnerIds = ownersCacheService.fetchOwners(getTenantId(), userGroup.getOwnerId());
            EntityId currentUserOwnerId = getCurrentUser().getOwnerId();
            if (!CollectionUtils.isEmpty(userGroupOwnerIds) && userGroupOwnerIds.contains(currentUserOwnerId)) {
                EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
                EntityGroup entityGroup = entityGroupService.findEntityGroupById(getTenantId(), entityGroupId);
                if (entityGroup == null) {
                    throw new ThingsboardException("Entity group with requested id: " + entityGroupId + " wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
                }
                Set<EntityId> groupToShareOwnerIds = ownersCacheService.fetchOwners(getTenantId(), entityGroup.getOwnerId());
                Set<Operation> mergedOperations = new HashSet<>();
                MergedUserPermissions userPermissions = getCurrentUser().getUserPermissions();
                if (groupToShareOwnerIds.contains(currentUserOwnerId)) {
                    if (hasGenenericPermissionToShareGroup(entityGroup.getType())) {
                        Map<Resource, Set<Operation>> genericPermissions = userPermissions.getGenericPermissions();
                        genericPermissions.forEach((resource, operations) -> {
                            if (resource.equals(Resource.ALL) || (resource.getEntityType().isPresent() && resource.getEntityType().get().equals(EntityType.ENTITY_GROUP))) {
                                mergedOperations.addAll(operations);
                            }
                        });
                    }
                }
                if (hasGroupPermissionsToShareGroup(entityGroupId)) {
                    Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions = userPermissions.getGroupPermissions();
                    MergedGroupPermissionInfo mergedGroupPermissionInfo = groupPermissions.get(entityGroupId);
                    mergedOperations.addAll(mergedGroupPermissionInfo.getOperations());
                }
                RoleId roleId = new RoleId(toUUID(strRoleId));
                Role role = roleService.findRoleById(getTenantId(), roleId);
                if (role == null) {
                    throw new ThingsboardException("Role with requested id: " + roleId + " wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
                }
                Set<EntityId> roleOwnerIds = ownersCacheService.fetchOwners(getTenantId(), role.getOwnerId());
                if (roleOwnerIds.contains(currentUserOwnerId) || userGroupOwnerIds.containsAll(roleOwnerIds)) {
                    shareGroup(role, userGroup, entityGroup, mergedOperations);
                } else {
                    throw permissionDenied();
                }
            } else {
                throw permissionDenied();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/entityGroup/{entityGroupId}/{groupType}", method = RequestMethod.POST)
    @ResponseBody
    public EntityGroup assignEntityGroupToEdge(@PathVariable("edgeId") String strEdgeId,
                                               @ApiParam(value = "EntityGroup type", required = true, allowableValues = "ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType,
                                               @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);

            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            checkEntityGroupId(entityGroupId, Operation.ASSIGN_TO_EDGE);
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);

            EntityGroup savedEntityGroup = checkNotNull(entityGroupService.assignEntityGroupToEdge(getCurrentUser().getTenantId(), entityGroupId, edgeId, groupType));

            logEntityAction(entityGroupId, savedEntityGroup,
                    null,
                    ActionType.ASSIGNED_TO_EDGE, null, strEntityGroupId, savedEntityGroup.getName(), strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedEntityGroup.getId(), EdgeEventActionType.ASSIGNED_TO_EDGE);

            return savedEntityGroup;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ENTITY_GROUP), null,
                    null,
                    ActionType.ASSIGNED_TO_EDGE, e, strEntityGroupId, strEdgeId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/entityGroup/{entityGroupId}/{groupType}", method = RequestMethod.DELETE)
    @ResponseBody
    public EntityGroup unassignEntityGroupFromEdge(@PathVariable("edgeId") String strEdgeId,
                                                   @ApiParam(value = "EntityGroup type", required = true, allowableValues = "ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType,
                                                   @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            Edge edge = checkEdgeId(edgeId, Operation.READ);
            EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.UNASSIGN_FROM_EDGE);
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);

            EntityGroup savedEntityGroup = checkNotNull(entityGroupService.unassignEntityGroupFromEdge(getCurrentUser().getTenantId(), entityGroupId, edgeId, groupType));

            logEntityAction(entityGroupId, entityGroup,
                    null,
                    ActionType.UNASSIGNED_FROM_EDGE, null, strEntityGroupId, savedEntityGroup.getName(), strEdgeId, edge.getName());

            sendEntityAssignToEdgeNotificationMsg(getTenantId(), edgeId, savedEntityGroup.getId(), EdgeEventActionType.UNASSIGNED_FROM_EDGE);

            return savedEntityGroup;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.ENTITY_GROUP), null,
                    null,
                    ActionType.UNASSIGNED_FROM_EDGE, e, strEntityGroupId);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/allEntityGroups/edge/{edgeId}/{groupType}", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityGroupInfo> getAllEdgeEntityGroups(
            @PathVariable("edgeId") String strEdgeId,
            @ApiParam(value = "EntityGroup type", required = true, allowableValues = "ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(UUID.fromString(strEdgeId));
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
            checkEdgeId(edgeId, Operation.READ);
            MergedGroupTypePermissionInfo groupTypePermissionInfo = getCurrentUser().getUserPermissions().getReadGroupPermissions().get(groupType);
            if (groupTypePermissionInfo.isHasGenericRead()) {
                List<EntityGroup> result = new ArrayList<>();
                PageLink pageLink = new PageLink(DEFAULT_ENTITY_GROUP_LIMIT);
                PageData<EntityGroup> pageData;
                do {
                    pageData = entityGroupService.findEdgeEntityGroupsByType(getTenantId(), edgeId, groupType, pageLink);
                    if (pageData.getData().size() > 0) {
                        result.addAll(pageData.getData());
                        if (pageData.hasNext()) {
                            pageLink = pageLink.nextPageLink();
                        }
                    }
                } while (pageData.hasNext());
                return checkNotNull(toEntityGroupsInfo(result));
            } else {
                throw permissionDenied();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroups/edge/{edgeId}/{groupType}", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityGroupInfo> getEdgeEntityGroups(
            @PathVariable("edgeId") String strEdgeId,
            @ApiParam(value = "EntityGroup type", required = true, allowableValues = "ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD") @PathVariable("groupType") String strGroupType,
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        try {
            EdgeId edgeId = new EdgeId(UUID.fromString(strEdgeId));
            EntityType groupType = checkStrEntityGroupType("groupType", strGroupType);
            checkEdgeId(edgeId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            MergedGroupTypePermissionInfo groupTypePermissionInfo = getCurrentUser().getUserPermissions().getReadGroupPermissions().get(groupType);
            if (groupTypePermissionInfo.isHasGenericRead()) {
                return toEntityGroupsInfo(entityGroupService.findEdgeEntityGroupsByType(getTenantId(), edgeId, groupType, pageLink));
            } else {
                throw permissionDenied();
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private PageData<EntityGroupInfo> toEntityGroupsInfo(PageData<EntityGroup> data) throws ThingsboardException {
        List<EntityGroupInfo> entityGroupsInfo = new ArrayList<>(data.getData().size());
        for (EntityGroup entityGroup : data.getData()) {
            entityGroupsInfo.add(toEntityGroupInfo(entityGroup));
        }
        return new PageData<>(entityGroupsInfo, data.getTotalPages(), data.getTotalElements(), data.hasNext());
    }

    private void shareGroup(Role role, EntityGroup userGroup, EntityGroup entityGroup, Set<Operation> mergedOperations) throws ThingsboardException, IOException {
        CollectionType collectionType = TypeFactory.defaultInstance().constructCollectionType(List.class, Operation.class);
        List<Operation> roleOperations = mapper.readValue(role.getPermissions().toString(), collectionType);
        if (!mergedOperations.isEmpty() && (mergedOperations.contains(Operation.ALL) || mergedOperations.containsAll(roleOperations))) {
            groupPermissionService.saveGroupPermission(getTenantId(), new GroupPermission(getTenantId(), userGroup.getId(), role.getId(), entityGroup.getId(), entityGroup.getId().getEntityType(), false));
        } else {
            throw permissionDenied();
        }
    }

    private boolean hasGenenericPermissionToShareGroup(EntityType type) throws ThingsboardException {
        return getCurrentUser().getUserPermissions().hasGenericPermission(Resource.groupResourceFromGroupType(type), Operation.SHARE_GROUP);
    }

    private boolean hasGroupPermissionsToShareGroup(EntityGroupId entityGroupId) throws ThingsboardException {
        return getCurrentUser().getUserPermissions().hasGroupPermissions(entityGroupId, Operation.SHARE_GROUP);
    }

    private List<EntityGroup> filterEntityGroupsByReadPermission(List<EntityGroup> entityGroups) {
        return entityGroups.stream().filter(entityGroup -> {
            try {
                return accessControlService.hasEntityGroupPermission(getCurrentUser(), Operation.READ, entityGroup);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }
}
