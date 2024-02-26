/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.translation.TranslationCacheKey;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbTranslationService extends AbstractTbEntityService implements TbTranslationService {
    private final TbClusterService clusterService;
    private final CustomTranslationService customTranslationService;
    private final Cache<TranslationCacheKey, String> etagCache;

    public DefaultTbTranslationService(TbClusterService clusterService, CustomTranslationService customTranslationService,
                                 @Value("${cache.translation.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                 @Value("${cache.translation.etag.maxSize:20}") int cacheMaxSize) {
        this.clusterService = clusterService;
        this.customTranslationService = customTranslationService;
        this.etagCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    public CustomTranslation saveCustomTranslation(CustomTranslation customTranslation) {
        CustomTranslation saved = customTranslationService.saveCustomTranslation(customTranslation);
        evictFromCache(customTranslation.getTenantId());
        return saved;
    }

    @Override
    public CustomTranslation patchCustomTranslation(CustomTranslation customTranslation) {
        CustomTranslation saved = customTranslationService.patchCustomTranslation(customTranslation);
        evictFromCache(customTranslation.getTenantId());
        return saved;
    }

    @Override
    public String getETag(TranslationCacheKey translationCacheKey) {
        return etagCache.getIfPresent(translationCacheKey);
    }

    @Override
    public void putETag(TranslationCacheKey translationCacheKey, String etag) {
        etagCache.put(translationCacheKey, etag);
    }

    @Override
    public void evictETags(TenantId tenantId) {
        if (tenantId.isSysTenantId()) {
            etagCache.invalidateAll();
        } else {
            Set<TranslationCacheKey> keysToInvalidate = etagCache
                    .asMap().keySet().stream()
                    .filter(translationCacheKey -> translationCacheKey.getTenantId().equals(tenantId))
                    .collect(Collectors.toSet());
            etagCache.invalidateAll(keysToInvalidate);
        }
    }

    private void evictFromCache(TenantId tenantId) {
        evictETags(tenantId);
        clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                .setTranslationCacheInvalidateMsg(TransportProtos.TranslationCacheInvalidateMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build())
                .build());
    }
}
