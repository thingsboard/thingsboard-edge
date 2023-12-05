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
package org.thingsboard.server.service.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbImageService extends AbstractTbEntityService implements TbImageService {

    private final TbClusterService clusterService;
    private final ImageService imageService;
    private final Cache<ImageCacheKey, String> etagCache;

    public DefaultTbImageService(TbClusterService clusterService, ImageService imageService,
                                 @Value("${cache.image.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                 @Value("${cache.image.etag.maxSize:10000}") int cacheMaxSize) {
        this.clusterService = clusterService;
        this.imageService = imageService;
        this.etagCache = Caffeine.newBuilder()
                .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .build();
    }

    @Override
    public String getETag(ImageCacheKey imageCacheKey) {
        return etagCache.getIfPresent(imageCacheKey);
    }

    @Override
    public void putETag(ImageCacheKey imageCacheKey, String etag) {
        etagCache.put(imageCacheKey, etag);
    }

    @Override
    public void evictETag(ImageCacheKey imageCacheKey) {
        etagCache.invalidate(imageCacheKey);
    }

    @Override
    public TbResourceInfo save(TbResource image, User user) throws Exception {
        ActionType actionType = image.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = image.getTenantId();
        try {
            var oldEtag = getEtag(image);
            if (image.getId() == null && StringUtils.isNotEmpty(image.getResourceKey())) {
                var existingImage = imageService.getImageInfoByTenantIdAndKey(tenantId, image.getResourceKey());
                if (existingImage != null) {
                    image.setId(existingImage.getId());
                }
            }
            TbResourceInfo savedImage = imageService.saveImage(image);
            notificationEntityService.logEntityAction(tenantId, savedImage.getId(), savedImage, actionType, user);
            if (oldEtag.isPresent()) {
                var newEtag = getEtag(savedImage);
                if (newEtag.isPresent() && !oldEtag.get().equals(newEtag.get())) {
                    evictETag(new ImageCacheKey(image.getTenantId(), image.getResourceKey(), false));
                    evictETag(new ImageCacheKey(image.getTenantId(), image.getResourceKey(), true));
                    clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                            .setResourceCacheInvalidateMsg(TransportProtos.ResourceCacheInvalidateMsg.newBuilder()
                                    .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                                    .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                                    .setResourceKey(image.getResourceKey())
                                    .build())
                            .build());
                }
            }
            return savedImage;
        } catch (Exception e) {
            image.setData(null);
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.TB_RESOURCE), image, actionType, user, e);
            throw e;
        }
    }

    private Optional<String> getEtag(TbResourceInfo image) throws JsonProcessingException {
        var descriptor = image.getDescriptor(ImageDescriptor.class);
        return Optional.ofNullable(descriptor != null ? descriptor.getEtag() : null);
    }

    private Optional<String> getPreviewEtag(TbResourceInfo image) throws JsonProcessingException {
        var descriptor = image.getDescriptor(ImageDescriptor.class);
        descriptor = descriptor != null ? descriptor.getPreviewDescriptor() : null;
        return Optional.ofNullable(descriptor != null ? descriptor.getEtag() : null);
    }

    @Override
    public TbResourceInfo save(TbResourceInfo imageInfo, User user) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            imageInfo = imageService.saveImageInfo(imageInfo);
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.UPDATED, user);
            return imageInfo;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public TbImageDeleteResult delete(TbResourceInfo imageInfo, User user, boolean force) {
        TenantId tenantId = imageInfo.getTenantId();
        TbResourceId imageId = imageInfo.getId();
        try {
            TbImageDeleteResult result = imageService.deleteImage(imageInfo, force);
            if (result.isSuccess()) {
                notificationEntityService.logEntityAction(tenantId, imageId, imageInfo, ActionType.DELETED, user, imageId.toString());
            }
            return result;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, imageId, ActionType.DELETED, user, e, imageId.toString());
            throw e;
        }
    }
}
