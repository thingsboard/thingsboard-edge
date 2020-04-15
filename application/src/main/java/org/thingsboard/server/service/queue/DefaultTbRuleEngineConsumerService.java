/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.queue;

import akka.actor.ActorRef;
import com.google.protobuf.ProtocolStringList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueConfiguration;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    @Value("${queue.rule-engine.poll-interval}")
    private long pollDuration;
    @Value("${queue.rule-engine.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.rule-engine.stats.enabled:true}")
    private boolean statsEnabled;

    private final TbRuleEngineSubmitStrategyFactory submitStrategyFactory;
    private final TbRuleEngineProcessingStrategyFactory processingStrategyFactory;
    private final TbRuleEngineQueueFactory tbRuleEngineQueueFactory;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final RuleEngineStatisticsService statisticsService;
    private final ConcurrentMap<String, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbRuleEngineQueueConfiguration> consumerConfigurations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbRuleEngineConsumerStats> consumerStats = new ConcurrentHashMap<>();
    private ExecutorService submitExecutor;

    public DefaultTbRuleEngineConsumerService(TbRuleEngineProcessingStrategyFactory processingStrategyFactory,
                                              TbRuleEngineSubmitStrategyFactory submitStrategyFactory,
                                              TbQueueRuleEngineSettings ruleEngineSettings,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory, RuleEngineStatisticsService statisticsService,
                                              ActorSystemContext actorContext, DataDecodingEncodingService encodingService) {
        super(actorContext, encodingService, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer());
        this.statisticsService = statisticsService;
        this.ruleEngineSettings = ruleEngineSettings;
        this.tbRuleEngineQueueFactory = tbRuleEngineQueueFactory;
        this.submitStrategyFactory = submitStrategyFactory;
        this.processingStrategyFactory = processingStrategyFactory;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-consumer", "tb-rule-engine-notifications-consumer");
        for (TbRuleEngineQueueConfiguration configuration : ruleEngineSettings.getQueues()) {
            consumerConfigurations.putIfAbsent(configuration.getName(), configuration);
            consumers.computeIfAbsent(configuration.getName(), queueName -> tbRuleEngineQueueFactory.createToRuleEngineMsgConsumer(configuration));
            consumerStats.put(configuration.getName(), new TbRuleEngineConsumerStats(configuration.getName()));
        }
        submitExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void stop() {
        if (submitExecutor != null) {
            submitExecutor.shutdownNow();
        }

        ruleEngineSettings.getQueues().forEach(config -> consumerConfigurations.put(config.getName(), config));
    }

    @Override
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (partitionChangeEvent.getServiceType().equals(getServiceType())) {
            ServiceQueue serviceQueue = partitionChangeEvent.getServiceQueueKey().getServiceQueue();
            log.info("[{}] Subscribing to partitions: {}", serviceQueue.getQueue(), partitionChangeEvent.getPartitions());
            consumers.get(serviceQueue.getQueue()).subscribe(partitionChangeEvent.getPartitions());
        }
    }

    @Override
    protected void launchMainConsumers() {
        consumers.forEach((queue, consumer) -> launchConsumer(consumer, consumerConfigurations.get(queue), consumerStats.get(queue)));
    }

    @Override
    protected void stopMainConsumers() {
        consumers.values().forEach(TbQueueConsumer::unsubscribe);
    }

    private void launchConsumer(TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, TbRuleEngineQueueConfiguration configuration, TbRuleEngineConsumerStats stats) {
        consumersExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    TbRuleEngineSubmitStrategy submitStrategy = submitStrategyFactory.newInstance(configuration.getName(), configuration.getSubmitStrategy());
                    TbRuleEngineProcessingStrategy ackStrategy = processingStrategyFactory.newInstance(configuration.getName(), configuration.getProcessingStrategy());

                    submitStrategy.init(msgs);

                    while (!stopped) {
                        ProcessingAttemptContext ctx = new ProcessingAttemptContext(submitStrategy);
                        submitStrategy.submitAttempt((id, msg) -> submitExecutor.submit(() -> {
                            log.trace("[{}] Creating callback for message: {}", id, msg.getValue());
                            ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
                            TenantId tenantId = new TenantId(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
                            TbMsgCallback callback = new TbMsgPackCallback(id, tenantId, ctx);
                            try {
                                if (toRuleEngineMsg.getTbMsg() != null && !toRuleEngineMsg.getTbMsg().isEmpty()) {
                                    forwardToRuleEngineActor(tenantId, toRuleEngineMsg, callback);
                                } else {
                                    callback.onSuccess();
                                }
                            } catch (Exception e) {
                                callback.onFailure(new RuleEngineException(e.getMessage()));
                            }
                        }));

                        boolean timeout = false;
                        if (!ctx.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                            timeout = true;
                        }

                        TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(timeout, ctx);
                        TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
                        if (statsEnabled) {
                            stats.log(result, decision.isCommit());
                        }
                        if (decision.isCommit()) {
                            submitStrategy.stop();
                            break;
                        } else {
                            submitStrategy.update(decision.getReprocessMap());
                        }
                    }
                    consumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to process messages from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
            log.info("TB Rule Engine Consumer stopped.");
        });
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollDuration;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) throws Exception {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.getComponentLifecycleMsg() != null && !nfMsg.getComponentLifecycleMsg().isEmpty()) {
            Optional<TbActorMsg> actorMsg = encodingService.decode(nfMsg.getComponentLifecycleMsg().toByteArray());
            if (actorMsg.isPresent()) {
                log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
                actorContext.tell(actorMsg.get(), ActorRef.noSender());
            }
            callback.onSuccess();
        } else {
            callback.onSuccess();
        }
    }

    private void forwardToRuleEngineActor(TenantId tenantId, ToRuleEngineMsg toRuleEngineMsg, TbMsgCallback callback) {
        TbMsg tbMsg = TbMsg.fromBytes(toRuleEngineMsg.getTbMsg().toByteArray(), callback);
        QueueToRuleEngineMsg msg;
        ProtocolStringList relationTypesList = toRuleEngineMsg.getRelationTypesList();
        Set<String> relationTypes = null;
        if (relationTypesList != null) {
            if (relationTypesList.size() == 1) {
                relationTypes = Collections.singleton(relationTypesList.get(0));
            } else {
                relationTypes = new HashSet<>(relationTypesList);
            }
        }
        msg = new QueueToRuleEngineMsg(tenantId, tbMsg, relationTypes, toRuleEngineMsg.getFailureMessage());
        actorContext.tell(msg, ActorRef.noSender());
    }

    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            long ts = System.currentTimeMillis();
            consumerStats.forEach((queue, stats) -> {
                stats.printStats();
                statisticsService.reportQueueStats(ts, stats);
            });
        }
    }

}
