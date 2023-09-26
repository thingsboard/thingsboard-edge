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
package org.thingsboard.integration.service.api;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.gen.integration.ConverterRequestProto;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationInfoListRequestProto;
import org.thingsboard.server.gen.integration.IntegrationRequestProto;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;
import org.thingsboard.server.service.integration.IntegrationProtoUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class TbIntegrationExecutorIntegrationConfigurationService implements IntegrationConfigurationService {

    private final DataDecodingEncodingService dataDecodingEncodingService;
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
    public List<IntegrationInfo> getActiveIntegrationList(IntegrationType type, boolean remote) {
        while (true) {
            try {
                var request = IntegrationInfoListRequestProto.newBuilder().setEnabled(true).setRemote(remote).setType(type.name()).build();
                var response =
                        apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationListRequest(request).build()));
                return Futures.transform(response, this::parseListFromProto, callbackExecutor).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("Failed to receive the list of integrations. Going to retry immediately.", e);
            }
        }
    }

    @SneakyThrows
    @Override
    public Integration getIntegration(TenantId tenantId, IntegrationId integrationId) {
        var request = IntegrationRequestProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setIntegrationIdMSB(integrationId.getId().getMostSignificantBits())
                .setIntegrationIdLSB(integrationId.getId().getLeastSignificantBits())
                .build();
        var response =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationRequest(request).build()));
        return Futures.transform(response, this::parseIntegrationFromProto, callbackExecutor).get(1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    @Override
    public Integration getIntegration(TenantId tenantId, String routingKey) {
        var request = IntegrationRequestProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRoutingKey(routingKey)
                .build();
        var response =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationRequest(request).build()));
        return Futures.transform(response, this::parseIntegrationFromProto, callbackExecutor).get(1, TimeUnit.MINUTES);
    }

    @SneakyThrows
    @Override
    public Converter getConverter(TenantId tenantId, ConverterId converterId) {
        var request = ConverterRequestProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setConverterIdMSB(converterId.getId().getMostSignificantBits())
                .setConverterIdLSB(converterId.getId().getLeastSignificantBits())
                .build();
        var response =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setConverterRequest(request).build()));
        return Futures.transform(response, this::parseConverterFromProto, callbackExecutor).get(1, TimeUnit.MINUTES);
    }


    private List<IntegrationInfo> parseListFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<IntegrationInfo>();

        var response = proto.getValue().getIntegrationListResponse().getIntegrationInfoListList();

        for (var integrationInfoProto : response) {
            result.add(IntegrationProtoUtil.toInfo(integrationInfoProto));
        }

        return result;
    }

    private Integration parseIntegrationFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        ByteString data = proto.getValue().getIntegrationResponse().getData();
        if (!data.isEmpty()) {
            Optional<Integration> integration = dataDecodingEncodingService.decode(data.toByteArray());
            return integration.orElseThrow(() -> new RuntimeException("Can't parse the integration from bytes!"));
        } else {
            return null;
        }
    }

    private Converter parseConverterFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        ByteString data = proto.getValue().getConverterResponse().getData();
        Optional<Converter> converter = dataDecodingEncodingService.decode(data.toByteArray());
        return converter.orElseThrow(() -> new RuntimeException("Can't parse the converter from bytes!"));
    }

}
