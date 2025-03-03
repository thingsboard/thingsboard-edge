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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.rule.BaseRuleChainProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class RuleChainCloudProcessor extends BaseRuleChainProcessor {

    public ListenableFuture<Void> processRuleChainMsgFromCloud(TenantId tenantId, RuleChainUpdateMsg ruleChainUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateRuleChain(tenantId, ruleChainId, ruleChainUpdateMsg);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    RuleChain ruleChainById = edgeCtx.getRuleChainService().findRuleChainById(tenantId, ruleChainId);
                    if (ruleChainById != null) {
                        edgeCtx.getRuleChainService().deleteRuleChainById(tenantId, ruleChainId);
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

    private void saveOrUpdateRuleChain(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateMsg ruleChainUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = saveOrUpdateRuleChain(tenantId, ruleChainId, ruleChainUpdateMsg, RuleChainType.CORE);
        Boolean isRoot = resultPair.getSecond();
        if (isRoot) {
            edgeCtx.getRuleChainService().setRootRuleChain(tenantId, ruleChainId);
        }
    }

    public ListenableFuture<Void> processRuleChainMetadataMsgFromCloud(TenantId tenantId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateRuleChainMetadata(tenantId, ruleChainMetadataUpdateMsg);
                    break;
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(ruleChainMetadataUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process rule chain metadata update msg %s", ruleChainMetadataUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        RuleChainId ruleChainId = new RuleChainId(cloudEvent.getEntityId());
        UplinkMsg msg = null;
        switch (cloudEvent.getAction()) {
            case ADDED:
            case UPDATED:
                RuleChain ruleChain = edgeCtx.getRuleChainService().findRuleChainById(cloudEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg = EdgeMsgConstructorUtils.constructRuleChainUpdatedMsg(msgType, ruleChain, ruleChain.isRoot());
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg);

                    RuleChainMetaData ruleChainMetaData = edgeCtx.getRuleChainService().loadRuleChainMetaData(cloudEvent.getTenantId(), ruleChainId);
                    RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = EdgeMsgConstructorUtils
                            .constructRuleChainMetadataUpdatedMsg(msgType, ruleChainMetaData);
                    builder.addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg);

                    msg = builder.build();
                } else {
                    log.info("Skipping event as asset was not found [{}]", cloudEvent);
                }
                break;
            case DELETED:
                msg = UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainUpdateMsg(EdgeMsgConstructorUtils.constructRuleChainDeleteMsg(ruleChainId))
                        .build();
                break;
        }
        return msg;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.RULE_CHAIN;
    }
}
