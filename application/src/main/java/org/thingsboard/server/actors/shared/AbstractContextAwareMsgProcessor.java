/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.actors.shared;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Scheduler;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.actors.ActorSystemContext;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public abstract class AbstractContextAwareMsgProcessor {

    protected final ActorSystemContext systemContext;
    protected final LoggingAdapter logger;
    protected final ObjectMapper mapper = new ObjectMapper();

    protected AbstractContextAwareMsgProcessor(ActorSystemContext systemContext, LoggingAdapter logger) {
        super();
        this.systemContext = systemContext;
        this.logger = logger;
    }

    protected ActorRef getAppActor() {
        return systemContext.getAppActor();
    }

    protected Scheduler getScheduler() {
        return systemContext.getScheduler();
    }

    protected ExecutionContextExecutor getSystemDispatcher() {
        return systemContext.getActorSystem().dispatcher();
    }

    protected void schedulePeriodicMsgWithDelay(ActorContext ctx, Object msg, long delayInMs, long periodInMs) {
        schedulePeriodicMsgWithDelay(ctx, msg, delayInMs, periodInMs, ctx.self());
    }

    protected void schedulePeriodicMsgWithDelay(ActorContext ctx, Object msg, long delayInMs, long periodInMs, ActorRef target) {
        logger.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        getScheduler().schedule(Duration.create(delayInMs, TimeUnit.MILLISECONDS), Duration.create(periodInMs, TimeUnit.MILLISECONDS), target, msg, getSystemDispatcher(), null);
    }


    protected void scheduleMsgWithDelay(ActorContext ctx, Object msg, long delayInMs) {
        scheduleMsgWithDelay(ctx, msg, delayInMs, ctx.self());
    }

    protected void scheduleMsgWithDelay(ActorContext ctx, Object msg, long delayInMs, ActorRef target) {
        logger.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        getScheduler().scheduleOnce(Duration.create(delayInMs, TimeUnit.MILLISECONDS), target, msg, getSystemDispatcher(), null);
    }

    @Data
    @AllArgsConstructor
    private static class ComponentConfiguration {
        private final String clazz;
        private final String name;
        private final String configuration;
    }

}
