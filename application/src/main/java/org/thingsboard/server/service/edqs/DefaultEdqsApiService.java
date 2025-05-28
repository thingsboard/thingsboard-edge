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
package org.thingsboard.server.service.edqs;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.edqs.query.EdqsRequest;
import org.thingsboard.server.common.data.edqs.query.EdqsResponse;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.edqs.EdqsApiService;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.EdqsClientQueueFactory;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.api.supported:true}' == 'true' && ('${service.type:null}' == 'monolith' || '${service.type:null}' == 'tb-core')")
public class DefaultEdqsApiService implements EdqsApiService {

    private final EdqsPartitionService edqsPartitionService;
    private final EdqsClientQueueFactory queueFactory;
    private TbQueueRequestTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> requestTemplate;

    @PostConstruct
    private void init() {
        requestTemplate = queueFactory.createEdqsRequestTemplate();
        requestTemplate.init();
    }

    @Override
    public ListenableFuture<EdqsResponse> processRequest(TenantId tenantId, CustomerId customerId, EdqsRequest request) {
        var requestMsg = ToEdqsMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTs(System.currentTimeMillis())
                .setRequestMsg(TransportProtos.EdqsRequestMsg.newBuilder()
                        .setValue(JacksonUtil.toString(request))
                        .build());
        if (customerId != null && !customerId.isNullUid()) {
            requestMsg.setCustomerIdMSB(customerId.getId().getMostSignificantBits());
            requestMsg.setCustomerIdLSB(customerId.getId().getLeastSignificantBits());
        }

        UUID key = UUID.randomUUID();
        Integer partition = edqsPartitionService.resolvePartition(tenantId, key);
        ListenableFuture<TbProtoQueueMsg<FromEdqsMsg>> resultFuture = requestTemplate.send(new TbProtoQueueMsg<>(key, requestMsg.build()), partition);
        return Futures.transform(resultFuture, msg -> {
            TransportProtos.EdqsResponseMsg responseMsg = msg.getValue().getResponseMsg();
            return JacksonUtil.fromString(responseMsg.getValue(), EdqsResponse.class);
        }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @PreDestroy
    private void stop() {
        requestTemplate.stop();
    }

}
