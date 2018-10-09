/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaRequestTemplate;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.service.cluster.discovery.DiscoveryService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "remote", matchIfMissing = true)
@Service
public class RemoteJsInvokeService extends AbstractJsInvokeService {

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private TbKafkaSettings kafkaSettings;

    @Value("${js.remote.request_topic}")
    private String requestTopic;

    @Value("${js.remote.response_topic_prefix}")
    private String responseTopicPrefix;

    @Value("${js.remote.max_pending_requests}")
    private long maxPendingRequests;

    @Value("${js.remote.max_requests_timeout}")
    private long maxRequestsTimeout;

    @Value("${js.remote.response_poll_interval}")
    private int responsePollDuration;

    @Value("${js.remote.response_auto_commit_interval}")
    private int autoCommitInterval;

    @Getter
    @Value("${js.remote.max_errors}")
    private int maxErrors;

    private TbKafkaRequestTemplate<JsInvokeProtos.RemoteJsRequest, JsInvokeProtos.RemoteJsResponse> kafkaTemplate;
    protected Map<UUID, String> scriptIdToBodysMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<JsInvokeProtos.RemoteJsRequest> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.defaultTopic(requestTopic);
        requestBuilder.encoder(new RemoteJsRequestEncoder());
        requestBuilder.enricher((request, responseTopic, requestId) -> {
            JsInvokeProtos.RemoteJsRequest.Builder remoteRequest = JsInvokeProtos.RemoteJsRequest.newBuilder();
            if (request.hasCompileRequest()) {
                remoteRequest.setCompileRequest(request.getCompileRequest());
            }
            if (request.hasInvokeRequest()) {
                remoteRequest.setInvokeRequest(request.getInvokeRequest());
            }
            if (request.hasReleaseRequest()) {
                remoteRequest.setReleaseRequest(request.getReleaseRequest());
            }
            remoteRequest.setResponseTopic(responseTopic);
            remoteRequest.setRequestIdMSB(requestId.getMostSignificantBits());
            remoteRequest.setRequestIdLSB(requestId.getLeastSignificantBits());
            return remoteRequest.build();
        });

        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<JsInvokeProtos.RemoteJsResponse> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(responseTopicPrefix + "." + discoveryService.getNodeId());
        responseBuilder.clientId(discoveryService.getNodeId());
        responseBuilder.groupId("rule-engine-node");
        responseBuilder.autoCommit(true);
        responseBuilder.autoCommitIntervalMs(autoCommitInterval);
        responseBuilder.decoder(new RemoteJsResponseDecoder());
        responseBuilder.requestIdExtractor((response) -> {
            return new UUID(response.getRequestIdMSB(), response.getRequestIdLSB());
        });

        TbKafkaRequestTemplate.TbKafkaRequestTemplateBuilder
                <JsInvokeProtos.RemoteJsRequest, JsInvokeProtos.RemoteJsResponse> builder = TbKafkaRequestTemplate.builder();
        builder.requestTemplate(requestBuilder.build());
        builder.responseTemplate(responseBuilder.build());
        builder.maxPendingRequests(maxPendingRequests);
        builder.maxRequestTimeout(maxRequestsTimeout);
        builder.pollInterval(responsePollDuration);
        kafkaTemplate = builder.build();
        kafkaTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        if (kafkaTemplate != null) {
            kafkaTemplate.stop();
        }
    }

    @Override
    protected boolean isLocal() {
        return false;
    }

    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, String functionName, String scriptBody) {
        JsInvokeProtos.JsCompileRequest jsRequest = JsInvokeProtos.JsCompileRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName)
                .setScriptBody(scriptBody).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setCompileRequest(jsRequest)
                .build();

        ListenableFuture<JsInvokeProtos.RemoteJsResponse> future = kafkaTemplate.post(scriptId.toString(), jsRequestWrapper);
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsCompileResponse compilationResult = response.getCompileResponse();
            UUID compiledScriptId = new UUID(compilationResult.getScriptIdMSB(), compilationResult.getScriptIdLSB());
            if (compilationResult.getSuccess()) {
                scriptIdToNameMap.put(scriptId, functionName);
                scriptIdToBodysMap.put(scriptId, scriptBody);
                return compiledScriptId;
            } else {
                log.debug("[{}] Failed to compile script due to [{}]: {}", compiledScriptId, compilationResult.getErrorCode().name(), compilationResult.getErrorDetails());
                throw new RuntimeException(compilationResult.getErrorDetails());
            }
        });
    }

    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, String functionName, Object[] args) {
        String scriptBody = scriptIdToBodysMap.get(scriptId);
        if (scriptBody == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No script body found for scriptId: [" + scriptId + "]!"));
        }
        JsInvokeProtos.JsInvokeRequest.Builder jsRequestBuilder = JsInvokeProtos.JsInvokeRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName)
                .setTimeout((int) maxRequestsTimeout)
                .setScriptBody(scriptIdToBodysMap.get(scriptId));

        for (int i = 0; i < args.length; i++) {
            jsRequestBuilder.addArgs(args[i].toString());
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setInvokeRequest(jsRequestBuilder.build())
                .build();

        ListenableFuture<JsInvokeProtos.RemoteJsResponse> future = kafkaTemplate.post(scriptId.toString(), jsRequestWrapper);
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsInvokeResponse invokeResult = response.getInvokeResponse();
            if (invokeResult.getSuccess()) {
                return invokeResult.getResult();
            } else {
                log.debug("[{}] Failed to compile script due to [{}]: {}", scriptId, invokeResult.getErrorCode().name(), invokeResult.getErrorDetails());
                throw new RuntimeException(invokeResult.getErrorDetails());
            }
        });
    }

    @Override
    protected void doRelease(UUID scriptId, String functionName) throws Exception {
        JsInvokeProtos.JsReleaseRequest jsRequest = JsInvokeProtos.JsReleaseRequest.newBuilder()
                .setScriptIdMSB(scriptId.getMostSignificantBits())
                .setScriptIdLSB(scriptId.getLeastSignificantBits())
                .setFunctionName(functionName).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setReleaseRequest(jsRequest)
                .build();

        ListenableFuture<JsInvokeProtos.RemoteJsResponse> future = kafkaTemplate.post(scriptId.toString(), jsRequestWrapper);
        JsInvokeProtos.RemoteJsResponse response = future.get();

        JsInvokeProtos.JsReleaseResponse compilationResult = response.getReleaseResponse();
        UUID compiledScriptId = new UUID(compilationResult.getScriptIdMSB(), compilationResult.getScriptIdLSB());
        if (compilationResult.getSuccess()) {
            scriptIdToBodysMap.remove(scriptId);
        } else {
            log.debug("[{}] Failed to release script due", compiledScriptId);
        }
    }

}
