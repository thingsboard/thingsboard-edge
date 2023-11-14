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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class IntegrationCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ConverterService converterService;

    public ListenableFuture<Void> processIntegrationMsgFromCloud(TenantId tenantId, IntegrationUpdateMsg integrationUpdateMsg) {
        try {
            IntegrationId integrationId = new IntegrationId(new UUID(integrationUpdateMsg.getIdMSB(), integrationUpdateMsg.getIdLSB()));
            switch (integrationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Integration integration = JacksonUtil.fromStringIgnoreUnknownProperties(integrationUpdateMsg.getEntity(), Integration.class);
                    if (integration == null) {
                        throw new RuntimeException("[{" + tenantId + "}] integrationUpdateMsg {" + integrationUpdateMsg + "} cannot be converted to integration");
                    }
                    Integration integrationById = integrationService.findIntegrationById(tenantId, integrationId);
                    boolean created = false;
                    if (integrationById == null) {
                        created = true;
                        integration.setId(null);
                    }
                    integration.setEdgeTemplate(false);

                    integrationValidator.validate(integration, Integration::getTenantId);

                    if (created) {
                        integration.setId(integrationId);
                    }

                    Integration savedIntegration = integrationService.saveIntegration(integration, false);

                    tbClusterService.broadcastEntityStateChangeEvent(savedIntegration.getTenantId(), savedIntegration.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

                    cleanUpUnusedConverters(tenantId);

                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Integration integrationToDelete = integrationService.findIntegrationById(tenantId, integrationId);
                    if (integrationToDelete != null) {
                        integrationService.deleteIntegration(tenantId, integrationId);
                        tbClusterService.broadcastEntityStateChangeEvent(integrationToDelete.getTenantId(), integrationToDelete.getId(), ComponentLifecycleEvent.DELETED);
                        cleanUpUnusedConverters(tenantId);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(integrationUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process integration msg [%s]", integrationUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    private void cleanUpUnusedConverters(TenantId tenantId) {
        List<Converter> tenantConverters = new ArrayList<>();
        PageData<Converter> pageData;
        PageLink pageLink = new PageLink(100);
        do {
            pageData = converterService.findTenantConverters(tenantId, pageLink);
            tenantConverters.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        for (Converter tenantConverter : tenantConverters) {
            cleanUpUnusedConverters(tenantId, tenantConverter.getId());
        }
    }

    private void cleanUpUnusedConverters(TenantId tenantId, ConverterId converterId) {
        List<Integration> integrationsByConverterId = integrationService.findIntegrationsByConverterId(tenantId, converterId);
        if (integrationsByConverterId.isEmpty()) {
            converterService.deleteConverter(tenantId, converterId);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, converterId, ComponentLifecycleEvent.DELETED);
        }
    }
}
