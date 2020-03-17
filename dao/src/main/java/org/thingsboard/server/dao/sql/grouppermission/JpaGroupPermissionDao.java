/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specifications.where;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

@Component
public class JpaGroupPermissionDao extends JpaAbstractSearchTimeDao<GroupPermissionEntity, GroupPermission> implements GroupPermissionDao {

    @Autowired
    private GroupPermissionRepository groupPermissionRepository;

    @Override
    protected Class<GroupPermissionEntity> getEntityClass() {
        return GroupPermissionEntity.class;
    }

    @Override
    protected CrudRepository<GroupPermissionEntity, String> getCrudRepository() {
        return groupPermissionRepository;
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantId(UUID tenantId, TimePageLink pageLink) {
        return findGroupPermissions(tenantId, null, null, null, pageLink);
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId, TimePageLink pageLink) {
        return findGroupPermissions(tenantId, userGroupId, null, null, pageLink);
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupIdAndRoleId(UUID tenantId, UUID userGroupId, UUID roleId, TimePageLink pageLink) {
        return findGroupPermissions(tenantId, userGroupId, null, roleId, pageLink);
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(UUID tenantId, UUID entityGroupId, UUID userGroupId, UUID roleId, TimePageLink pageLink) {
        return findGroupPermissions(tenantId, userGroupId, entityGroupId, roleId, pageLink);
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, TimePageLink pageLink) {
        return findGroupPermissions(tenantId, null, entityGroupId, null, pageLink);
    }

    @Override
    public List<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(UUID tenantId, UUID roleId, TimePageLink pageLink) {
        return findGroupPermissions(tenantId, null, null, roleId, pageLink);
    }

    private List<GroupPermission> findGroupPermissions(UUID tenantId, UUID userGroupId, UUID entityGroupId, UUID roleId, TimePageLink pageLink) {
        Specification<GroupPermissionEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<GroupPermissionEntity> fieldsSpec = getEntityFieldsSpec(tenantId, userGroupId, entityGroupId, roleId);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, ID_PROPERTY);
        return DaoUtil.convertDataList(groupPermissionRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent());
    }

    private Specification<GroupPermissionEntity> getEntityFieldsSpec(UUID tenantId, UUID userGroupId, UUID entityGroupId, UUID roleId) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(tenantId));
                predicates.add(tenantIdPredicate);
            }
            if (userGroupId != null) {
                Predicate userGroupIdPredicate = criteriaBuilder.equal(root.get("userGroupId"), UUIDConverter.fromTimeUUID(userGroupId));
                predicates.add(userGroupIdPredicate);
            }
            if (entityGroupId != null) {
                Predicate entityGroupIdPredicate = criteriaBuilder.equal(root.get("entityGroupId"), UUIDConverter.fromTimeUUID(entityGroupId));
                predicates.add(entityGroupIdPredicate);
            }
            if (roleId != null) {
                Predicate roleIdPredicate = criteriaBuilder.equal(root.get("roleId"), UUIDConverter.fromTimeUUID(roleId));
                predicates.add(roleIdPredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
        };
    }

}
