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
package org.thingsboard.server.service.housekeeper;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.housekeeper.HouseKeeperService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryHouseKeeperServiceService implements HouseKeeperService {

    @Lazy
    final TbAlarmService alarmService;

    ListeningExecutorService executor;

    AtomicInteger queueSize = new AtomicInteger();
    AtomicInteger totalProcessedCounter = new AtomicInteger();

    @PostConstruct
    public void init() {
        log.debug("Starting HouseKeeper service");
        executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper")));
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            log.debug("Stopping HouseKeeper service");
            executor.shutdown();
        }
    }

    @Override
    public ListenableFuture<List<AlarmId>> unassignDeletedUserAlarms(TenantId tenantId, User user) {
        log.debug("[{}][{}] unassignDeletedUserAlarms submitting, pending queue size: {} ", tenantId, user.getId().getId(), queueSize.get());
        queueSize.incrementAndGet();
        ListenableFuture<List<AlarmId>> future = executor.submit(() -> alarmService.unassignDeletedUserAlarms(tenantId, user));
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(List<AlarmId> alarmIds) {
                queueSize.decrementAndGet();
                totalProcessedCounter.incrementAndGet();
                log.debug("[{}][{}] unassignDeletedUserAlarms finished, pending queue size: {}, total processed count: {} ",
                        tenantId, user.getId().getId(), queueSize.get(), totalProcessedCounter.get());
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                queueSize.decrementAndGet();
                totalProcessedCounter.incrementAndGet();
                log.error("[{}][{}] unassignDeletedUserAlarms failed, pending queue size: {}, total processed count: {}",
                        tenantId, user.getId().getId(), queueSize.get(), totalProcessedCounter.get(), throwable);
            }
        }, MoreExecutors.directExecutor());
        return future;
    }

}
