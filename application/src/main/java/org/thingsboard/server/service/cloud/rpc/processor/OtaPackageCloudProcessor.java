/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class OtaPackageCloudProcessor extends BaseEdgeProcessor {

    private final Lock otaPackageCreationLock = new ReentrantLock();

    public ListenableFuture<Void> processOtaPackageMsgFromCloud(TenantId tenantId, OtaPackageUpdateMsg otaPackageUpdateMsg) {
        OtaPackageId otaPackageId = new OtaPackageId(new UUID(otaPackageUpdateMsg.getIdMSB(), otaPackageUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (otaPackageUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    otaPackageCreationLock.lock();
                    try {
                        OtaPackage otaPackage = JacksonUtil.fromString(otaPackageUpdateMsg.getEntity(), OtaPackage.class, true);
                        if (otaPackage == null) {
                            throw new RuntimeException("[{" + tenantId + "}] otaPackageUpdateMsg {" + otaPackageUpdateMsg + "} cannot be converted to ota package");
                        }
                        otaPackageService.saveOtaPackage(otaPackage, false);
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
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

}
