/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.entitiy.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.AccessValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbIntegrationService extends AbstractTbEntityService implements TbIntegrationService {

    private static final String INTEGRATION_STATUS_KEY_PREFIX = "integration_status_";
    private static final String MONOLITH = "monolith";
    private static final int DAY_IN_MS = 24 * 60 * 60 * 1000;

    @Value("${integrations.statistics.enabled}")
    private boolean statisticsEnabled;

    private final IntegrationService integrationService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final AttributesService attributesService;
    private final PartitionService partitionService;
    private final DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public void findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate, DeferredResult<ResponseEntity> response) {
        PageData<IntegrationInfo> pageData = integrationService.findTenantIntegrationInfos(tenantId, pageLink, isEdgeTemplate);
        List<IntegrationInfo> integrationInfos = pageData.getData();

        if (isEdgeTemplate) {
            integrationInfos.forEach(integration -> {
                ObjectNode status = JacksonUtil.newObjectNode();
                status.put("success", true);
                integration.setStatus(status);
                integration.setStats(JacksonUtil.OBJECT_MAPPER.createArrayNode());
            });
            response.setResult(new ResponseEntity<>(pageData, HttpStatus.OK));
            return;
        }

        List<ListenableFuture<Void>> futures = new ArrayList<>(integrationInfos.size());

        for (IntegrationInfo integrationInfo : integrationInfos) {
            if (integrationInfo.isEnabled()) {
                if (integrationInfo.isRemote()) {
                    futures.add(setRemoteIntegrationStatus(integrationInfo));
                } else {
                    futures.add(setIntegrationStatus(integrationInfo));
                }
            }

            if (statisticsEnabled) {
                long startTs = System.currentTimeMillis() - DAY_IN_MS;
                ArrayNode stats = integrationService.findIntegrationStats(integrationInfo.getTenantId(), integrationInfo.getId(), startTs);
                integrationInfo.setStats(stats);
            } else {
                integrationInfo.setStats(JacksonUtil.OBJECT_MAPPER.createArrayNode());
            }
        }

        DonAsynchron.withCallback(Futures.allAsList(futures),
                v -> response.setResult(new ResponseEntity<>(pageData, HttpStatus.OK)),
                error -> {
                    log.error("Failed to set integration status!", error);
                    AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                },
                dbCallbackExecutorService);
    }

    private ListenableFuture<Void> setRemoteIntegrationStatus(IntegrationInfo integration) {
        List<String> attrKeys =
                attributesService.findAllKeysByEntityIds(integration.getTenantId(), EntityType.INTEGRATION, Collections.singletonList(integration.getId()));
        attrKeys = attrKeys.stream().filter(k -> k.startsWith(INTEGRATION_STATUS_KEY_PREFIX)).collect(Collectors.toList());

        return doSetStatus(integration, attrKeys);
    }

    private ListenableFuture<Void> setIntegrationStatus(IntegrationInfo integration) {
        String serviceType = serviceInfoProvider.getServiceType();
        Set<String> serviceIds;
        if (serviceType.equals(MONOLITH)) {
            serviceIds = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        } else {
            serviceIds = partitionService.getAllServiceIds(ServiceType.TB_INTEGRATION_EXECUTOR);
        }

        List<String> attrKeys = serviceIds.stream().map(this::constructAttributeKey).collect(Collectors.toList());

        return doSetStatus(integration, attrKeys);
    }

    private ListenableFuture<Void> doSetStatus(IntegrationInfo integration, List<String> keys) {
        ListenableFuture<List<AttributeKvEntry>> attributesFuture =
                attributesService.find(integration.getTenantId(), integration.getId(), "SERVER_SCOPE", keys);

        return Futures.transform(attributesFuture, attributes -> {
            Optional<AttributeKvEntry> error = attributes.stream().filter(kv -> {
                ObjectNode json = JacksonUtil.fromString(kv.getJsonValue().get(), ObjectNode.class);
                return json.has("error");
            }).max(Comparator.comparingLong(AttributeKvEntry::getLastUpdateTs));

            ObjectNode status;

            if (error.isPresent()) {
                AttributeKvEntry kv = error.get();
                status = JacksonUtil.fromString(kv.getJsonValue().get(), ObjectNode.class);
            } else {
                status = JacksonUtil.newObjectNode();
                status.put("success", true);
            }

            integration.setStatus(status);
            return null;
        }, MoreExecutors.directExecutor());
    }

    private String constructAttributeKey(String serviceId) {
        return INTEGRATION_STATUS_KEY_PREFIX + serviceId.toLowerCase();
    }
}
