/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.grouppermission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class GroupPermissionServiceImpl extends AbstractEntityService implements GroupPermissionService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_USER_GROUP_ID = "Incorrect userGroupId ";
    public static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";
    public static final String INCORRECT_GROUP_PERMISSION_ID = "Incorrect groupPermissionId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    private GroupPermissionDao groupPermissionDao;

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private DataValidator<GroupPermission> groupPermissionValidator;

    @Override
    public GroupPermission saveGroupPermission(TenantId tenantId, GroupPermission groupPermission) {
        log.trace("Executing save groupPermission [{}]", groupPermission);
        groupPermissionValidator.validate(groupPermission, GroupPermission::getTenantId);
        return groupPermissionDao.save(tenantId, groupPermission);
    }

    @Override
    public GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing findGroupPermissionById [{}]", groupPermissionId);
        validateId(groupPermissionId, INCORRECT_GROUP_PERMISSION_ID + groupPermissionId);
        return groupPermissionDao.findById(tenantId, groupPermissionId.getId());
    }

    @Override
    public ListenableFuture<GroupPermissionInfo> findGroupPermissionInfoByIdAsync(TenantId tenantId,
                                                                                  GroupPermissionId groupPermissionId,
                                                                                  boolean isUserGroup) {
        log.trace("Executing findGroupPermissionInfoByIdAsync [{}]", groupPermissionId);
        validateId(groupPermissionId, INCORRECT_GROUP_PERMISSION_ID + groupPermissionId);
        GroupPermission groupPermission = groupPermissionDao.findById(tenantId, groupPermissionId.getId());
        if (isUserGroup) {
            return fetchUserGroupPermissionInfoAsync(tenantId, groupPermission);
        } else {
            return fetchEntityGroupPermissionInfoAsync(tenantId, groupPermission);
        }
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}], pageLink [{}]", tenantId, userGroupId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndUserGroupIdAndRoleId, tenantId [{}], userGroupId [{}], roleId [{}], pageLink [{}]", tenantId, userGroupId, roleId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupIdAndRoleId(tenantId.getId(),
                userGroupId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(TenantId tenantId,
                                                                                                              EntityGroupId entityGroupId,
                                                                                                              EntityGroupId userGroupId,
                                                                                                              RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId, tenantId [{}], entityGroupId [{}], " +
                "userGroupId [{}], roleId [{}], pageLink [{}]", tenantId, userGroupId, roleId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(tenantId.getId(),
                entityGroupId.getId(), userGroupId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        PageData<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), new PageLink(Integer.MAX_VALUE));
        return toGroupPermissionInfoListAsync(tenantId, groupPermissions.getData(), true);
    }

    @Override
    public ListenableFuture<List<GroupPermissionInfo>> loadUserGroupPermissionInfoListAsync(TenantId tenantId, List<GroupPermission> permissions) {
        log.trace("Executing loadUserGroupPermissionInfoListAsync, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return toGroupPermissionInfoListAsync(tenantId, permissions, true);
    }

    @Override
    public List<GroupPermission> findGroupPermissionListByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing findGroupPermissionListByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), new PageLink(Integer.MAX_VALUE)).getData();
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}], pageLink [{}]", tenantId, entityGroupId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), pageLink);
    }

    @Override
    public Optional<GroupPermission> findPublicGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findPublicGroupPermissionByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        List<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), new PageLink(Integer.MAX_VALUE)).getData();
        List<GroupPermission> permissions = groupPermissions.stream().filter((GroupPermission::isPublic)).collect(Collectors.toList());
        if (permissions.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(permissions.get(0));
        }
    }

    @Override
    public ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        PageData<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), new PageLink(Integer.MAX_VALUE));
        return toGroupPermissionInfoListAsync(tenantId, groupPermissions.getData(), false);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndRoleId(TenantId tenantId, RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndRuleId, tenantId [{}], ruleId [{}], pageLink [{}]", tenantId, roleId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndRoleId(tenantId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<GroupPermission> findGroupPermissionByIdAsync(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing findGroupPermissionByIdAsync [{}]", groupPermissionId);
        validateId(groupPermissionId, INCORRECT_GROUP_PERMISSION_ID + groupPermissionId);
        return groupPermissionDao.findByIdAsync(tenantId, groupPermissionId.getId());
    }

    @Override
    public void deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing deleteGroupPermission [{}]", groupPermissionId);
        validateId(groupPermissionId, INCORRECT_GROUP_PERMISSION_ID + groupPermissionId);
        GroupPermission groupPermission = groupPermissionDao.findById(tenantId, groupPermissionId.getId());
        if (groupPermission == null) {
            throw new IncorrectParameterException("Unable to delete non-existent group permission.");
        }
        if (groupPermission.isPublic() && groupPermission.getEntityGroupId() != null) {
            EntityGroup entityGroup = entityGroupDao.findById(tenantId, groupPermission.getEntityGroupId().getId());
            if (entityGroup != null) {
                JsonNode additionalInfo = entityGroup.getAdditionalInfo();
                if (additionalInfo == null) {
                    additionalInfo = new ObjectMapper().createObjectNode();
                }
                ((ObjectNode) additionalInfo).put("isPublic", false);
                ((ObjectNode) additionalInfo).put("publicCustomerId", "");
                entityGroup.setAdditionalInfo(additionalInfo);
                entityGroupDao.save(tenantId, entityGroup);
            }
        }
        deleteEntityRelations(tenantId, groupPermissionId);
        groupPermissionDao.removeById(tenantId, groupPermissionId.getId());
    }

    @Override
    public void deleteGroupPermissionsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteGroupPermissionsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantGroupPermissionRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        userGroupPermissionRemover.removeEntities(tenantId, userGroupId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        entityGroupPermissionRemover.removeEntities(tenantId, entityGroupId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndRoleId, tenantId [{}], roleId [{}]", tenantId, roleId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        rolePermissionRemover.removeEntities(tenantId, roleId);
    }

    private ListenableFuture<List<GroupPermissionInfo>> toGroupPermissionInfoListAsync(TenantId tenantId, List<GroupPermission> groupPermissions, boolean isUserGroup) {
        List<ListenableFuture<GroupPermissionInfo>> groupPermissionInfoFutureList = new ArrayList<>();
        groupPermissions.forEach(groupPermission -> {
            if (isUserGroup) {
                groupPermissionInfoFutureList.add(fetchUserGroupPermissionInfoAsync(tenantId, groupPermission));
            } else {
                groupPermissionInfoFutureList.add(fetchEntityGroupPermissionInfoAsync(tenantId, groupPermission));
            }
        });
        return Futures.successfulAsList(groupPermissionInfoFutureList);
    }

    private ListenableFuture<GroupPermissionInfo> fetchUserGroupPermissionInfoAsync(TenantId tenantId, GroupPermission groupPermission) {
        ListenableFuture<Role> roleFuture = roleDao.findByIdAsync(tenantId, groupPermission.getRoleId().getId());
        return Futures.transformAsync(roleFuture, role -> {
            GroupPermissionInfo groupPermissionInfo = new GroupPermissionInfo(groupPermission);
            groupPermissionInfo.setRole(role);
            if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
                ListenableFuture<EntityGroup> entityGroup = entityGroupService.findEntityGroupByIdAsync(tenantId, groupPermission.getEntityGroupId());
                return Futures.transformAsync(entityGroup, entityGroup1 -> {
                    groupPermissionInfo.setEntityGroupName(entityGroup1.getName());
                    groupPermissionInfo.setEntityGroupType(entityGroup1.getType());
                    EntityId ownerId = entityGroup1.getOwnerId();
                    groupPermissionInfo.setEntityGroupOwnerId(ownerId);
                    ListenableFuture <String> ownerName = entityService.fetchEntityNameAsync(tenantId, ownerId);
                    return Futures.transform(ownerName, ownerName1 -> {
                        groupPermissionInfo.setEntityGroupOwnerName(ownerName1);
                        return groupPermissionInfo;
                    }, MoreExecutors.directExecutor());
                }, MoreExecutors.directExecutor());
            } else {
                return Futures.immediateFuture(groupPermissionInfo);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<GroupPermissionInfo> fetchEntityGroupPermissionInfoAsync(TenantId tenantId, GroupPermission groupPermission) {
        ListenableFuture<Role> roleFuture = roleDao.findByIdAsync(tenantId, groupPermission.getRoleId().getId());
        return Futures.transformAsync(roleFuture, role -> {
            GroupPermissionInfo groupPermissionInfo = new GroupPermissionInfo(groupPermission);
            groupPermissionInfo.setRole(role);
            ListenableFuture<EntityGroup> userGroup = entityGroupService.findEntityGroupByIdAsync(tenantId, groupPermission.getUserGroupId());
            return Futures.transformAsync(userGroup, userGroup1 -> {
                groupPermissionInfo.setUserGroupName(userGroup1.getName());
                EntityId ownerId = userGroup1.getOwnerId();
                groupPermissionInfo.setUserGroupOwnerId(ownerId);
                ListenableFuture <String> ownerName = entityService.fetchEntityNameAsync(tenantId, ownerId);
                return Futures.transform(ownerName, ownerName1 -> {
                    groupPermissionInfo.setUserGroupOwnerName(ownerName1);
                    return groupPermissionInfo;
                }, MoreExecutors.directExecutor());
            }, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    private PaginatedRemover<TenantId, GroupPermission> tenantGroupPermissionRemover = new PaginatedRemover<TenantId, GroupPermission>() {

        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantId(tenantId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }
    };

    private PaginatedRemover<EntityGroupId, GroupPermission> userGroupPermissionRemover = new PaginatedRemover<EntityGroupId, GroupPermission>() {
        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }
    };

    private PaginatedRemover<EntityGroupId, GroupPermission> entityGroupPermissionRemover = new PaginatedRemover<EntityGroupId, GroupPermission>() {
        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }
    };

    private PaginatedRemover<RoleId, GroupPermission> rolePermissionRemover = new PaginatedRemover<RoleId, GroupPermission>() {
        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, RoleId roleId, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantIdAndRoleId(tenantId.getId(), roleId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }
    };


}
