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
package org.thingsboard.server.dao.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.List;

import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseResourceService implements ResourceService {

    private final ResourceDao resourceDao;

    public BaseResourceService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    @Override
    public Resource saveResource(Resource resource) {
        log.trace("Executing saveResource [{}]", resource);
        validate(resource);
        return resourceDao.saveResource(resource);
    }

    @Override
    public Resource getResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        log.trace("Executing getResource [{}] [{}] [{}]", tenantId, resourceType, resourceId);
        validate(tenantId, resourceType, resourceId);
        return resourceDao.getResource(tenantId, resourceType, resourceId);
    }

    @Override
    public void deleteResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        log.trace("Executing deleteResource [{}] [{}] [{}]", tenantId, resourceType, resourceId);
        validate(tenantId, resourceType, resourceId);
        resourceDao.deleteResource(tenantId, resourceType, resourceId);
    }

    @Override
    public PageData<Resource> findResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantId(tenantId, pageLink);
    }


    @Override
    public List<Resource> findResourcesByTenantIdResourceType(TenantId tenantId, ResourceType resourceType) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantIdResourceType(tenantId, resourceType);
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        resourceDao.removeAllByTenantId(tenantId);
    }

    protected void validate(Resource resource) {
        if (resource == null) {
            throw new DataValidationException("Resource should be specified!");
        }

        if (resource.getValue() == null) {
            throw new DataValidationException("Resource value should be specified!");
        }
        validate(resource.getTenantId(), resource.getResourceType(), resource.getResourceId());
    }

    protected void validate(TenantId tenantId, ResourceType resourceType, String resourceId) {
        if (resourceType == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (resourceId == null) {
            throw new DataValidationException("Resource id should be specified!");
        }
        validateId(tenantId, "Incorrect tenantId ");
    }

}
