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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceStateServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.SchedulerServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TbCoreConsumerStats {
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String SESSION_EVENTS = "sessionEvents";
    public static final String GET_ATTRIBUTE = "getAttr";
    public static final String ATTRIBUTE_SUBSCRIBES = "subToAttr";
    public static final String RPC_SUBSCRIBES = "subToRpc";
    public static final String TO_DEVICE_RPC_CALL_RESPONSES = "toDevRpc";
    public static final String SUBSCRIPTION_INFO = "subInfo";
    public static final String DEVICE_CLAIMS = "claimDevice";
    public static final String DEVICE_STATES = "deviceState";
    public static final String SUBSCRIPTION_MSGS = "subMsgs";
    public static final String TO_CORE_NOTIFICATIONS = "coreNfs";
    public static final String SCHEDULER = "scheduler";
    public static final String EDGE_NOTIFICATIONS = "edgeNfs";
    public static final String CLOUD_NOTIFICATIONS = "cloudNfs";
    public static final String DEVICE_ACTIVITIES = "deviceActivity";

    private final StatsCounter totalCounter;
    private final StatsCounter sessionEventCounter;
    private final StatsCounter getAttributesCounter;
    private final StatsCounter subscribeToAttributesCounter;
    private final StatsCounter subscribeToRPCCounter;
    private final StatsCounter toDeviceRPCCallResponseCounter;
    private final StatsCounter subscriptionInfoCounter;
    private final StatsCounter claimDeviceCounter;

    private final StatsCounter schedulerMsgCounter;
    private final StatsCounter deviceStateCounter;
    private final StatsCounter subscriptionMsgCounter;
    private final StatsCounter toCoreNotificationsCounter;
    private final StatsCounter edgeNotificationsCounter;
    private final StatsCounter cloudNotificationMsgCounter;
    private final StatsCounter deviceActivitiesCounter;

    private final List<StatsCounter> counters = new ArrayList<>();

    public TbCoreConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.CORE.getName();

        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.sessionEventCounter = register(statsFactory.createStatsCounter(statsKey, SESSION_EVENTS));
        this.getAttributesCounter = register(statsFactory.createStatsCounter(statsKey, GET_ATTRIBUTE));
        this.subscribeToAttributesCounter = register(statsFactory.createStatsCounter(statsKey, ATTRIBUTE_SUBSCRIBES));
        this.subscribeToRPCCounter = register(statsFactory.createStatsCounter(statsKey, RPC_SUBSCRIBES));
        this.toDeviceRPCCallResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_DEVICE_RPC_CALL_RESPONSES));
        this.subscriptionInfoCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_INFO));
        this.claimDeviceCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_CLAIMS));
        this.deviceStateCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_STATES));
        this.subscriptionMsgCounter = register(statsFactory.createStatsCounter(statsKey, SUBSCRIPTION_MSGS));
        this.toCoreNotificationsCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NOTIFICATIONS));
        this.schedulerMsgCounter = register(statsFactory.createStatsCounter(statsKey, SCHEDULER));
        this.edgeNotificationsCounter = register(statsFactory.createStatsCounter(statsKey, EDGE_NOTIFICATIONS));
        this.cloudNotificationMsgCounter = register(statsFactory.createStatsCounter(statsKey, CLOUD_NOTIFICATIONS));
        this.deviceActivitiesCounter = register(statsFactory.createStatsCounter(statsKey, DEVICE_ACTIVITIES));
    }

    private StatsCounter register(StatsCounter counter){
        counters.add(counter);
        return counter;
    }

    public void log(TransportToDeviceActorMsg msg) {
        totalCounter.increment();
        if (msg.hasSessionEvent()) {
            sessionEventCounter.increment();
        }
        if (msg.hasGetAttributes()) {
            getAttributesCounter.increment();
        }
        if (msg.hasSubscribeToAttributes()) {
            subscribeToAttributesCounter.increment();
        }
        if (msg.hasSubscribeToRPC()) {
            subscribeToRPCCounter.increment();
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            toDeviceRPCCallResponseCounter.increment();
        }
        if (msg.hasSubscriptionInfo()) {
            subscriptionInfoCounter.increment();
        }
        if (msg.hasClaimDevice()) {
            claimDeviceCounter.increment();
        }
    }

    public void log(DeviceStateServiceMsgProto msg) {
        totalCounter.increment();
        deviceStateCounter.increment();
    }

    public void log(SubscriptionMgrMsgProto msg) {
        totalCounter.increment();
        subscriptionMsgCounter.increment();
    }

    public void log(SchedulerServiceMsgProto schedulerServiceMsg) {
        totalCounter.increment();
        schedulerMsgCounter.increment();
    }

    public void log(TransportProtos.EdgeNotificationMsgProto msg) {
        totalCounter.increment();
        edgeNotificationsCounter.increment();
    }

    public void log(TransportProtos.DeviceActivityProto msg) {
        totalCounter.increment();
        deviceActivitiesCounter.increment();
    }

    public void log(TransportProtos.CloudNotificationMsgProto msg) {
        totalCounter.increment();
        cloudNotificationMsgCounter.increment();
    }

    public void logToCoreNotification() {
        totalCounter.increment();
        toCoreNotificationsCounter.increment();
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> stats.append(counter.getName()).append(" = [").append(counter.get()).append("] "));
            log.info("Core Stats: {}", stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}
