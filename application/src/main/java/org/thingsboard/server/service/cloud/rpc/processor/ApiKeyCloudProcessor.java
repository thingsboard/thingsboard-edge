/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.apikey.BaseApiKeyProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class ApiKeyCloudProcessor extends BaseApiKeyProcessor {

    public ListenableFuture<Void> processApiKeyMsgFromCloud(TenantId tenantId, ApiKeyUpdateMsg apiKeyUpdateMsg) {
        ApiKeyId apiKeyId = new ApiKeyId(new UUID(apiKeyUpdateMsg.getIdMSB(), apiKeyUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (apiKeyUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    boolean created = saveOrUpdateApiKey(tenantId, apiKeyId, apiKeyUpdateMsg);
                    if (created) {
                        ApiKey apiKey = edgeCtx.getApiKeyService().findApiKeyById(tenantId, apiKeyId);
                        if (apiKey != null) {
                            pushEntityEventToRuleEngine(tenantId, null, apiKey, TbMsgType.ENTITY_CREATED);
                        }
                    }
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteApiKey(tenantId, null, apiKeyId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(apiKeyUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        ApiKeyId apiKeyId = new ApiKeyId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                ApiKey apiKey = edgeCtx.getApiKeyService().findApiKeyById(cloudEvent.getTenantId(), apiKeyId);
                if (apiKey != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    ApiKeyUpdateMsg apiKeyUpdateMsg = EdgeMsgConstructorUtils.constructApiKeyUpdatedMsg(msgType, apiKey);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addApiKeyUpdateMsg(apiKeyUpdateMsg)
                            .build();
                } else {
                    log.info("Skipping event as ApiKey was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                ApiKeyUpdateMsg apiKeyUpdateMsg = EdgeMsgConstructorUtils.constructApiKeyDeleteMsg(apiKeyId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addApiKeyUpdateMsg(apiKeyUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.API_KEY;
    }

}
