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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.edge.rpc.EdgeEventUtils;

import java.util.ArrayList;
import java.util.List;


@AllArgsConstructor
@Slf4j
public class WhiteLabelingEdgeEventFetcher extends BasePageableEdgeEventFetcher {

    private final WhiteLabelingService whiteLabelingService;
    private final CustomTranslationService customTranslationService;

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        List<EdgeEvent> result = new ArrayList<>();
        EdgeEvent loginWhiteLabelingEdgeEvent = getLoginWhiteLabelingEdgeEvent(tenantId, edge);
        if (loginWhiteLabelingEdgeEvent != null) {
            result.add(loginWhiteLabelingEdgeEvent);
        }
        EdgeEvent whiteLabelingEdgeEvent = getWhiteLabelingEdgeEvent(tenantId, edge);
        if (whiteLabelingEdgeEvent != null) {
            result.add(whiteLabelingEdgeEvent);
        }
        EdgeEvent customTranslationEdgeEvent = getCustomTranslationEdgeEvent(tenantId, edge);
        if (customTranslationEdgeEvent != null) {
            result.add(customTranslationEdgeEvent);
        }
        // @voba - returns PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

    private EdgeEvent getLoginWhiteLabelingEdgeEvent(TenantId tenantId, Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            String domainName = "localhost";
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                domainName = whiteLabelingService.getTenantLoginWhiteLabelingParams(tenantId).getDomainName();
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                domainName = whiteLabelingService.getCustomerLoginWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId())).getDomainName();
            }
            LoginWhiteLabelingParams loginWhiteLabelingParams = whiteLabelingService
                    .getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, domainName == null ? "localhost" : domainName, null, null);
            if (loginWhiteLabelingParams != null) {
                return EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                        EdgeEventType.LOGIN_WHITE_LABELING, EdgeEventActionType.UPDATED, null, mapper.valueToTree(loginWhiteLabelingParams));
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Can't load login white labeling params", e);
            throw new RuntimeException(e);
        }
    }

    private EdgeEvent getWhiteLabelingEdgeEvent(TenantId tenantId, Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            WhiteLabelingParams whiteLabelingParams = null;
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(tenantId, null, null);
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId()), null, null);
            }
            if (whiteLabelingParams != null) {
                return EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                        EdgeEventType.WHITE_LABELING, EdgeEventActionType.UPDATED, null, mapper.valueToTree(whiteLabelingParams));
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
            throw new RuntimeException(e);
        }
    }

    private EdgeEvent getCustomTranslationEdgeEvent(TenantId tenantId, Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            CustomTranslation customTranslation = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedTenantCustomTranslation(new TenantId(ownerId.getId()));
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedCustomerCustomTranslation(edge.getTenantId(), new CustomerId(ownerId.getId()));
            }

            if (customTranslation != null) {
                return EdgeEventUtils.constructEdgeEvent(tenantId, edge.getId(),
                        EdgeEventType.CUSTOM_TRANSLATION, EdgeEventActionType.UPDATED, null, mapper.valueToTree(customTranslation));
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("Can't load custom translation", e);
            throw new RuntimeException(e);
        }
    }

}




