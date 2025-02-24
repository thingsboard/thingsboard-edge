/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@TbCoreComponent
public class RuleChainCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processRuleChainMsgFromCloud(TenantId tenantId, RuleChainUpdateMsg ruleChainUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB()));
            switch (ruleChainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChain ruleChainMsg = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
                    if (ruleChainMsg == null) {
                        throw new RuntimeException("[{" + tenantId + "}] ruleChainUpdateMsg {" + ruleChainUpdateMsg + "} cannot be converted to rule chain");
                    }
                    boolean isRoot = ruleChainMsg.isRoot();
                    ruleChainMsg.setRoot(false);
                    ruleChainMsg.setType(RuleChainType.CORE);
                    edgeCtx.getRuleChainService().saveRuleChain(ruleChainMsg);

                    if (isRoot) {
                        edgeCtx.getRuleChainService().setRootRuleChain(tenantId, ruleChainId);
                    }
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

    public ListenableFuture<Void> processRuleChainMetadataMsgFromCloud(TenantId tenantId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        try {
            cloudSynchronizationManager.getSync().set(true);
            switch (ruleChainMetadataUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    RuleChainMetaData ruleChainMetadata = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
                    if (ruleChainMetadata == null) {
                        throw new RuntimeException("[{" + tenantId + "}] ruleChainMetadataUpdateMsg {" + ruleChainMetadataUpdateMsg + "} cannot be converted to rule chain metadata");
                    }
                    if (!ruleChainMetadata.getNodes().isEmpty()) {
                        edgeCtx.getRuleChainService().saveRuleChainMetaData(tenantId, ruleChainMetadata, Function.identity(), true, false);
                    }
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

}
