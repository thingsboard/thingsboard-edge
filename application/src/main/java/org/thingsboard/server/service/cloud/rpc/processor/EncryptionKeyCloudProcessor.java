// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.EncryptionKeyUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
public class EncryptionKeyCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processEncryptionKeyMsgFromCloud(TenantId tenantId, EncryptionKeyUpdateMsg keyMsg) {
        switch (keyMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    EncryptionKey key = JacksonUtil.fromString(keyMsg.getEntity(), EncryptionKey.class, true);
                    if (key == null) {
                        throw new RuntimeException("[{" + tenantId + "}] encryptionKeyUpdateMsg {" + keyMsg + " } cannot be converted to encryptionKey");
                    }
                    edgeCtx.getEncryptionService().save(tenantId, key);
                } catch (Exception e) {
                    log.error("[{}] Failed to process encryption key update msg [{}]", tenantId, keyMsg, e);
                    throw e;
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                edgeCtx.getEncryptionService().deleteEncryptionKeyByTenantId(tenantId);
                break;
            default:
                log.warn("[{}] Unsupported msg type [{}] for encryption key", tenantId, keyMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

}
