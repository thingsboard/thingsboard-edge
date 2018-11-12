/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.role;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.role.RoleSearchQuery;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ROLE_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class RoleServiceImpl extends AbstractEntityService implements RoleService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private TenantDao tenantDao;

    @CacheEvict(cacheNames = ROLE_CACHE, key = "{#role.id}")
    @Override
    public Role saveRole(TenantId tenantId, Role role) {
        log.trace("Executing save role [{}]", role);
        roleValidator.validate(role, Role::getTenantId);
        Role savedRole = roleDao.save(tenantId, role);
        return savedRole;
    }

    @Cacheable(cacheNames = ROLE_CACHE, key = "{#roleId}")
    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleById [{}]", roleId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        return roleDao.findById(tenantId, roleId.getId());
    }

    @Override
    public TextPageData<Role> findRoleByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findRoleByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Role> roles = roleDao.findRolesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(roles, pageLink);
    }

    @Override
    public TextPageData<Role> findRoleByTenantIdAndType(TenantId tenantId, TextPageLink pageLink, String type) {
        log.trace("Executing findRoleByTenantIdAndType, tenantId [{}], pageLink [{}], type [{}]", tenantId, pageLink, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        validateString(type, "Incorrect type " + type);
        List<Role> roles = roleDao.findRolesByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(roles, pageLink);
    }

    @Override
    public ListenableFuture<List<Role>> findRolesByQuery(TenantId tenantId, RoleSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Role>> roles = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Role>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ROLE) {
                    futures.add(findRoleByIdAsync(tenantId, new RoleId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });

        roles = Futures.transform(roles, new Function<List<Role>, List<Role>>() {
            @Nullable
            @Override
            public List<Role> apply(@Nullable List<Role> roleList) {
                return roleList == null ? Collections.emptyList() : roleList.stream().filter(role -> query.getRoleTypes().contains(role.getType())).collect(Collectors.toList());
            }
        });

        return roles;
    }

    @Override
    public ListenableFuture<Role> findRoleByIdAsync(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findRoleByIdAsync [{}]", roleId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        return roleDao.findByIdAsync(tenantId, roleId.getId());
    }

    @CacheEvict(cacheNames = ROLE_CACHE, key = "{#roleId}")
    @Override
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteRole [{}]", roleId);
        validateId(roleId, INCORRECT_ROLE_ID + roleId);
        deleteEntityRelations(tenantId, roleId);
        roleDao.removeById(tenantId, roleId.getId());
    }

    @Override
    public void deleteRolesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteRolesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantRoleRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findRoleTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findRoleTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantRoleTypes = roleDao.findTenantRoleTypesAsync(tenantId.getId());
        return Futures.transform(tenantRoleTypes,
                roleTypes -> {
                    roleTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return roleTypes;
                });
    }

    private DataValidator<Role> roleValidator =
            new DataValidator<Role>() {

                @Override
                protected void validateCreate(TenantId tenantId, Role role) {
                    roleDao.findRoleByTenantIdAndName(role.getTenantId().getId(), role.getName())
                            .ifPresent(e -> {
                                throw new DataValidationException("Role with such name already exists!");
                            });
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Role role) {
                    roleDao.findRoleByTenantIdAndName(role.getTenantId().getId(), role.getName())
                            .ifPresent(e -> {
                                if (!e.getUuidId().equals(role.getUuidId())) {
                                    throw new DataValidationException("Role with such name already exists!");
                                }
                            });
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Role role) {
                    if (StringUtils.isEmpty(role.getType())) {
                        throw new DataValidationException("Role type should be specified!");
                    }
                    if (StringUtils.isEmpty(role.getName())) {
                        throw new DataValidationException("Role name should be specified!");
                    }
                    if (role.getTenantId() == null) {
                        throw new DataValidationException("Role should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, role.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Role is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Role> tenantRoleRemover =
            new PaginatedRemover<TenantId, Role>() {

                @Override
                protected List<Role> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return roleDao.findRolesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Role entity) {
                    deleteRole(tenantId, new RoleId(entity.getUuidId()));
                }
            };
}
