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
package org.thingsboard.server.dao.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.ColumnType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupConfiguration;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.group.SortOrder;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityQueryDao;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.extractConstraintViolationException;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateEntityIds;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("EntityGroupDaoService")
@Slf4j
public class BaseEntityGroupService extends AbstractEntityService implements EntityGroupService {

    public static final String ENTITY_GROUP_RELATION_PREFIX = "ENTITY_GROUP_";
    public static final String INCORRECT_PARENT_ENTITY_ID = "Incorrect parentEntityId ";

    public static final String INCORRECT_OWNER_ENTITY_IDS = "Incorrect ownerEntityIds ";
    public static final String INCORRECT_GROUP_TYPE = "Incorrect groupType ";
    public static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    public static final String INCORRECT_ENTITY_ID = "Incorrect entityId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String UNABLE_TO_FIND_ENTITY_GROUP_BY_ID = "Unable to find entity group by id ";
    public static final String EDGE_ENTITY_GROUP_RELATION_PREFIX = "EDGE_ENTITY_GROUP_";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ReentrantLock roleCreationLock = new ReentrantLock();

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private EntityGroupInfoDao entityGroupInfoDao;

    @Autowired
    private RelationDao relationDao;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EntityQueryDao entityQueryDao;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private JpaExecutorService executorService;

    @Override
    public EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupById [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupDao.findById(tenantId, entityGroupId.getId());
    }

    @Override
    public EntityGroupInfo findEntityGroupInfoById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupInfoById [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupInfoDao.findById(tenantId, entityGroupId.getId());
    }

    @Override
    public EntityInfo findEntityGroupEntityInfoById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupEntityInfoById [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupInfoDao.findEntityGroupEntityInfoById(tenantId, entityGroupId.getId());
    }

    @Override
    public ListenableFuture<EntityGroup> findEntityGroupByIdAsync(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupByIdAsync [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupDao.findByIdAsync(tenantId, entityGroupId.getId());
    }

    @Override
    public ListenableFuture<EntityGroupInfo> findEntityGroupInfoByIdAsync(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupInfoByIdAsync [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return entityGroupInfoDao.findByIdAsync(tenantId, entityGroupId.getId());
    }

    @Override
    public EntityGroup saveEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup) {
        log.trace("Executing saveEntityGroup [{}]", entityGroup);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        entityGroup = new EntityGroup(entityGroup);
        if (entityGroup.getId() == null) {
            entityGroup.setOwnerId(parentEntityId);
        }
        new EntityGroupValidator().validate(entityGroup, data -> tenantId);
        if (entityGroup.getId() == null && entityGroup.getConfiguration() == null) {
            EntityGroupConfiguration entityGroupConfiguration =
                    EntityGroupConfiguration.createDefaultEntityGroupConfiguration(entityGroup.getType());
            ObjectNode jsonConfiguration = (ObjectNode) JacksonUtil.valueToTree(entityGroupConfiguration);
            jsonConfiguration.putObject("settings");
            jsonConfiguration.putObject("actions");
            entityGroup.setConfiguration(jsonConfiguration);
        }
        EntityGroup savedEntityGroup;
        try {
            savedEntityGroup = entityGroupDao.save(tenantId, entityGroup);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && "group_name_per_owner_unq_key".equalsIgnoreCase(e.getConstraintName())) {
                throw new DataValidationException("Entity Group with such name, type and owner already exists!");
            } else {
                throw t;
            }
        }
        return savedEntityGroup;
    }

    @Override
    public EntityGroup createEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityType groupType) {
        log.trace("Executing createEntityGroupAll, parentEntityId [{}], groupType [{}]", parentEntityId, groupType);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(EntityGroup.GROUP_ALL_NAME);
        entityGroup.setType(groupType);
        return saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    @Override
    public EntityGroup findOrCreateUserGroup(TenantId tenantId, EntityId parentEntityId, String groupName, String description) {
        log.trace("Executing findOrCreateUserGroup, parentEntityId [{}], groupName [{}]", parentEntityId, groupName);
        return findOrCreateEntityGroup(tenantId, parentEntityId, EntityType.USER, groupName, description, null);
    }

    @Override
    public EntityGroup findOrCreateEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String groupName,
                                               String description, CustomerId publicCustomerId) {
        log.trace("Executing findOrCreateEntityGroup, parentEntityId [{}], groupType [{}], groupName [{}]", parentEntityId, groupType, groupName);
        try {
            Optional<EntityGroup> entityGroupOptional = findEntityGroupByTypeAndName(tenantId, parentEntityId, groupType, groupName);
            if (entityGroupOptional.isPresent()) {
                return entityGroupOptional.get();
            } else {
                EntityGroup entityGroup = new EntityGroup();
                entityGroup.setName(groupName);
                entityGroup.setType(groupType);
                JsonNode additionalInfo = entityGroup.getAdditionalInfo();
                if (additionalInfo == null) {
                    additionalInfo = mapper.createObjectNode();
                }
                ((ObjectNode) additionalInfo).put("description", description);
                if (publicCustomerId != null && !publicCustomerId.isNullUid()) {
                    ((ObjectNode) additionalInfo).put("isPublic", true);
                    ((ObjectNode) additionalInfo).put("publicCustomerId", publicCustomerId.getId().toString());
                }
                entityGroup.setAdditionalInfo(additionalInfo);
                return saveEntityGroup(tenantId, parentEntityId, entityGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable find or create entity group!", e);
        }
    }

    @Override
    public Optional<EntityGroupInfo> findOwnerEntityGroupInfo(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String groupName) {
        log.trace("Executing findOwnerEntityGroupInfo, parentEntityId [{}], groupType [{}], groupName [{}]", parentEntityId, groupType, groupName);
        try {
            return findEntityGroupInfoByTypeAndName(tenantId, parentEntityId, groupType, groupName);
        } catch (Exception e) {
            throw new RuntimeException("Entity group with name: " + groupName + " and type: " + groupType + " doesn't exist!", e);
        }
    }

    @Override
    public EntityGroup findOrCreateTenantUsersGroup(TenantId tenantId) {

        // User Group 'Tenant Users' -> 'Tenant User' role -> Read only permissions

        EntityGroup tenantUsers = findOrCreateUserGroup(tenantId,
                tenantId, EntityGroup.GROUP_TENANT_USERS_NAME, "Autogenerated Tenant Users group with read-only permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateTenantUserRole, TenantId [{}]", tenantId);
            Role tenantUserRole = roleService.findOrCreateTenantUserRole();
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, tenantUsers.getType(), tenantUsers.getName());
            findOrCreateUserGroupPermission(tenantId, tenantUsers.getId(), tenantUserRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateTenantUserRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return tenantUsers;
    }

    @Override
    public EntityGroup findOrCreateTenantAdminsGroup(TenantId tenantId) {

        // User Group 'Tenant Administrators' -> 'Tenant Administrator' role -> All permissions

        EntityGroup tenantAdmins = findOrCreateUserGroup(tenantId,
                tenantId, EntityGroup.GROUP_TENANT_ADMINS_NAME, "Autogenerated Tenant Administrators group with all permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateTenantAdminRole, TenantId [{}]", tenantId);
            Role tenantAdminRole = roleService.findOrCreateTenantAdminRole();
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, tenantAdmins.getType(), tenantAdmins.getName());
            findOrCreateUserGroupPermission(tenantId, tenantAdmins.getId(), tenantAdminRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateTenantAdminRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return tenantAdmins;
    }

    @Override
    public EntityGroup findOrCreateCustomerUsersGroup(TenantId tenantId, CustomerId customerId, CustomerId parentCustomerId) {

        // User Group 'Customer Users' -> 'Customer User' role -> Read only permissions

        EntityGroup customerUsers = findOrCreateUserGroup(tenantId, customerId,
                EntityGroup.GROUP_CUSTOMER_USERS_NAME, "Autogenerated Customer Users group with read-only permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateCustomerUserRole, TenantId [{}]", tenantId);
            Role customerUserRole = roleService.findOrCreateCustomerUserRole(tenantId, parentCustomerId);
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, customerUsers.getType(), customerUsers.getName());
            findOrCreateUserGroupPermission(tenantId, customerUsers.getId(), customerUserRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateCustomerUserRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return customerUsers;
    }

    @Override
    public EntityGroup findOrCreateCustomerAdminsGroup(TenantId tenantId, CustomerId customerId, CustomerId parentCustomerId) {

        // User Group 'Customer Administrators' -> 'Customer Administrator' role -> All permissions

        EntityGroup customerAdmins = findOrCreateUserGroup(tenantId, customerId,
                EntityGroup.GROUP_CUSTOMER_ADMINS_NAME, "Autogenerated Customer Administrators group with all permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateCustomerAdminRole, TenantId [{}]", tenantId);
            Role customerAdminRole = roleService.findOrCreateCustomerAdminRole(tenantId, parentCustomerId);
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, customerAdmins.getType(), customerAdmins.getName());
            findOrCreateUserGroupPermission(tenantId, customerAdmins.getId(), customerAdminRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateCustomerAdminRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return customerAdmins;
    }

    @Override
    public EntityGroup findOrCreatePublicUsersGroup(TenantId tenantId, CustomerId customerId) {
        EntityGroup publicUsers = findOrCreateUserGroup(tenantId, customerId,
                EntityGroup.GROUP_PUBLIC_USERS_NAME, "Autogenerated Public Users group with read-only permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreatePublicUserRole, TenantId [{}]", tenantId);
            Role publicUserRole = roleService.findOrCreatePublicUserRole(tenantId, customerId);
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, publicUsers.getType(), publicUsers.getName());
            findOrCreateUserGroupPermission(tenantId, publicUsers.getId(), publicUserRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreatePublicUserRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return publicUsers;
    }

    @Override
    public EntityGroup findOrCreateReadOnlyEntityGroupForCustomer(TenantId tenantId, CustomerId customerId, EntityType groupType) {

        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new RuntimeException("Customer with id '" + customerId.getId().toString() + "' is not present in database!");
        }

        String groupName = customer.getTitle();
        String description = "Autogenerated ";
        if (customer.isPublic()) {
            description += "Public ";
        }
        switch (groupType) {
            case DEVICE:
                groupName += " Devices";
                description += "Device";
                break;
            case ASSET:
                groupName += " Assets";
                description += "Asset";
                break;
            case ENTITY_VIEW:
                groupName += " Entity Views";
                description += "Entity View";
                break;
            case EDGE:
                groupName += " Edges";
                description += "Edge";
                break;
            case DASHBOARD:
                groupName += " Dashboards";
                description += "Dashboard";
                break;
            default:
                throw new RuntimeException("Invalid entity group type '" + groupType + "' specified for read-only entity group for customer!");
        }
        if (customer.isPublic()) {
            description += " group";
        } else {
            description += " group with read-only access for customer '" + customer.getTitle() + "'";
        }
        EntityGroup group;
        if (customer.isPublic()) {
            group = findOrCreateEntityGroup(tenantId, tenantId, groupType, groupName, description, customer.getId());
            EntityGroup publicUsers = findOrCreatePublicUsersGroup(tenantId, customer.getId());
            Role publicUserEntityGroupRole = roleService.findOrCreatePublicUsersEntityGroupRole(tenantId, customer.getId());
            findOrCreateEntityGroupPermission(tenantId, group.getId(), group.getType(), publicUsers.getId(), publicUserEntityGroupRole.getId(), true);
        } else {
            group = findOrCreateEntityGroup(tenantId, tenantId, groupType, groupName, description, null);
            Role readOnlyGroupRole = roleService.findOrCreateReadOnlyEntityGroupRole(tenantId, null);
            EntityGroup customerUsers = findOrCreateCustomerUsersGroup(tenantId, customer.getId(), null);
            findOrCreateEntityGroupPermission(tenantId, group.getId(), group.getType(), customerUsers.getId(), readOnlyGroupRole.getId(), false);
        }
        return group;
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findPublicUserGroupAsync(TenantId tenantId, CustomerId publicCustomerId) {
        log.trace("Executing findPublicUserGroupAsync, tenantId [{}], publicCustomerId [{}]", tenantId, publicCustomerId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(publicCustomerId, INCORRECT_CUSTOMER_ID + publicCustomerId);
        return findEntityGroupByTypeAndNameAsync(tenantId, publicCustomerId, EntityType.USER, EntityGroup.GROUP_PUBLIC_USERS_NAME);
    }

    private GroupPermission findOrCreateUserGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId) {
        List<GroupPermission> userGroupPermissions =
                groupPermissionService.findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(tenantId,
                        userGroupId, roleId, new PageLink(Integer.MAX_VALUE)).getData();
        if (userGroupPermissions.isEmpty()) {
            GroupPermission userGroupPermission = new GroupPermission();
            userGroupPermission.setTenantId(tenantId);
            userGroupPermission.setUserGroupId(userGroupId);
            userGroupPermission.setRoleId(roleId);
            return groupPermissionService.saveGroupPermission(tenantId, userGroupPermission);
        } else {
            return userGroupPermissions.get(0);
        }
    }

    private GroupPermission findOrCreateEntityGroupPermission(TenantId tenantId, EntityGroupId entityGroupId,
                                                              EntityType entityGroupType,
                                                              EntityGroupId userGroupId, RoleId roleId, boolean isPublic) {
        List<GroupPermission> entityGroupPermissions =
                groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(tenantId,
                        entityGroupId, userGroupId, roleId, new PageLink(Integer.MAX_VALUE)).getData();
        if (entityGroupPermissions.isEmpty()) {
            GroupPermission entityGroupPermission = new GroupPermission();
            entityGroupPermission.setTenantId(tenantId);
            entityGroupPermission.setEntityGroupId(entityGroupId);
            entityGroupPermission.setEntityGroupType(entityGroupType);
            entityGroupPermission.setUserGroupId(userGroupId);
            entityGroupPermission.setRoleId(roleId);
            entityGroupPermission.setPublic(isPublic);
            return groupPermissionService.saveGroupPermission(tenantId, entityGroupPermission);
        } else {
            return entityGroupPermissions.get(0);
        }
    }

    @Override
    public void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing deleteEntityGroup [{}]", entityGroupId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        groupPermissionService.deleteGroupPermissionsByTenantIdAndUserGroupId(tenantId, entityGroupId);
        groupPermissionService.deleteGroupPermissionsByTenantIdAndEntityGroupId(tenantId, entityGroupId);
        deleteEntityRelations(tenantId, entityGroupId);
        entityGroupDao.removeById(tenantId, entityGroupId.getId());
    }

    @Override
    public PageData<EntityGroup> findAllEntityGroupsByParentRelation(TenantId tenantId, EntityId parentEntityId, PageLink pageLink) {
        log.trace("Executing findAllEntityGroupsByParentRelation, parentEntityId [{}], pageLink [{}]", parentEntityId, pageLink);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        validatePageLink(pageLink);
        return this.entityGroupDao.findAllEntityGroupsByParentRelation(tenantId.getId(), parentEntityId.getId(), parentEntityId.getEntityType(), pageLink);
    }

    @Override
    public void deleteAllEntityGroups(TenantId tenantId, EntityId parentEntityId) {
        log.trace("Executing deleteAllEntityGroups, parentEntityId [{}]", parentEntityId);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        entityGroupsRemover.removeEntities(tenantId, parentEntityId);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityId parentEntityId,
                                                        EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupsByType, parentEntityId [{}], groupType [{}], pageLink [{}]", parentEntityId, groupType, pageLink);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupDao.findEntityGroupsByType(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByType(TenantId tenantId, EntityId parentEntityId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByType, parentEntityId [{}], groupType [{}], pageLink [{}]", parentEntityId, groupType, pageLink);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupsByType(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByType(TenantId tenantId, EntityId parentEntityId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByType, parentEntityId [{}], groupType [{}], pageLink [{}]", parentEntityId, groupType, pageLink);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupEntityInfosByType(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByOwnersAndType(TenantId tenantId, List<EntityId> ownerIds, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByOwnersAndType, ownerIds [{}], groupType [{}], pageLink [{}]", ownerIds, groupType, pageLink);
        validateEntityIds(ownerIds, INCORRECT_OWNER_ENTITY_IDS + ownerIds);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupsByOwnerIdsAndType(tenantId.getId(),
                ownerIds.stream().map(EntityId::getId).collect(Collectors.toList()), groupType, pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByOwnersAndType(TenantId tenantId, List<EntityId> ownerIds, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByOwnersAndType, ownerIds [{}], groupType [{}], pageLink [{}]", ownerIds, groupType, pageLink);
        validateEntityIds(ownerIds, INCORRECT_OWNER_ENTITY_IDS + ownerIds);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupEntityInfosByOwnerIdsAndType(tenantId.getId(),
                ownerIds.stream().map(EntityId::getId).collect(Collectors.toList()), groupType, pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByIds(TenantId tenantId, List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByIds, entityGroupIds [{}], pageLink [{}]", entityGroupIds, pageLink);
        validateIds(entityGroupIds, "Incorrect entityGroupIds " + entityGroupIds);
        validatePageLink(pageLink);
        return entityGroupInfoDao.findEntityGroupsByIds(tenantId.getId(), toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByIds(TenantId tenantId, List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByIds, entityGroupIds [{}], pageLink [{}]", entityGroupIds, pageLink);
        validateIds(entityGroupIds, "Incorrect entityGroupIds " + entityGroupIds);
        validatePageLink(pageLink);
        return entityGroupInfoDao.findEntityGroupEntityInfosByIds(tenantId.getId(), toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByTypeOrIds(TenantId tenantId, EntityId parentEntityId, EntityType groupType,
                                                                     List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByTypeOrIds, parentEntityId [{}], groupType [{}], entityGroupIds [{}], pageLink [{}]",
                parentEntityId, groupType, entityGroupIds, pageLink);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateIds(entityGroupIds, "Incorrect entityGroupIds " + entityGroupIds);
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupsByTypeOrIds(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByTypeOrIds(TenantId tenantId, EntityId parentEntityId, EntityType groupType, List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByTypeOrIds, parentEntityId [{}], groupType [{}], entityGroupIds [{}], pageLink [{}]",
                parentEntityId, groupType, entityGroupIds, pageLink);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateIds(entityGroupIds, "Incorrect entityGroupIds " + entityGroupIds);
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupEntityInfosByTypeOrIds(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEdgeEntityGroupInfosByOwnerIdType(TenantId tenantId, EdgeId edgeId, EntityId ownerId, EntityType groupType, PageLink pageLink) {
        log.trace("[{}] Executing findEdgeEntityGroupInfosByOwnerIdType, edgeId [{}], ownerId [{}], groupType [{}], pageLink [{}]", tenantId, edgeId, ownerId, groupType, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        Validator.validateEntityId(ownerId, "Incorrect ownerId " + ownerId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        return this.entityGroupInfoDao.findEdgeEntityGroupsByOwnerIdAndType(tenantId.getId(), edgeId.getId(), ownerId.getId(), ownerId.getEntityType(), relationType, pageLink);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupsByType, tenantId [{}], groupType [{}], pageLink [{}]", tenantId, groupType, pageLink);
        validateEntityId(tenantId, INCORRECT_TENANT_ID + tenantId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupDao.findEntityGroupsByType(tenantId.getId(), groupType, pageLink);
    }

    @Override
    public Optional<EntityGroup> findEntityGroupByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Executing findEntityGroupByTypeAndName, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        return this.entityGroupDao.findEntityGroupByTypeAndName(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, name);
    }

    @Override
    public Optional<EntityGroupInfo> findEntityGroupInfoByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Executing findEntityGroupInfoByTypeAndName, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        return this.entityGroupInfoDao.findEntityGroupByTypeAndName(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, name);
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndNameAsync(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name) {
        log.warn("Executing findEntityGroupByTypeAndNameAsync, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        String relationType = validateAndComposeRelationType(parentEntityId, groupType, name);
        return this.entityGroupDao.findEntityGroupByTypeAndNameAsync(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, name);
    }

    private String validateAndComposeRelationType(EntityId parentEntityId, EntityType groupType, String name) {
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateString(name, "Incorrect name " + name);
        return ENTITY_GROUP_RELATION_PREFIX + groupType.name();
    }

    @Override
    public void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(entityGroupId);
        entityRelation.setTo(entityId);
        entityRelation.setTypeGroup(RelationTypeGroup.FROM_ENTITY_GROUP);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(tenantId, entityRelation);
    }

    @Override
    public void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing addEntityToEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        for (EntityId entityId : entityIds) {
            validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
        }
        var relations = entityIds.stream().map(entityId -> {
            EntityRelation entityRelation = new EntityRelation();
            entityRelation.setFrom(entityGroupId);
            entityRelation.setTo(entityId);
            entityRelation.setTypeGroup(RelationTypeGroup.FROM_ENTITY_GROUP);
            entityRelation.setType(EntityRelation.CONTAINS_TYPE);
            return entityRelation;
        }).collect(Collectors.toList());
        relationService.saveRelations(tenantId, relations);
    }

    @Override
    public void addEntityToEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroupAll, parentEntityId [{}], entityId [{}]", parentEntityId, entityId);
        validateEntityId(parentEntityId, INCORRECT_PARENT_ENTITY_ID + parentEntityId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
        try {
            Optional<EntityGroup> entityGroup = findEntityGroupByTypeAndName(tenantId, parentEntityId, entityId.getEntityType(), EntityGroup.GROUP_ALL_NAME);
            if (entityGroup.isPresent()) {
                addEntityToEntityGroup(tenantId, entityGroup.get().getId(), entityId);
            } else {
                throw new DataValidationException("Group All of type " + entityId.getEntityType() + " is absent for entityId " + parentEntityId);
            }
        } catch (Exception e) {
            log.error("Unable to add entity to group All", e);
        }
    }

    @Override
    public void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing removeEntityFromEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);
        relationService.deleteRelation(tenantId, entityGroupId, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    @Override
    public void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing removeEntitiesFromEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        entityIds.forEach(entityId -> removeEntityFromEntityGroup(tenantId, entityGroupId, entityId));
    }

    @Override
    public ShortEntityView findGroupEntity(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing findGroupEntity, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validateEntityId(entityId, INCORRECT_ENTITY_ID + entityId);

        if (!isEntityInGroup(tenantId, entityId, entityGroupId)) {
            throw new IncorrectParameterException(String.format("Entity %s not present in entity group %s.", entityId, entityGroupId));
        }

        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        List<ColumnConfiguration> columns = getEntityGroupColumns(entityGroup);

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(entityId);

        List<ColumnConfiguration> entityFieldsColumns = new ArrayList<>();
        List<ColumnConfiguration> latestValuesColumns = new ArrayList<>();

        columns.forEach(column -> {
            if (column.getType().equals(ColumnType.ENTITY_FIELD)) {
                entityFieldsColumns.add(column);
            } else {
                latestValuesColumns.add(column);
            }
        });

        List<EntityKey> entityFields = entityFieldsColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());
        List<EntityKey> latestValues = latestValuesColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());

        EntityDataQuery dataQuery = new EntityDataQuery(singleEntityFilter, new EntityDataPageLink(), entityFields, latestValues, Collections.emptyList());
        PageData<EntityData> entityDataByQuery = entityQueryDao.findEntityDataByQuery(tenantId, customerId, userPermissions, dataQuery);

        return entityDataToShortEntityView(entityDataByQuery.getData().get(0));
    }

    @Override
    public PageData<ShortEntityView> findGroupEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findGroupEntities, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        validatePageLink(pageLink);
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        List<ColumnConfiguration> columns = getEntityGroupColumns(entityGroup);

        EntityGroupFilter entityGroupFilter = new EntityGroupFilter();
        entityGroupFilter.setEntityGroup(entityGroupId.getId().toString());
        entityGroupFilter.setGroupType(entityGroup.getType());

        List<ColumnConfiguration> entityFieldsColumns = new ArrayList<>();
        List<ColumnConfiguration> latestValuesColumns = new ArrayList<>();

        columns.forEach(column -> {
            if (column.getType().equals(ColumnType.ENTITY_FIELD)) {
                entityFieldsColumns.add(column);
            } else {
                latestValuesColumns.add(column);
            }
        });

        List<EntityKey> entityFields = entityFieldsColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());
        List<EntityKey> latestValues = latestValuesColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());

        EntityDataSortOrder sortOrder = null;
        if (pageLink.getSortOrder() != null && !StringUtils.isEmpty(pageLink.getSortOrder().getProperty())) {
            String property = pageLink.getSortOrder().getProperty();
            for (ColumnConfiguration column : columns) {
                if (column.getKey().equals(property)) {
                    sortOrder = new EntityDataSortOrder(columnToEntityKey(column), EntityDataSortOrder.Direction.valueOf(pageLink.getSortOrder().getDirection().name()));
                    break;
                }
            }
        } else {
            for (ColumnConfiguration column : columns) {
                if (column.getSortOrder() != null && !column.getSortOrder().equals(SortOrder.NONE)) {
                    sortOrder = new EntityDataSortOrder(columnToEntityKey(column), EntityDataSortOrder.Direction.valueOf(column.getSortOrder().name()));
                    break;
                }
            }
        }

        EntityDataPageLink entityDataPageLink = new EntityDataPageLink(pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder);
        EntityDataQuery dataQuery = new EntityDataQuery(entityGroupFilter, entityDataPageLink, entityFields, latestValues, Collections.emptyList());
        PageData<EntityData> entityDataByQuery = entityQueryDao.findEntityDataByQuery(tenantId, customerId, userPermissions, dataQuery);

        return new PageData<>(
                entityDataByQuery.getData().stream().map(this::entityDataToShortEntityView).collect(Collectors.toList()),
                entityDataByQuery.getTotalPages(),
                entityDataByQuery.getTotalElements(),
                entityDataByQuery.hasNext());
    }

    private ShortEntityView entityDataToShortEntityView(EntityData entityData) {
        ShortEntityView entityView = new ShortEntityView(entityData.getEntityId());
        entityData.getLatest().forEach((type, map) -> map.forEach((k, v) -> {
            String key;
            switch (type) {
                case ENTITY_FIELD:
                    key = entityDataKeyToShortEntityViewKeyMap.getOrDefault(k, k);
                    break;
                case CLIENT_ATTRIBUTE:
                    key = "client_" + k;
                    break;
                case SHARED_ATTRIBUTE:
                    key = "shared_" + k;
                    break;
                case SERVER_ATTRIBUTE:
                    key = "server_" + k;
                    break;
                default:
                    key = k;
            }
            entityView.put(key, v.getValue());
        }));
        return entityView;
    }

    private EntityKey columnToEntityKey(ColumnConfiguration column) {
        EntityKeyType entityKeyType = column.getType().getEntityKeyType();
        String key;

        if (entityKeyType.equals(EntityKeyType.ENTITY_FIELD)) {
            key = columnToEntityKeyMap.getOrDefault(column.getKey(), column.getKey());
        } else {
            key = column.getKey();
        }

        return new EntityKey(entityKeyType, key);
    }

    private final Map<String, String> entityDataKeyToShortEntityViewKeyMap = new HashMap<>();

    private final Map<String, String> columnToEntityKeyMap = new HashMap<>();

    {
        columnToEntityKeyMap.put("created_time", "createdTime");
        columnToEntityKeyMap.put("assigned_customer", "assignedCustomer");
        columnToEntityKeyMap.put("first_name", "firstName");
        columnToEntityKeyMap.put("last_name", "lastName");
        columnToEntityKeyMap.put("device_profile", "type");

        entityDataKeyToShortEntityViewKeyMap.put("createdTime", "created_time");
        entityDataKeyToShortEntityViewKeyMap.put("assignedCustomer", "assigned_customer");
        entityDataKeyToShortEntityViewKeyMap.put("firstName", "first_name");
        entityDataKeyToShortEntityViewKeyMap.put("lastName", "last_name");
        entityDataKeyToShortEntityViewKeyMap.put("type", "device_profile");
    }

    @Override
    public ListenableFuture<List<EntityId>> findAllEntityIdsAsync(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findAllEntityIdsAsync, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        return findEntityIds(tenantId, entityGroupId, entityGroup.getType(), pageLink);
    }

    @Override
    public PageData<EntityId> findEntityIds(TenantId tenantId, EntityType entityType, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findEntitiesSync, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        return entityGroupDao.findGroupEntityIdsSync(entityType, entityGroupId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<EntityGroupId>> findEntityGroupsForEntityAsync(TenantId tenantId, EntityId entityId) {
        return executorService.submit(() -> {
            var relations = relationDao.findAllByToAndType(tenantId, entityId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
            List<EntityGroupId> entityGroupIds = new ArrayList<>(relations.size());
            for (EntityRelation relation : relations) {
                entityGroupIds.add(new EntityGroupId(relation.getFrom().getId()));
            }
            return entityGroupIds;
        });
    }

    @Override
    public EntityGroup assignEntityGroupToEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign entity group to non-existent edge!");
        }
        try {
            String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
            createRelation(tenantId, new EntityRelation(edgeId, entityGroupId, relationType, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity group relation. Edge Id: [{}]", entityGroupId, edgeId);
            throw new RuntimeException(e);
        }
        return entityGroup;
    }

    @Override
    public EntityGroup unassignEntityGroupFromEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign entity group from non-existent edge!");
        }
        try {
            String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
            deleteRelation(tenantId, new EntityRelation(edgeId, entityGroupId, relationType, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete entity group relation. Edge id: [{}]", entityGroupId, edgeId);
            throw new RuntimeException(e);
        }
        return entityGroup;
    }

    @Override
    public PageData<EntityGroup> findEdgeEntityGroupsByType(TenantId tenantId, EdgeId edgeId, EntityType groupType, PageLink pageLink) {
        log.trace("[{}] Executing findEdgeEntityGroupsByType, edgeId [{}], groupType [{}], pageLink [{}]", tenantId, edgeId, groupType, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        return this.entityGroupDao.findEdgeEntityGroupsByType(tenantId.getId(), edgeId.getId(), relationType, pageLink);
    }

    @Override
    public ListenableFuture<Boolean> checkEdgeEntityGroupByIdAsync(TenantId tenantId, EdgeId edgeId, EntityGroupId entityGroupId, EntityType groupType) {
        log.trace("Executing checkEdgeEntityGroupByIdAsync, tenantId [{}], edgeId [{}], entityGroupId [{}]", tenantId, edgeId, entityGroupId);
        validateEntityId(entityGroupId, INCORRECT_ENTITY_GROUP_ID + entityGroupId);
        return relationService.checkRelationAsync(tenantId, edgeId, entityGroupId,
                EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name()
                , RelationTypeGroup.EDGE);
    }

    @Override
    public ListenableFuture<EntityGroup> findOrCreateEdgeAllGroupAsync(TenantId tenantId, Edge edge, String edgeName, EntityType groupType) {
        String entityGroupName = String.format(EntityGroup.GROUP_EDGE_ALL_NAME_PATTERN, edgeName);
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = entityGroupService
                .findEntityGroupByTypeAndNameAsync(tenantId, edge.getOwnerId(), groupType, entityGroupName);
        return Futures.transformAsync(futureEntityGroup, optionalEntityGroup -> {
            if (optionalEntityGroup != null && optionalEntityGroup.isPresent()) {
                return Futures.immediateFuture(optionalEntityGroup.get());
            } else {
                try {
                    ListenableFuture<Optional<EntityGroup>> currentEntityGroupFuture = entityGroupService
                            .findEntityGroupByTypeAndNameAsync(tenantId, edge.getOwnerId(), groupType, entityGroupName);
                    return Futures.transformAsync(currentEntityGroupFuture, currentEntityGroup -> {
                        if (currentEntityGroup.isEmpty()) {
                            EntityGroup entityGroup = createEntityGroup(entityGroupName, edge.getOwnerId(), tenantId, groupType);
                            entityGroupService.assignEntityGroupToEdge(tenantId, entityGroup.getId(),
                                    edge.getId(), groupType);
                            return Futures.immediateFuture(entityGroup);
                        } else {
                            return Futures.immediateFuture(currentEntityGroup.get());
                        }
                    }, MoreExecutors.directExecutor());
                } catch (Exception e) {
                    log.error("[{}] Can't get entity group by name edge owner id [{}], groupType [{}], entityGroupName [{}]",
                            tenantId, edge.getOwnerId(), groupType, entityGroupName, e);
                    throw new RuntimeException(e);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, TenantId tenantId, EntityType groupType) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(groupType);
        return entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    @Override
    public boolean isEntityInGroup(TenantId tenantId, EntityId entityId, EntityGroupId entityGroupId) {
        return relationService.checkRelation(tenantId, entityGroupId, entityId,
                EntityRelation.CONTAINS_TYPE
                , RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    private ListenableFuture<List<EntityId>> findEntityIds(TenantId tenantId,
                                                           EntityGroupId entityGroupId, EntityType groupType, PageLink pageLink) {
        ListenableFuture<PageData<EntityId>> pageData = entityGroupDao.findGroupEntityIds(groupType, entityGroupId.getId(), pageLink);
        return Futures.transform(pageData, PageData::getData, MoreExecutors.directExecutor());
    }

    private List<ColumnConfiguration> getEntityGroupColumns(EntityGroup entityGroup) {
        JsonNode jsonConfiguration = entityGroup.getConfiguration();
        List<ColumnConfiguration> columns = null;
        if (jsonConfiguration != null) {
            try {
                EntityGroupConfiguration entityGroupConfiguration =
                        JacksonUtil.treeToValue(jsonConfiguration, EntityGroupConfiguration.class);
                columns = entityGroupConfiguration.getColumns();
            } catch (IllegalArgumentException e) {
                log.error("Unable to read entity group configuration", e);
                throw new RuntimeException("Unable to read entity group configuration", e);
            }
        }
        if (columns == null) {
            columns = Collections.emptyList();
        }
        return columns;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEntityGroupById(tenantId, new EntityGroupId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

    private class EntityGroupValidator extends DataValidator<EntityGroup> {

        @Override
        protected void validateCreate(TenantId tenantId, EntityGroup entityGroup) {
            if (entityGroup.getExternalId() != null) {
                EntityGroup other = entityGroupDao.findByTenantIdAndExternalId(tenantId.getId(), entityGroup.getExternalId().getId());
                if (other != null) {
                    throw new DataValidationException("Entity group with such external id already exists!");
                }
            }
        }

        @Override
        protected void validateDataImpl(TenantId tenantId, EntityGroup entityGroup) {
            if (entityGroup.getType() == null) {
                throw new DataValidationException("Entity group type should be specified!");
            }
            if (StringUtils.isEmpty(entityGroup.getName())) {
                throw new DataValidationException("Entity group name should be specified!");
            }
            if (entityGroup.getOwnerId() == null || entityGroup.getOwnerId().isNullUid()) {
                throw new DataValidationException("Entity group ownerId should be specified!");
            }
        }
    }

    private PaginatedRemover<EntityId, EntityGroup> entityGroupsRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<EntityGroup> findEntities(TenantId tenantId, EntityId id, PageLink pageLink) {
                    return entityGroupDao.findAllEntityGroups(tenantId.getId(), id.getId(), id.getEntityType(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, EntityGroup entity) {
                    deleteEntityGroup(tenantId, new EntityGroupId(entity.getId().getId()));
                }
            };

}
