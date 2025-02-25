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
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbTransportComponent
public class DefaultTransportDeviceProfileCache implements TransportDeviceProfileCache {

    private final Lock deviceProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<DeviceProfileId, DeviceProfile> deviceProfiles = new ConcurrentHashMap<>();

    private TransportService transportService;

    @Lazy
    @Autowired
    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    @Override
    public DeviceProfile getOrCreate(DeviceProfileId id, TransportProtos.DeviceProfileProto proto) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            profile = ProtoUtils.fromProto(proto);
            deviceProfiles.put(id, profile);
        }
        return profile;
    }

    @Override
    public DeviceProfile get(DeviceProfileId id) {
        return this.getDeviceProfile(id);
    }

    @Override
    public void put(DeviceProfile profile) {
        deviceProfiles.put(profile.getId(), profile);
    }

    @Override
    public DeviceProfile put(TransportProtos.DeviceProfileProto proto) {
        DeviceProfile deviceProfile = ProtoUtils.fromProto(proto);
        put(deviceProfile);
        return deviceProfile;
    }

    @Override
    public void evict(DeviceProfileId id) {
        deviceProfiles.remove(id);
    }


    private DeviceProfile getDeviceProfile(DeviceProfileId id) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            deviceProfileFetchLock.lock();
            try {
                TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                        .setEntityType(EntityType.DEVICE_PROFILE.name())
                        .setEntityIdMSB(id.getId().getMostSignificantBits())
                        .setEntityIdLSB(id.getId().getLeastSignificantBits())
                        .build();
                TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                profile = ProtoUtils.fromProto(entityProfileMsg.getDeviceProfile());
                this.put(profile);
            } finally {
                deviceProfileFetchLock.unlock();
            }
        }
        return profile;
    }
}
