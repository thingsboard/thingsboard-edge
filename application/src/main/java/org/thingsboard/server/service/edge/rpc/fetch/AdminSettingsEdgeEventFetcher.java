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
package org.thingsboard.server.service.edge.rpc.fetch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@AllArgsConstructor
@Slf4j
public class AdminSettingsEdgeEventFetcher implements EdgeEventFetcher {

    private final AdminSettingsService adminSettingsService;
    private final AttributesService attributesService;
    private final CustomerService customerService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        List<EdgeEvent> result = fetchAdminSettingsForKeys(tenantId, edge, List.of("general", "mail", "connectivity", "jwt", "customTranslation", "customMenu"));

        // return PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

    private List<EdgeEvent> fetchAdminSettingsForKeys(TenantId tenantId, Edge edge, List<String> keys) throws Exception {
        List<EdgeEvent> result = new ArrayList<>();
        for (String key : keys) {
            AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key);
            if (adminSettings != null) {
                result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                        EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(key)));
            }
            Optional<AttributeKvEntry> tenantSettingsAttr = attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, key).get();
            if (tenantSettingsAttr.isPresent()) {
                ObjectNode tenantNode = JacksonUtil.newObjectNode()
                        .put("tenantId", tenantId.getId().toString())
                        .put("key", key);
                result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                        EdgeEventActionType.UPDATED, null, tenantNode));
            }
            if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
                getCustomerAdminSettingsEdgeEvents(tenantId, edge.getId(), edge.getOwnerId(), key, result);
            }
        }
        return result;
    }

    private void getCustomerAdminSettingsEdgeEvents(TenantId tenantId, EdgeId edgeId, EntityId customerId, String key, List<EdgeEvent> result) throws Exception {
        Optional<AttributeKvEntry> customerSettingsAttr = attributesService.find(tenantId, customerId, DataConstants.SERVER_SCOPE, key).get();
        if (customerSettingsAttr.isPresent()) {
            ObjectNode customerNode = JacksonUtil.newObjectNode()
                    .put("customerId", customerId.getId().toString())
                    .put("key", key);
            result.add(EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.ADMIN_SETTINGS,
                    EdgeEventActionType.UPDATED, null, customerNode));
        }
        Customer customer = customerService.findCustomerById(tenantId, (CustomerId) customerId);
        if (customer.isSubCustomer()) {
            getCustomerAdminSettingsEdgeEvents(tenantId, edgeId, customer.getParentCustomerId(), key, result);
        }
    }
}
