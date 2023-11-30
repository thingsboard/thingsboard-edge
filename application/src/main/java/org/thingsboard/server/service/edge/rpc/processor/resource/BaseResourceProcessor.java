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
package org.thingsboard.server.service.edge.rpc.processor.resource;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseResourceProcessor extends BaseEdgeProcessor {

    protected boolean saveOrUpdateTbResource(TenantId tenantId, TbResourceId tbResourceId, ResourceUpdateMsg resourceUpdateMsg) {
        boolean resourceKeyUpdated = false;
        try {
            boolean created = false;
            TbResource resource = resourceService.findResourceById(tenantId, tbResourceId);
            if (resource == null) {
                resource = new TbResource();
                if (resourceUpdateMsg.getIsSystem()) {
                    resource.setTenantId(TenantId.SYS_TENANT_ID);
                } else {
                    resource.setTenantId(tenantId);
                }
                resource.setCreatedTime(Uuids.unixTimestamp(tbResourceId.getId()));
                created = true;
            }
            String resourceKey = resourceUpdateMsg.getResourceKey();
            ResourceType resourceType = ResourceType.valueOf(resourceUpdateMsg.getResourceType());
            PageDataIterable<TbResource> resourcesIterable = new PageDataIterable<>(
                    link -> resourceService.findTenantResourcesByResourceTypeAndPageLink(tenantId, resourceType, link), 1024);
            for (TbResource tbResource : resourcesIterable) {
                if (tbResource.getResourceKey().equals(resourceUpdateMsg.getResourceKey()) && !tbResourceId.equals(tbResource.getId())) {
                    resourceKey = StringUtils.randomAlphabetic(15) + "_" + resourceKey;
                    log.warn("[{}] Resource with resource type {} and key {} already exists. Renaming resource key to {}",
                            tenantId, resourceType, resourceUpdateMsg.getResourceKey(), resourceKey);
                    resourceKeyUpdated = true;
                }
            }
            resource.setTitle(resourceUpdateMsg.getTitle());
            resource.setResourceKey(resourceKey);
            resource.setResourceType(resourceType);
            resource.setFileName(resourceUpdateMsg.getFileName());
            resource.setData(resourceUpdateMsg.hasData() ? resourceUpdateMsg.getData() : null);
            resource.setEtag(resourceUpdateMsg.hasEtag() ? resourceUpdateMsg.getEtag() : null);
            resourceValidator.validate(resource, TbResourceInfo::getTenantId);
            if (created) {
                resource.setId(tbResourceId);
            }
            resourceService.saveResource(resource, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process resource update msg [{}]", tenantId, resourceUpdateMsg, e);
            throw e;
        }
        return resourceKeyUpdated;
    }
}
