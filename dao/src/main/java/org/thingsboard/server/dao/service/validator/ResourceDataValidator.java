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
package org.thingsboard.server.dao.service.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.EntityType.TB_RESOURCE;

@Component
public class ResourceDataValidator extends DataValidator<TbResource> {

    @Autowired
    private TbResourceDao resourceDao;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, TbResource resource) {
        if (resource.getData() == null || resource.getData().length == 0) {
            throw new DataValidationException("Resource data should be specified");
        }
    }

    @Override
    protected TbResource validateUpdate(TenantId tenantId, TbResource resource) {
        if (resource.getData() != null && !resource.getResourceType().isUpdatable() &&
                tenantId != null && !tenantId.isSysTenantId()) {
            throw new DataValidationException("This type of resource can't be updated");
        }
        return resource;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, TbResource resource) {
        validateString("Resource title", resource.getTitle());
        if (resource.getTenantId() == null) {
            resource.setTenantId(TenantId.SYS_TENANT_ID);
        }
        if (!resource.getTenantId().isSysTenantId()) {
            if (!tenantService.tenantExists(resource.getTenantId())) {
                throw new DataValidationException("Resource is referencing to non-existent tenant!");
            }
        }
        if (resource.getResourceType() == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (!resource.getTenantId().isSysTenantId() && resource.getData() != null) {
            DefaultTenantProfileConfiguration profileConfiguration = tenantProfileCache.get(tenantId).getDefaultProfileConfiguration();
            long maxResourceSize = profileConfiguration.getMaxResourceSize();
            if (maxResourceSize > 0 && resource.getData().length > maxResourceSize) {
                throw new IllegalArgumentException("Resource exceeds the maximum size of " + maxResourceSize + " bytes");
            }
            long maxSumResourcesDataInBytes = profileConfiguration.getMaxResourcesInBytes();
            int dataSize = resource.getData().length;
            if (resource.getId() != null) {
                long prevSize = resourceDao.getResourceSize(tenantId, resource.getId());
                dataSize -= prevSize;
            }
            validateMaxSumDataSizePerTenant(tenantId, resourceDao, maxSumResourcesDataInBytes, dataSize, TB_RESOURCE);
        }
        if (StringUtils.isEmpty(resource.getFileName())) {
            throw new DataValidationException("Resource file name should be specified!");
        }
        if (StringUtils.containsAny(resource.getFileName(), "/", "\\")) {
            throw new DataValidationException("File name contains forbidden symbols");
        }
        if (StringUtils.isEmpty(resource.getResourceKey())) {
            throw new DataValidationException("Resource key should be specified!");
        }
    }

    @Override
    public void validateDelete(TenantId tenantId, EntityId resourceId) {
        List<WidgetTypeDetails> widgets = widgetTypeDao.findWidgetTypesInfosByTenantIdAndResourceId(tenantId.getId(),
                resourceId.getId());
        if (!widgets.isEmpty()) {
            List<String> widgetNames = widgets.stream().map(BaseWidgetType::getName).collect(Collectors.toList());
            throw new DataValidationException(String.format("Following widget types uses current resource: %s", widgetNames));
        }
    }

}
