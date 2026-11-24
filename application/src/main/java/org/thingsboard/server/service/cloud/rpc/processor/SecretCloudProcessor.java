// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
@Component
public class SecretCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processSecretMsgFromCloud(TenantId tenantId, SecretUpdateMsg secretUpdateMsg) {
        SecretId secretId = new SecretId(new UUID(secretUpdateMsg.getIdMSB(), secretUpdateMsg.getIdLSB()));
        switch (secretUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                try {
                    Secret secret = JacksonUtil.fromString(secretUpdateMsg.getEntity(), Secret.class, true);
                    if (secret == null) {
                        throw new RuntimeException("[{" + tenantId + "}] SecretUpdateMsg {" + secretUpdateMsg + " } cannot be converted to secret");
                    }
                    // The synced secret carries the cloud's id. Reuse the existing row's id (matched by tenant + name)
                    // so it is updated in place instead of colliding on the (tenant_id, name) unique constraint with an
                    // edge auto-generated secret. Use the secret's own tenant so system-tenant secrets are matched too.
                    Secret existing = edgeCtx.getSecretService().findSecretByName(secret.getTenantId(), secret.getName());
                    if (existing != null) {
                        secret.setId(existing.getId());
                        secret.setCreatedTime(existing.getCreatedTime());
                    }
                    edgeCtx.getSecretService().saveSecret(tenantId, secret, false);
                } catch (Exception e) {
                    log.error("[{}] Failed to process SecretUpdateMsg [{}]", tenantId, secretUpdateMsg, e);
                    throw e;
                }
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                SecretInfo secretInfo = new SecretInfo(secretId);
                edgeCtx.getSecretService().deleteSecret(tenantId, secretInfo);
                break;
            default:
                log.warn("[{}] Unsupported msg type [{}] for secret", tenantId, secretUpdateMsg.getMsgType());
        }
        return Futures.immediateFuture(null);
    }

}
