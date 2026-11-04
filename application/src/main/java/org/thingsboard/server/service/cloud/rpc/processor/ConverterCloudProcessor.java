// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.service.validator.ConverterDataValidator;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class ConverterCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private ConverterDataValidator converterValidator;

    public ListenableFuture<Void> processConverterMsgFromCloud(TenantId tenantId, ConverterUpdateMsg converterUpdateMsg) {
        try {
            ConverterId converterId = new ConverterId(new UUID(converterUpdateMsg.getIdMSB(), converterUpdateMsg.getIdLSB()));
            switch (converterUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Converter converter = JacksonUtil.fromString(converterUpdateMsg.getEntity(), Converter.class, true);
                    if (converter == null) {
                        throw new RuntimeException("[{" + tenantId + "}] converterUpdateMsg {" + converterUpdateMsg + "} cannot be converted to convertor");
                    }
                    Converter converterById = edgeCtx.getConverterService().findConverterById(tenantId, converterId);
                    boolean created = false;
                    if (converterById == null) {
                        created = true;
                        converter.setId(null);
                    }
                    converter.setEdgeTemplate(false);

                    converterValidator.validate(converter, Converter::getTenantId);

                    if (created) {
                        converter.setId(converterId);
                    }

                    Converter savedConverter = edgeCtx.getConverterService().saveConverter(converter, false);

                    edgeCtx.getClusterService().broadcastEntityStateChangeEvent(savedConverter.getTenantId(), savedConverter.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

                    break;
                case UNRECOGNIZED:
                    String errMsg = "Unsupported msg type " + converterUpdateMsg.getMsgType();
                    log.error(errMsg);
                    return Futures.immediateFailedFuture(new RuntimeException(errMsg));
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process converter msg [%s]", converterUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

}
