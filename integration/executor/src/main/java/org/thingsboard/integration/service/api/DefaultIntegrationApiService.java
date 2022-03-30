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
package org.thingsboard.integration.service.api;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationApiResponseMsg;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultIntegrationApiService implements IntegrationApiService {

    private final TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> apiTemplate;
    private final ExecutorService callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "integration-api-callback");

    @PostConstruct
    public void init() {
        apiTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        if (apiTemplate != null) {
            apiTemplate.stop();
        }
        callbackExecutor.shutdownNow();
    }

    @Override
    public ListenableFuture<List<IntegrationInfo>> getActiveIntegrationList(IntegrationType type) {
        var request = TransportProtos.IntegrationInfoListRequestProto.newBuilder().setEnabled(true).setType(type.name()).build();

        var responseFuture =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationListRequest(request).build()));

        return Futures.transform(responseFuture, this::parseListFromProto, callbackExecutor);
    }

    private List<IntegrationInfo> parseListFromProto(TbProtoQueueMsg<TransportProtos.IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<IntegrationInfo>();

        var response = proto.getValue().getIntegrationListResponse().getIntegrationInfoListList();

        for (var integrationInfoProto : response) {
            var integrationId = new IntegrationId(new UUID(integrationInfoProto.getIntegrationIdMSB(), integrationInfoProto.getIntegrationIdLSB()));
            var tenantId = new TenantId(new UUID(integrationInfoProto.getTenantIdMSB(), integrationInfoProto.getTenantIdLSB()));
            result.add(new IntegrationInfo(integrationId, tenantId, integrationInfoProto.getName(), IntegrationType.valueOf(integrationInfoProto.getType())));
        }

        return result;
    }
}
