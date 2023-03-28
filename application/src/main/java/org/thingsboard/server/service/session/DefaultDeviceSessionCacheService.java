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
package org.thingsboard.server.service.session;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.Serializable;
import java.util.Collections;

import static org.thingsboard.server.common.data.CacheConstants.SESSIONS_CACHE;

/**
 * Created by ashvayka on 29.10.18.
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultDeviceSessionCacheService implements DeviceSessionCacheService {

    @Autowired
    protected TbTransactionalCache<DeviceId, DeviceSessionsCacheEntry> cache;

    @Override
    public DeviceSessionsCacheEntry get(DeviceId deviceId) {
        log.debug("[{}] Fetching session data from cache", deviceId);
        return cache.getAndPutInTransaction(deviceId, () ->
                DeviceSessionsCacheEntry.newBuilder().addAllSessions(Collections.emptyList()).build(), false);
    }

    @Override
    public DeviceSessionsCacheEntry put(DeviceId deviceId, DeviceSessionsCacheEntry sessions) {
        log.debug("[{}] Pushing session data to cache: {}", deviceId, sessions);
        cache.put(deviceId, sessions);
        return sessions;
    }
}
