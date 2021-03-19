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
package org.thingsboard.server.dao.sql.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ResourceCompositeKey;
import org.thingsboard.server.dao.model.sql.ResourceEntity;
import org.thingsboard.server.dao.resource.ResourceDao;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ResourceDaoImpl implements ResourceDao {

    private final ResourceRepository resourceRepository;

    public ResourceDaoImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional
    public Resource saveResource(Resource resource) {
        return DaoUtil.getData(resourceRepository.save(new ResourceEntity(resource)));
    }

    @Override
    public Resource getResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceCompositeKey key = new ResourceCompositeKey();
        key.setTenantId(tenantId.getId());
        key.setResourceType(resourceType.name());
        key.setResourceId(resourceId);

        return DaoUtil.getData(resourceRepository.findById(key));
    }

    @Override
    @Transactional
    public void deleteResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        ResourceCompositeKey key = new ResourceCompositeKey();
        key.setTenantId(tenantId.getId());
        key.setResourceType(resourceType.name());
        key.setResourceId(resourceId);

        resourceRepository.deleteById(key);
    }

    @Override
    public PageData<Resource> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<Resource> findAllByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType) {
        return DaoUtil.convertDataList(resourceRepository.findAllByTenantIdAndResourceType(tenantId.getId(), resourceType.name()));
    }

    @Override
    public PageData<Resource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                     ResourceType resourceType,
                                                                     PageLink pageLink) {
        return DaoUtil.toPageData(resourceRepository.findResourcesPage(
                tenantId.getId(),
                TenantId.SYS_TENANT_ID.getId(),
                resourceType.name(),
                Objects.toString(pageLink.getTextSearch(), ""),
                DaoUtil.toPageable(pageLink)
        ));
    }

    @Override
    public List<Resource> findResourcesByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType,
                                                                 String[] objectIds,
                                                                 String searchText) {
        return objectIds == null ?
                DaoUtil.convertDataList(resourceRepository.findResources(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(),
                        Objects.toString(searchText, ""))) :
                DaoUtil.convertDataList(resourceRepository.findResourcesByIds(
                        tenantId.getId(),
                        TenantId.SYS_TENANT_ID.getId(),
                        resourceType.name(), objectIds));
    }

    @Override
    public void removeAllByTenantId(TenantId tenantId) {
        resourceRepository.removeAllByTenantId(tenantId.getId());
    }
}
