/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class OtaPackageCloudProcessor extends BaseEdgeProcessor {

    private final Lock otaPackageCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processOtaPackageMsgFromCloud(TenantId tenantId, OtaPackageUpdateMsg otaPackageUpdateMsg) {
        OtaPackageId otaPackageId = new OtaPackageId(new UUID(otaPackageUpdateMsg.getIdMSB(), otaPackageUpdateMsg.getIdLSB()));
        switch (otaPackageUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                otaPackageCreationLock.lock();
                try {
                    OtaPackageInfo otaPackageInfo = otaPackageService.findOtaPackageInfoById(tenantId, otaPackageId);
                    if (otaPackageInfo == null) {
                        otaPackageInfo = new OtaPackageInfo();
                        otaPackageInfo.setId(otaPackageId);
                        otaPackageInfo.setCreatedTime(Uuids.unixTimestamp(otaPackageId.getId()));
                        otaPackageInfo.setTenantId(tenantId);
                    }
                    otaPackageInfo.setDeviceProfileId(new DeviceProfileId(new UUID(otaPackageUpdateMsg.getDeviceProfileIdMSB(), otaPackageUpdateMsg.getDeviceProfileIdLSB())));
                    otaPackageInfo.setType(OtaPackageType.valueOf(otaPackageUpdateMsg.getType()));
                    otaPackageInfo.setTitle(otaPackageUpdateMsg.getTitle());
                    otaPackageInfo.setVersion(otaPackageUpdateMsg.getVersion());
                    otaPackageInfo.setTag(otaPackageUpdateMsg.getTag());
                    if (otaPackageUpdateMsg.hasUrl()) {
                        otaPackageInfo.setUrl(otaPackageUpdateMsg.getUrl());
                    }
                    if (otaPackageUpdateMsg.hasFileName()) {
                        otaPackageInfo.setFileName(otaPackageUpdateMsg.getFileName());
                    }
                    if (otaPackageUpdateMsg.hasContentType()) {
                        otaPackageInfo.setContentType(otaPackageUpdateMsg.getContentType());
                    }
                    if (otaPackageUpdateMsg.hasChecksumAlgorithm()) {
                        otaPackageInfo.setChecksumAlgorithm(ChecksumAlgorithm.valueOf(otaPackageUpdateMsg.getChecksumAlgorithm()));
                    }
                    if (otaPackageUpdateMsg.hasChecksum()) {
                        otaPackageInfo.setChecksum(otaPackageUpdateMsg.getChecksum());
                    }
                    if (otaPackageUpdateMsg.hasDataSize()) {
                        otaPackageInfo.setDataSize(otaPackageUpdateMsg.getDataSize());
                    }
                    if (otaPackageUpdateMsg.hasAdditionalInfo()) {
                        otaPackageInfo.setAdditionalInfo(JacksonUtil.toJsonNode(otaPackageUpdateMsg.getAdditionalInfo()));
                    }
                    otaPackageService.saveOtaPackageInfo(otaPackageInfo, otaPackageUpdateMsg.hasUrl(), false);
                    if (otaPackageUpdateMsg.hasData()) {
                        OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
                        otaPackage.setData(ByteBuffer.wrap(otaPackageUpdateMsg.getData().toByteArray()));
                        otaPackageService.saveOtaPackage(otaPackage, false);
                    }
                } finally {
                    otaPackageCreationLock.unlock();
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
                if (otaPackage != null) {
                    otaPackageService.deleteOtaPackage(tenantId, otaPackageId);
                }
                break;
            case UNRECOGNIZED:
                return handleUnsupportedMsgType(otaPackageUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }
}
