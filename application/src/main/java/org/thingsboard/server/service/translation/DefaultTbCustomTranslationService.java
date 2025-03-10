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
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractEtagCacheService;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbCustomTranslationService extends AbstractEtagCacheService<TranslationCacheKey> implements TbCustomTranslationService {

    private final CustomTranslationService customTranslationService;
    private final TbClusterService clusterService;

    public DefaultTbCustomTranslationService(TbClusterService clusterService, CustomTranslationService customTranslationService,
                                             @Value("${cache.translation.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                             @Value("${cache.translation.etag.maxSize:1000000}") int cacheMaxSize) {
        super(cacheTtl, cacheMaxSize);
        this.clusterService = clusterService;
        this.customTranslationService = customTranslationService;
    }

    @Override
    public void saveCustomTranslation(CustomTranslation customTranslation) {
        customTranslationService.saveCustomTranslation(customTranslation);
        evictFromCache(customTranslation.getTenantId());
    }

    @Override
    public void patchCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode customTranslation) {
        customTranslationService.patchCustomTranslation(tenantId, customerId, localeCode, customTranslation);
        evictFromCache(tenantId);
    }

    @Override
    public void deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String keyPath) {
        customTranslationService.deleteCustomTranslationKeyByPath(tenantId, customerId, localeCode, keyPath);
        evictFromCache(tenantId);
    }

    @Override
    public void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        customTranslationService.deleteCustomTranslation(tenantId, customerId, localeCode);
        evictFromCache(tenantId);
    }

    private void evictFromCache(TenantId tenantId) {
        evictETags(TranslationCacheKey.forTenant(tenantId));
        clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                .setTranslationCacheInvalidateMsg(TransportProtos.TranslationCacheInvalidateMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build())
                .build());
    }

    @Override
    public void evictETags(TranslationCacheKey cacheKey) {
        TenantId tenantId = cacheKey.getTenantId();
        if (tenantId.isSysTenantId()) {
            etagCache.invalidateAll();
        } else {
            invalidateByFilter(key -> tenantId.equals(key.getTenantId()));
        }
    }

}
