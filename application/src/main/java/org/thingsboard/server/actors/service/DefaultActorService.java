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
package org.thingsboard.server.actors.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.app.AppActor;
import org.thingsboard.server.actors.app.AppInitMsg;
import org.thingsboard.server.actors.stats.StatsActor;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DefaultActorService implements ActorService {

    private static final String ACTOR_SYSTEM_NAME = "Akka";

    public static final String APP_DISPATCHER_NAME = "app-dispatcher";
    public static final String CORE_DISPATCHER_NAME = "core-dispatcher";
    public static final String SYSTEM_RULE_DISPATCHER_NAME = "system-rule-dispatcher";
    public static final String TENANT_RULE_DISPATCHER_NAME = "rule-dispatcher";

    @Autowired
    private ActorSystemContext actorContext;

    private ActorSystem system;

    private ActorRef appActor;

    @PostConstruct
    public void initActorSystem() {
        log.info("Initializing Actor system.");
        actorContext.setActorService(this);
        system = ActorSystem.create(ACTOR_SYSTEM_NAME, actorContext.getConfig());
        actorContext.setActorSystem(system);

        appActor = system.actorOf(Props.create(new AppActor.ActorCreator(actorContext)).withDispatcher(APP_DISPATCHER_NAME), "appActor");
        actorContext.setAppActor(appActor);

        ActorRef statsActor = system.actorOf(Props.create(new StatsActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME), "statsActor");
        actorContext.setStatsActor(statsActor);

        log.info("Actor system initialized.");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Sending application init message to actor system");
        appActor.tell(new AppInitMsg(), ActorRef.noSender());
    }

    @EventListener(PartitionChangeEvent.class)
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        log.info("Received partition change event.");
        this.appActor.tell(new PartitionChangeMsg(partitionChangeEvent.getServiceQueueKey(), partitionChangeEvent.getPartitions()), ActorRef.noSender());
    }

    @PreDestroy
    public void stopActorSystem() {
        Future<Terminated> status = system.terminate();
        try {
            Terminated terminated = Await.result(status, Duration.Inf());
            log.info("Actor system terminated: {}", terminated);
        } catch (Exception e) {
            log.error("Failed to terminate actor system.", e);
        }
    }

    //TODO 2.5: ashvayka
//    @Override
//    public void onReceivedMsg(ServerAddress source, ClusterAPIProtos.ClusterMessage msg) {
//        if (statsEnabled) {
//            receivedClusterMsgs.incrementAndGet();
//        }
//        ServerAddress serverAddress = new ServerAddress(source.getHost(), source.getPort(), source.getServerType());
//        if (log.isDebugEnabled()) {
//            log.info("Received msg [{}] from [{}]", msg.getMessageType().name(), serverAddress);
//            log.info("MSG: {}", msg);
//        }
//        switch (msg.getMessageType()) {
//            case CLUSTER_ACTOR_MESSAGE:
//                java.util.Optional<TbActorMsg> decodedMsg = actorContext.getEncodingService()
//                        .decode(msg.getPayload().toByteArray());
//                if (decodedMsg.isPresent()) {
//                    appActor.tell(decodedMsg.get(), ActorRef.noSender());
//                } else {
//                    log.error("Error during decoding cluster proto message");
//                }
//                break;
//            case TO_ALL_NODES_MSG:
//                //TODO
//                break;
//            case CLUSTER_TELEMETRY_SUBSCRIPTION_CREATE_MESSAGE:
//                actorContext.getTsSubService().onNewRemoteSubscription(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_TELEMETRY_SUBSCRIPTION_UPDATE_MESSAGE:
//                actorContext.getTsSubService().onRemoteSubscriptionUpdate(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_TELEMETRY_SUBSCRIPTION_CLOSE_MESSAGE:
//                actorContext.getTsSubService().onRemoteSubscriptionClose(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_TELEMETRY_SESSION_CLOSE_MESSAGE:
//                actorContext.getTsSubService().onRemoteSessionClose(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_TELEMETRY_ATTR_UPDATE_MESSAGE:
//                actorContext.getTsSubService().onRemoteAttributesUpdate(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_TELEMETRY_TS_UPDATE_MESSAGE:
//                actorContext.getTsSubService().onRemoteTsUpdate(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_RPC_FROM_DEVICE_RESPONSE_MESSAGE:
//                actorContext.getDeviceRpcService().processResponseToServerSideRPCRequestFromRemoteServer(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_DEVICE_STATE_SERVICE_MESSAGE:
//                actorContext.getDeviceStateService().onRemoteMsg(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_SCHEDULER_SERVICE_MESSAGE:
//                actorContext.getSchedulerService().onRemoteMsg(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_INTEGRATION_DOWNLINK_MESSAGE:
//                actorContext.getPlatformIntegrationService().onRemoteDownlinkMsg(serverAddress, msg.getPayload().toByteArray());
//                break;
//            case CLUSTER_TRANSACTION_SERVICE_MESSAGE:
//                actorContext.getRuleChainTransactionService().onRemoteTransactionMsg(serverAddress, msg.getPayload().toByteArray());
//                break;
//        }
//    }

}
