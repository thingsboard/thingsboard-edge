// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbCoreComponent
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
                        if (otaPackage.getData() == null) {
                            edgeCtx.getOtaPackageService().saveOtaPackageInfo(new OtaPackageInfo(otaPackage), otaPackage.hasUrl(), false);
                        } else {
                            edgeCtx.getOtaPackageService().saveOtaPackage(otaPackage, false);
                        }
                    } finally {
                        otaPackageCreationLock.unlock();
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    OtaPackage otaPackage = edgeCtx.getOtaPackageService().findOtaPackageById(tenantId, otaPackageId);
                    if (otaPackage != null) {
                        edgeCtx.getOtaPackageService().deleteOtaPackage(tenantId, otaPackageId);
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
