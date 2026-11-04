// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.gen.edge.v1.CustomTranslationUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
public class CustomTranslationCloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processCustomTranslationMsgFromCloud(TenantId tenantId, CustomTranslationUpdateMsg customTranslationUpdateMsg) {
        try {
            CustomTranslation customTranslation = JacksonUtil.fromString(customTranslationUpdateMsg.getEntity(), CustomTranslation.class, true);
            if (customTranslation == null) {
                throw new RuntimeException("[{" + tenantId + "}] customTranslationUpdateMsg {" + customTranslationUpdateMsg + "} cannot be converted to custom translation");
            }
            switch (customTranslationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    edgeCtx.getCustomTranslationService().saveCustomTranslation(customTranslation);
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    edgeCtx.getCustomTranslationService().deleteCustomTranslation(customTranslation.getTenantId(), customTranslation.getCustomerId(), customTranslation.getLocaleCode());
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(customTranslationUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = "Exception during updating custom translation";
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

}
