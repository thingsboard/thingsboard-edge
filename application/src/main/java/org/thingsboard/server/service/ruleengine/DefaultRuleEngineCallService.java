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
package org.thingsboard.server.service.ruleengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultRuleEngineCallService implements RuleEngineCallService {

    private final TbClusterService clusterService;

    private ScheduledExecutorService rpcCallBackExecutor;

    private final ConcurrentMap<UUID, Consumer<TbMsg>> requests = new ConcurrentHashMap<>();

    public DefaultRuleEngineCallService(TbClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @PostConstruct
    public void initExecutor() {
        rpcCallBackExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("re-rpc-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (rpcCallBackExecutor != null) {
            rpcCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void processRestAPICallToRuleEngine(TenantId tenantId, UUID requestId, TbMsg request, boolean useQueueFromTbMsg, Consumer<TbMsg> consumer) {
        log.trace("[{}] Processing REST API call to rule engine: [{}] for entity: [{}]", tenantId, requestId, request.getOriginator());
        requests.put(requestId, consumer);
        sendRequestToRuleEngine(tenantId, request, useQueueFromTbMsg);
        scheduleTimeout(request, requestId, requests);
    }

    @Override
    public void onQueueMsg(TransportProtos.RestApiCallResponseMsgProto restApiCallResponseMsg, TbCallback callback) {
        UUID requestId = new UUID(restApiCallResponseMsg.getRequestIdMSB(), restApiCallResponseMsg.getRequestIdLSB());
        Consumer<TbMsg> consumer = requests.remove(requestId);
        if (consumer != null) {
            consumer.accept(TbMsg.fromBytes(null, restApiCallResponseMsg.getResponse().toByteArray(), TbMsgCallback.EMPTY));
        } else {
            log.trace("[{}] Unknown or stale rest api call response received", requestId);
        }
        callback.onSuccess();
    }

    private void sendRequestToRuleEngine(TenantId tenantId, TbMsg msg, boolean useQueueFromTbMsg) {
        clusterService.pushMsgToRuleEngine(tenantId, msg.getOriginator(), msg, useQueueFromTbMsg, null);
    }

    private void scheduleTimeout(TbMsg request, UUID requestId, ConcurrentMap<UUID, Consumer<TbMsg>> requestsMap) {
        long expirationTime = Long.parseLong(request.getMetaData().getValue("expirationTime"));
        long timeout = Math.max(0, expirationTime - System.currentTimeMillis());
        log.trace("[{}] processing the request: [{}]", this.hashCode(), requestId);
        rpcCallBackExecutor.schedule(() -> {
            Consumer<TbMsg> consumer = requestsMap.remove(requestId);
            if (consumer != null) {
                log.trace("[{}] request timeout detected: [{}]", this.hashCode(), requestId);
                consumer.accept(null);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }
}
