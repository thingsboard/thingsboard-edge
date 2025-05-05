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
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.cache.CacheExecutorService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.integration.ConverterRequestProto;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationInfoListRequestProto;
import org.thingsboard.server.gen.integration.IntegrationInfoListResponseProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.IntegrationRequestProto;
import org.thingsboard.server.gen.integration.TenantProfileRequestProto;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbCoreIntegrationApiService implements TbCoreIntegrationApiService {

    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final StatsFactory statsFactory;
    private final IntegrationService integrationService;
    private final ConverterService converterService;
    private final TbTenantProfileCache tenantProfileCache;
    private final PlatformIntegrationService platformIntegrationService;
    private final SecretConfigurationService secretConfigurationService;
    private final CacheExecutorService cacheExecutorService;

    @Value("${queue.integration_api.max_pending_requests:10000}")
    private int maxPendingRequests;
    @Value("${queue.integration_api.max_requests_timeout:10000}")
    private long requestTimeout;
    @Value("${queue.integration_api.request_poll_interval:25}")
    private int responsePollDuration;
    @Value("${queue.integration_api.max_callback_threads:10}")
    private int maxCallbackThreads;

    private ExecutorService integrationCallbackExecutor;
    private TbQueueResponseTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>,
            TbProtoQueueMsg<IntegrationApiResponseMsg>> integrationApiTemplate;

    @PostConstruct
    public void init() {
        this.integrationCallbackExecutor = ThingsBoardExecutors.newWorkStealingPool(maxCallbackThreads, getClass());
        TbQueueProducer<TbProtoQueueMsg<IntegrationApiResponseMsg>> producer = tbCoreQueueFactory.createIntegrationApiResponseProducer();
        TbQueueConsumer<TbProtoQueueMsg<IntegrationApiRequestMsg>> consumer = tbCoreQueueFactory.createIntegrationApiRequestConsumer();

        String key = StatsType.INTEGRATION.getName();
        MessagesStats queueStats = statsFactory.createMessagesStats(key);

        DefaultTbQueueResponseTemplate.DefaultTbQueueResponseTemplateBuilder
                <TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> builder = DefaultTbQueueResponseTemplate.builder();
        builder.requestTemplate(consumer);
        builder.responseTemplate(producer);
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(integrationCallbackExecutor);
        builder.stats(queueStats);
        integrationApiTemplate = builder.build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        integrationApiTemplate.subscribe();
        integrationApiTemplate.launch(this);
    }

    @PreDestroy
    public void destroy() {
        if (integrationApiTemplate != null) {
            integrationApiTemplate.stop();
        }
        if (integrationCallbackExecutor != null) {
            integrationCallbackExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<TbProtoQueueMsg<IntegrationApiResponseMsg>> handle(TbProtoQueueMsg<IntegrationApiRequestMsg> tbProtoQueueMsg) {
        var integrationApiRequest = tbProtoQueueMsg.getValue();

        ListenableFuture<IntegrationApiResponseMsg> result = null;

        if (integrationApiRequest.hasIntegrationListRequest()) {
            result = handleListRequest(integrationApiRequest.getIntegrationListRequest());
        } else if (integrationApiRequest.hasIntegrationRequest()) {
            result = handleIntegrationRequest(integrationApiRequest.getIntegrationRequest());
        } else if (integrationApiRequest.hasConverterRequest()) {
            result = handleConverterRequest(integrationApiRequest.getConverterRequest());
        } else if (integrationApiRequest.hasTenantProfileRequest()) {
            result = handleTenantProfileRequest(integrationApiRequest.getTenantProfileRequest());
        } else {
            throw new RuntimeException("Not Implemented!");
        }

        return Futures.transform(result,
                value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()),
                MoreExecutors.directExecutor());
    }

    @Override
    public void handle(Collection<TbProtoQueueMsg<ToCoreIntegrationMsg>> msgs, TbCallback callback) {
        List<Pair<TbProtoQueueMsg<ToCoreIntegrationMsg>, ListenableFuture<Runnable>>> futures = new ArrayList<>(msgs.size());
        for (TbProtoQueueMsg<ToCoreIntegrationMsg> msg : msgs) {
            try {
                // TODO: ashvayka: improve the retry strategy.
                ListenableFuture<Runnable> future = cacheExecutorService.executeAsync(() -> this.handle(msg, TbCallback.EMPTY));
                futures.add(Pair.of(msg, future));
            } catch (Throwable e) {
                log.warn("Failed to process integration msg: {}", msg, e);
            }
        }
        for (var future : futures) {
            try {
                future.getSecond().get(20, TimeUnit.SECONDS).run();
            } catch (Throwable e) {
                log.warn("Failed to process integration msg: {}", future.getFirst(), e);
            }
        }

        callback.onSuccess();
    }

    Runnable handle(TbProtoQueueMsg<ToCoreIntegrationMsg> envelope, TbCallback callback) {
        var msg = envelope.getValue();
        if (msg.hasIntegration()) {
            IntegrationInfo info = ProtoUtils.fromProto(msg.getIntegration());
            if (msg.hasDeviceUplinkProto()) {
                return platformIntegrationService.processUplinkData(info, msg.getDeviceUplinkProto(), new IntegrationApiCallback(callback));
            } else if (msg.hasAssetUplinkProto()) {
                return platformIntegrationService.processUplinkData(info, msg.getAssetUplinkProto(), new IntegrationApiCallback(callback));
            } else if (msg.hasEntityViewDataProto()) {
                return platformIntegrationService.processUplinkData(info, msg.getEntityViewDataProto(), new IntegrationApiCallback(callback));
            } else if (!msg.getCustomTbMsg().isEmpty()) {
                return () -> platformIntegrationService.processUplinkData(info, TbMsg.fromBytes(null, msg.getCustomTbMsg().toByteArray(), TbMsgCallback.EMPTY), new IntegrationApiCallback(callback));
            } else {
                callback.onFailure(new RuntimeException("Empty or not supported ToCoreIntegrationMsg!"));
            }
        } else if (msg.hasEventProto()) {
            return () -> platformIntegrationService.processUplinkData(msg.getEventProto(), new IntegrationApiCallback(callback));
        } else if (msg.hasTsDataProto()) {
            return () -> platformIntegrationService.processUplinkData(msg.getTsDataProto(), new IntegrationApiCallback(callback));
        } else {
            callback.onFailure(new IllegalArgumentException("Unsupported integration msg!"));
        }
        return () -> {
        };
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleConverterRequest(ConverterRequestProto request) {
        var converterId = new ConverterId(new UUID(request.getConverterIdMSB(), request.getConverterIdLSB()));
        var tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
        var future = converterService.findConverterByIdAsync(tenantId, converterId);

        return Futures.transform(future, converter -> IntegrationApiResponseMsg.newBuilder()
                .setConverterResponse(ProtoUtils.toProto(converter)).build(), MoreExecutors.directExecutor());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleIntegrationRequest(IntegrationRequestProto request) {
        var tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
        ListenableFuture<Integration> future;
        if (request.getIntegrationIdMSB() != 0 || request.getIntegrationIdLSB() != 0) {
            var integrationId = new IntegrationId(new UUID(request.getIntegrationIdMSB(), request.getIntegrationIdLSB()));
            future = integrationService.findIntegrationByIdAsync(tenantId, integrationId);
        } else if (StringUtils.isNotEmpty(request.getRoutingKey())) {
            future = Futures.transform(Futures.immediateFuture(integrationService.findIntegrationByRoutingKey(tenantId, request.getRoutingKey())), opt -> opt.orElse(null), MoreExecutors.directExecutor());
        } else {
            future = Futures.immediateFailedFuture(new RuntimeException("Invalid request parameters!"));
        }

        return Futures.transform(future, integration -> {
            var builder = IntegrationApiResponseMsg.newBuilder();
            if (integration != null) {
                var config = secretConfigurationService.replaceSecretPlaceholders(tenantId, integration.getConfiguration());
                integration.setConfiguration(config);
                builder.setIntegrationResponse(ProtoUtils.toProto(integration));
            }
            return builder.build();
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleListRequest(IntegrationInfoListRequestProto request) {
        IntegrationType integrationType = IntegrationType.valueOf(request.getType());
        List<IntegrationInfo> data = integrationService.findAllCoreIntegrationInfos(integrationType, false, request.getEnabled());

        List<IntegrationInfoProto> integrationInfoList = data.stream().map(ProtoUtils::toProto).collect(Collectors.toList());

        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder().setIntegrationListResponse(
                IntegrationInfoListResponseProto.newBuilder().addAllIntegrationInfoList(integrationInfoList).build()
        ).build());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleTenantProfileRequest(TenantProfileRequestProto request) {
        TenantId tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                .setTenantProfileResponse(ProtoUtils.toProto(tenantProfile))
                .build());
    }

}
