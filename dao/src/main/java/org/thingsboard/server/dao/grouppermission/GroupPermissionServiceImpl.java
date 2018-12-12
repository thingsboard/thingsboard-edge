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
package org.thingsboard.server.dao.grouppermission;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class GroupPermissionServiceImpl extends AbstractEntityService implements GroupPermissionService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_USER_GROUP_ID = "Incorrect userGroupId ";
    public static final String INCORRECT_GROUP_PERMISSION_ID = "Incorrect groupPermissionId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    private GroupPermissionDao groupPermissionDao;

    @Autowired
    private TenantDao tenantDao;

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
    public TimePageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, TimePageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), pageLink);
        return new TimePageData<>(groupPermissions, pageLink);
    }

    @Override
    public List<GroupPermission> findGroupPermissionListByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing findGroupPermissionListByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(userGroupId, INCORRECT_USER_GROUP_ID + userGroupId);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), new TimePageLink(Integer.MAX_VALUE));
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
        deleteEntityRelations(tenantId, groupPermissionId);
        groupPermissionDao.removeById(tenantId, groupPermissionId.getId());
    }

    @Override
    public void deleteGroupPermissionsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteGroupPermissionsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantGroupPermissionRemover.removeEntities(tenantId);
    }

    private DataValidator<GroupPermission> groupPermissionValidator =
            new DataValidator<GroupPermission>() {

                @Override
                protected void validateCreate(TenantId tenantId, GroupPermission groupPermission) {
                }

                @Override
                protected void validateUpdate(TenantId tenantId, GroupPermission groupPermission) {
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, GroupPermission groupPermission) {
                    if (StringUtils.isEmpty(groupPermission.getName())) {
                        throw new DataValidationException("Group Permission name should be specified!");
                    }
                    if (groupPermission.getTenantId() == null) {
                        throw new DataValidationException("Group Permission should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, groupPermission.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Group Permission is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover tenantGroupPermissionRemover = new PaginatedRemover();

    private class PaginatedRemover {
        public void removeEntities(TenantId tenantId) {
            TimePageLink pageLink = new TimePageLink(100);
            boolean hasNext = true;
            while (hasNext) {
                List<GroupPermission> entities = findEntities(tenantId, pageLink);
                for (GroupPermission entity : entities) {
                    removeEntity(tenantId, entity);
                }
                hasNext = entities.size() == pageLink.getLimit();
                if (hasNext) {
                    int index = entities.size() - 1;
                    UUID idOffset = entities.get(index).getUuidId();
                    pageLink.setIdOffset(idOffset);
                }
            }
        }

        protected List<GroupPermission> findEntities(TenantId tenantId, TimePageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantId(tenantId.getId(), pageLink);
        }

        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, new GroupPermissionId(entity.getUuidId()));
        }
    }

    ;


}
