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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RuleChainCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private RuleChainService ruleChainService;

    public ListenableFuture<Void> processRuleChainMsgFromCloud(TenantId tenantId, RuleChainUpdateMsg ruleChainUpdateMsg,
                                                               Long queueStartTs) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChain ruleChainMsg = JacksonUtil.fromEdgeString(ruleChainUpdateMsg.getEntity(), RuleChain.class);
                    if (ruleChainMsg == null) {
                        throw new RuntimeException("[{" + tenantId + "}] ruleChainUpdateMsg {" + ruleChainUpdateMsg + "} cannot be converted to rule chain");
                    }
                    RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
                    boolean created = false;
                    boolean isRoot = ruleChainMsg.isRoot();
                    if (ruleChain == null) {
                        created = true;
                        ruleChainMsg.setRoot(false);
                    }
                    ruleChainMsg.setType(RuleChainType.CORE);
                    ruleChainService.saveRuleChain(ruleChainMsg);

                    if (isRoot) {
                        ruleChainService.setRootRuleChain(tenantId, ruleChainId);
                    }
                    tbClusterService.broadcastEntityStateChangeEvent(ruleChainMsg.getTenantId(), ruleChainMsg.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
                    return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.RULE_CHAIN, EdgeEventActionType.RULE_CHAIN_METADATA_REQUEST, ruleChainId, null, queueStartTs);
                case ENTITY_DELETED_RPC_MESSAGE:
                    RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, ruleChainId);
                    if (ruleChainById != null) {
                        List<RuleNode> referencingRuleNodes = ruleChainService.getReferencingRuleChainNodes(tenantId, ruleChainId);

                        Set<RuleChainId> referencingRuleChainIds = referencingRuleNodes.stream().map(RuleNode::getRuleChainId).collect(Collectors.toSet());

                        ruleChainService.deleteRuleChainById(tenantId, ruleChainId);

                        referencingRuleChainIds.remove(ruleChainId);

                        referencingRuleChainIds.forEach(referencingRuleChainId ->
                                tbClusterService.broadcastEntityStateChangeEvent(tenantId, referencingRuleChainId, ComponentLifecycleEvent.UPDATED));

                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainId, ComponentLifecycleEvent.DELETED);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(ruleChainUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process rule chain update msg %s", ruleChainUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processRuleChainMetadataMsgFromCloud(TenantId tenantId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        try {
            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChainMetaData ruleChainMetadata = JacksonUtil.fromEdgeString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class);
                    if (ruleChainMetadata == null) {
                        throw new RuntimeException("[{" + tenantId + "}] ruleChainMetadataUpdateMsg {" + ruleChainMetadataUpdateMsg + "} cannot be converted to rule chain metadata");
                    }
                    if (ruleChainMetadata.getNodes().size() > 0) {
                        ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetadata, Function.identity());
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainMetadata.getRuleChainId(), ComponentLifecycleEvent.UPDATED);
                    }
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(ruleChainMetadataUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process rule chain metadata update msg %s", ruleChainMetadataUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    public UplinkMsg convertRuleChainMetadataRequestEventToUplink(CloudEvent cloudEvent) {
        EntityId ruleChainId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsg);
        return builder.build();
    }
}
