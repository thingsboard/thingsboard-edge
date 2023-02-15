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
package org.thingsboard.server.service.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.DefaultIntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultTbIntegrationDownlinkService implements TbIntegrationDownlinkService {

    private final PartitionService partitionService;
    private final IntegrationService integrationService;
    private final RemoteIntegrationRpcService remoteRpcService;
    private final TbQueueProducerProvider producerProvider;

    @Override
    public void onRuleEngineDownlinkMsg(TenantId tenantId, IntegrationId integrationId, IntegrationDownlinkMsgProto downlinkMsg, TbCallback callback) {
        Integration integration = integrationService.findIntegrationById(tenantId, integrationId);
        if (integration == null) {
            callback.onFailure(new TbNodeException("Integration is missing!"));
        } else if (!integration.isEnabled()) {
            callback.onFailure(new TbNodeException("Integration is disabled!"));
        } else if (integration.isRemote()) {
            onDownlinkToRemoteIntegrationMsg(tenantId, integrationId, downlinkMsg);
            callback.onSuccess();
        } else {
            var producer = producerProvider.getTbIntegrationExecutorDownlinkMsgProducer();
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, integration.getType().name(), tenantId, integrationId)
                    .newByTopic(HashPartitionService.getIntegrationDownlinkTopic(integration.getType()));
            producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ToIntegrationExecutorDownlinkMsg.newBuilder().setDownlinkMsg(downlinkMsg).build()), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }
            });
        }
    }

    @Override
    public void onDownlinkToRemoteIntegrationMsg(IntegrationDownlinkMsgProto msgProto, TbCallback callback) {
        TenantId tenantId = new TenantId(new UUID(msgProto.getTenantIdMSB(), msgProto.getTenantIdLSB()));
        IntegrationId integrationId = new IntegrationId(new UUID(msgProto.getIntegrationIdMSB(), msgProto.getIntegrationIdLSB()));
        onDownlinkToRemoteIntegrationMsg(tenantId, integrationId, msgProto);
        callback.onSuccess();
    }

    private void onDownlinkToRemoteIntegrationMsg(TenantId tenantId, IntegrationId integrationId, IntegrationDownlinkMsgProto downlinkMsg) {
        IntegrationDownlinkMsg msg = new DefaultIntegrationDownlinkMsg(tenantId, integrationId,
                TbMsg.fromBytes(null, downlinkMsg.getData().toByteArray(), TbMsgCallback.EMPTY), null);
        remoteRpcService.handleRemoteDownlink(msg);
    }
}
