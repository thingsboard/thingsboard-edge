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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.stats.StatsCounter;
import org.thingsboard.server.service.stats.StatsCounterFactory;
import org.thingsboard.server.service.stats.StatsType;

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
    public static final String SCHEDULER_NOTIFICATIONS = "scheduler";


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

    private final List<StatsCounter> counters = new ArrayList<>();

    public TbCoreConsumerStats(StatsCounterFactory counterFactory) {
        String statsKey = StatsType.CORE.getName();

        this.totalCounter = counterFactory.createStatsCounter(statsKey, TOTAL_MSGS);
        this.sessionEventCounter = counterFactory.createStatsCounter(statsKey, SESSION_EVENTS);
        this.getAttributesCounter = counterFactory.createStatsCounter(statsKey, GET_ATTRIBUTE);
        this.subscribeToAttributesCounter = counterFactory.createStatsCounter(statsKey, ATTRIBUTE_SUBSCRIBES);
        this.subscribeToRPCCounter = counterFactory.createStatsCounter(statsKey, RPC_SUBSCRIBES);
        this.toDeviceRPCCallResponseCounter = counterFactory.createStatsCounter(statsKey, TO_DEVICE_RPC_CALL_RESPONSES);
        this.subscriptionInfoCounter = counterFactory.createStatsCounter(statsKey, SUBSCRIPTION_INFO);
        this.claimDeviceCounter = counterFactory.createStatsCounter(statsKey, DEVICE_CLAIMS);
        this.deviceStateCounter = counterFactory.createStatsCounter(statsKey, DEVICE_STATES);
        this.subscriptionMsgCounter = counterFactory.createStatsCounter(statsKey, SUBSCRIPTION_MSGS);
        this.toCoreNotificationsCounter = counterFactory.createStatsCounter(statsKey, TO_CORE_NOTIFICATIONS);
        this.schedulerMsgCounter = counterFactory.createStatsCounter(statsKey, SCHEDULER_NOTIFICATIONS);

        counters.add(totalCounter);
        counters.add(sessionEventCounter);
        counters.add(getAttributesCounter);
        counters.add(subscribeToAttributesCounter);
        counters.add(subscribeToRPCCounter);
        counters.add(toDeviceRPCCallResponseCounter);
        counters.add(subscriptionInfoCounter);
        counters.add(claimDeviceCounter);

        counters.add(deviceStateCounter);
        counters.add(subscriptionMsgCounter);
        counters.add(toCoreNotificationsCounter);
    }

    public void log(TransportProtos.TransportToDeviceActorMsg msg) {
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

    public void log(TransportProtos.DeviceStateServiceMsgProto msg) {
        totalCounter.increment();
        deviceStateCounter.increment();
    }

    public void log(TransportProtos.SubscriptionMgrMsgProto msg) {
        totalCounter.increment();
        subscriptionMsgCounter.increment();
    }

    public void log(TransportProtos.ToCoreNotificationMsg msg) {
        totalCounter.increment();
        toCoreNotificationsCounter.increment();
    }

    public void log(TransportProtos.SchedulerServiceMsgProto msg) {
        totalCounter.increment();
        schedulerMsgCounter.increment();
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
