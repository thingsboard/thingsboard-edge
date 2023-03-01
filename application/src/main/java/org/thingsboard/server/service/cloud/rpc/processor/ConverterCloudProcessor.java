/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Component
@Slf4j
public class ConverterCloudProcessor extends BaseEdgeProcessor {

    @Autowired
    private ConverterService converterService;

    public ListenableFuture<Void> processConverterMsgFromCloud(TenantId tenantId, ConverterUpdateMsg converterMsg) {
        try {
            ConverterId converterId = new ConverterId(new UUID(converterMsg.getIdMSB(), converterMsg.getIdLSB()));
            switch (converterMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Converter converter = converterService.findConverterById(tenantId, converterId);
                    boolean created = false;
                    if (converter == null) {
                        converter = new Converter();
                        converter.setCreatedTime(Uuids.unixTimestamp(converterId.getId()));
                        converter.setTenantId(tenantId);
                        created = true;
                    }
                    converter.setEdgeTemplate(false);
                    converter.setName(converterMsg.getName());
                    converter.setType(ConverterType.valueOf(converterMsg.getType()));
                    converter.setDebugMode(converterMsg.getDebugMode());

                    converter.setConfiguration(JacksonUtil.toJsonNode(converterMsg.getConfiguration()));
                    converter.setAdditionalInfo(JacksonUtil.toJsonNode(converterMsg.getAdditionalInfo()));

                    if (created) {
                        converterValidator.validate(converter, Converter::getTenantId);
                        converter.setId(converterId);
                    } else {
                        converterValidator.validate(converter, Converter::getTenantId);
                    }

                    Converter savedConverter = converterService.saveConverter(converter);

                    tbClusterService.broadcastEntityStateChangeEvent(savedConverter.getTenantId(), savedConverter.getId(),
                            created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

                    break;
                case UNRECOGNIZED:
                    String errMsg = "Unsupported msg type " + converterMsg.getMsgType();
                    log.error(errMsg);
                    return Futures.immediateFailedFuture(new RuntimeException(errMsg));
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process converter msg [%s]", converterMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }
}
