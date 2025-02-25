/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultEdgeSettingsService implements EdgeSettingsService {

    private final AttributesService attributesService;
    private final TenantService tenantService;

    @Override
    public EdgeSettings findEdgeSettings() {
        try {
            List<Tenant> tenants = tenantService.findTenants(new PageLink(1)).getData();
            if (tenants.isEmpty()) {
                log.error("Tenant not found. Returning empty EdgeSettings!");
                return null;
            }
            TenantId tenantId = tenants.get(0).getId();
            Optional<AttributeKvEntry> attr =
                    attributesService.find(tenantId, tenantId, AttributeScope.SERVER_SCOPE, DataConstants.EDGE_SETTINGS_ATTR_KEY).get();

            if (attr.isPresent()) {
                log.trace("Found current edge settings {}", attr.get().getValueAsString());
                return JacksonUtil.fromString(attr.get().getValueAsString(), EdgeSettings.class);
            } else {
                log.trace("Edge settings not found");
                return null;
            }
        } catch (Exception e) {
            log.error("Fetching edge settings failed", e);
            throw new RuntimeException("Fetching edge settings failed", e);
        }
    }

    @Override
    public ListenableFuture<List<Long>> saveEdgeSettings(TenantId tenantId, EdgeSettings edgeSettings) {
        StringDataEntry dataEntry = new StringDataEntry(DataConstants.EDGE_SETTINGS_ATTR_KEY, JacksonUtil.toString(edgeSettings));
        BaseAttributeKvEntry edgeSettingAttr = new BaseAttributeKvEntry(dataEntry, System.currentTimeMillis());
        List<AttributeKvEntry> attributes = Collections.singletonList(edgeSettingAttr);

        return attributesService.save(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attributes);
    }

}
