/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
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
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleNodeProto;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RuleChainCloudProcessor extends BaseCloudProcessor {

    @Autowired
    private RuleChainService ruleChainService;

    public ListenableFuture<Void> processRuleChainMsgFromCloud(TenantId tenantId, RuleChainUpdateMsg ruleChainUpdateMsg,
                                                               Long queueStartTs) {
        try {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
                    boolean created = false;
                    if (ruleChain == null) {
                        created = true;
                        ruleChain = new RuleChain();
                        ruleChain.setId(ruleChainId);
                        ruleChain.setCreatedTime(Uuids.unixTimestamp(ruleChainId.getId()));
                        ruleChain.setTenantId(tenantId);
                        ruleChain.setRoot(false);
                    }
                    ruleChain.setName(ruleChainUpdateMsg.getName());
                    if (ruleChainUpdateMsg.hasFirstRuleNodeIdMSB() && ruleChainUpdateMsg.hasFirstRuleNodeIdLSB()) {
                        RuleNodeId firstRuleNodeId =
                                new RuleNodeId(new UUID(ruleChainUpdateMsg.getFirstRuleNodeIdMSB(),
                                        ruleChainUpdateMsg.getFirstRuleNodeIdLSB()));
                        ruleChain.setFirstRuleNodeId(firstRuleNodeId);
                    } else {
                        ruleChain.setFirstRuleNodeId(null);
                    }
                    ruleChain.setConfiguration(JacksonUtil.toJsonNode(ruleChainUpdateMsg.getConfiguration()));
                    ruleChain.setType(RuleChainType.CORE);
                    ruleChain.setDebugMode(ruleChainUpdateMsg.getDebugMode());
                    ruleChainService.saveRuleChain(ruleChain);

                    if (ruleChainUpdateMsg.getRoot()) {
                        ruleChainService.setRootRuleChain(tenantId, ruleChainId);
                    } else {
                        setRootIfFirstRuleChain(tenantId, ruleChainId);
                    }
                    tbClusterService.broadcastEntityStateChangeEvent(ruleChain.getTenantId(), ruleChain.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
                    return cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.RULE_CHAIN, EdgeEventActionType.RULE_CHAIN_METADATA_REQUEST, ruleChainId, null, null, queueStartTs);
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
        }
        return Futures.immediateFuture(null);
    }

    private void setRootIfFirstRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        // @voba - this is hack because of incorrect isRoot flag in the first rule chain
        long ruleChainsCnt = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, new PageLink(100)).getTotalElements();
        if (ruleChainsCnt == 1) {
            ruleChainService.setRootRuleChain(tenantId, ruleChainId);
        }
    }

    public ListenableFuture<Void> processRuleChainMetadataMsgFromCloud(TenantId tenantId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        try {
            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChainMetaData ruleChainMetadata = new RuleChainMetaData();
                    UUID ruleChainUUID = new UUID(ruleChainMetadataUpdateMsg.getRuleChainIdMSB(), ruleChainMetadataUpdateMsg.getRuleChainIdLSB());
                    RuleChainId ruleChainId = new RuleChainId(ruleChainUUID);
                    ruleChainMetadata.setRuleChainId(ruleChainId);
                    ruleChainMetadata.setNodes(parseNodeProtos(ruleChainId, ruleChainMetadataUpdateMsg.getNodesList()));
                    ruleChainMetadata.setConnections(parseConnectionProtos(ruleChainMetadataUpdateMsg.getConnectionsList()));
                    ruleChainMetadata.setRuleChainConnections(parseRuleChainConnectionProtos(ruleChainMetadataUpdateMsg.getRuleChainConnectionsList()));
                    if (ruleChainMetadataUpdateMsg.getFirstNodeIndex() != -1) {
                        ruleChainMetadata.setFirstNodeIndex(ruleChainMetadataUpdateMsg.getFirstNodeIndex());
                    }
                    if (ruleChainMetadata.getNodes().size() > 0) {
                        ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetadata);
                        tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainId, ComponentLifecycleEvent.UPDATED);
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

    private List<RuleChainConnectionInfo> parseRuleChainConnectionProtos(List<org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto> ruleChainConnectionsList) throws IOException {
        List<RuleChainConnectionInfo> result = new ArrayList<>();
        for (org.thingsboard.server.gen.edge.v1.RuleChainConnectionInfoProto proto : ruleChainConnectionsList) {
            RuleChainConnectionInfo info = new RuleChainConnectionInfo();
            info.setFromIndex(proto.getFromIndex());
            info.setTargetRuleChainId(new RuleChainId(new UUID(proto.getTargetRuleChainIdMSB(), proto.getTargetRuleChainIdLSB())));
            info.setType(proto.getType());
            info.setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.readTree(proto.getAdditionalInfo()));
            result.add(info);
        }
        return result;
    }

    private List<NodeConnectionInfo> parseConnectionProtos(List<org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto> connectionsList) {
        List<NodeConnectionInfo> result = new ArrayList<>();
        for (org.thingsboard.server.gen.edge.v1.NodeConnectionInfoProto proto : connectionsList) {
            NodeConnectionInfo info = new NodeConnectionInfo();
            info.setFromIndex(proto.getFromIndex());
            info.setToIndex(proto.getToIndex());
            info.setType(proto.getType());
            result.add(info);
        }
        return result;
    }

    private List<RuleNode> parseNodeProtos(RuleChainId ruleChainId, List<RuleNodeProto> nodesList) throws IOException {
        List<RuleNode> result = new ArrayList<>();
        for (RuleNodeProto proto : nodesList) {
            RuleNode ruleNode = new RuleNode();
            RuleNodeId ruleNodeId = new RuleNodeId(new UUID(proto.getIdMSB(), proto.getIdLSB()));
            ruleNode.setId(ruleNodeId);
            ruleNode.setCreatedTime(Uuids.unixTimestamp(ruleNodeId.getId()));
            ruleNode.setRuleChainId(ruleChainId);
            ruleNode.setType(proto.getType());
            ruleNode.setName(proto.getName());
            ruleNode.setDebugMode(proto.getDebugMode());
            ruleNode.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree(proto.getConfiguration()));
            ruleNode.setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.readTree(proto.getAdditionalInfo()));
            result.add(ruleNode);
        }
        return result;
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
