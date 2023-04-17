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
package org.thingsboard.server.service.notification;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class DefaultNotificationSchedulerService extends AbstractPartitionBasedService<NotificationRequestId> implements NotificationSchedulerService {

    private final NotificationCenter notificationCenter;
    private final NotificationRequestService notificationRequestService;
    private final SchedulerComponent scheduler;
    private final NotificationExecutorService notificationExecutor;

    private final Map<NotificationRequestId, ScheduledRequestMetadata> scheduledNotificationRequests = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        PageDataIterable<NotificationRequest> notificationRequests = new PageDataIterable<>(pageLink -> {
            return notificationRequestService.findScheduledNotificationRequests(pageLink);
        }, 1000);
        for (NotificationRequest notificationRequest : notificationRequests) {
            TopicPartitionInfo requestPartition = partitionService.resolve(ServiceType.TB_CORE, notificationRequest.getTenantId(), notificationRequest.getId());
            if (addedPartitions.contains(requestPartition)) {
                partitionedEntities.computeIfAbsent(requestPartition, k -> ConcurrentHashMap.newKeySet()).add(notificationRequest.getId());
                if (!scheduledNotificationRequests.containsKey(notificationRequest.getId())) {
                    scheduleNotificationRequest(notificationRequest.getTenantId(), notificationRequest, notificationRequest.getCreatedTime());
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public void scheduleNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId, long requestTs) {
        NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, notificationRequestId);
        scheduleNotificationRequest(tenantId, notificationRequest, requestTs);
    }

    private void scheduleNotificationRequest(TenantId tenantId, NotificationRequest request, long requestTs) {
        int delayInSec = Optional.ofNullable(request)
                .map(NotificationRequest::getAdditionalConfig)
                .map(NotificationRequestConfig::getSendingDelayInSec)
                .orElse(0);
        if (delayInSec <= 0) return;
        long delayInMs = TimeUnit.SECONDS.toMillis(delayInSec) - (System.currentTimeMillis() - requestTs);
        if (delayInMs < 0) {
            delayInMs = 0;
        }

        ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
            NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, request.getId());
            if (notificationRequest == null) return;

            notificationExecutor.executeAsync(() -> {
                try {
                    notificationCenter.processNotificationRequest(tenantId, notificationRequest, null);
                } catch (Exception e) {
                    log.error("Failed to process scheduled notification request {}", notificationRequest.getId(), e);
                    NotificationRequestStats stats = new NotificationRequestStats();
                    stats.setError(e.getMessage());
                    notificationRequestService.updateNotificationRequest(tenantId, request.getId(), NotificationRequestStatus.SENT, stats);
                }
            });
            scheduledNotificationRequests.remove(notificationRequest.getId());
        }, delayInMs, TimeUnit.MILLISECONDS);
        scheduledNotificationRequests.put(request.getId(), new ScheduledRequestMetadata(tenantId, scheduledTask));
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
            EntityId entityId = event.getEntityId();
            switch (entityId.getEntityType()) {
                case NOTIFICATION_REQUEST:
                    cancelAndRemove((NotificationRequestId) entityId);
                    break;
                case TENANT:
                    Set<NotificationRequestId> toCancel = new HashSet<>();
                    scheduledNotificationRequests.forEach((notificationRequestId, scheduledRequestMetadata) -> {
                        if (scheduledRequestMetadata.getTenantId().equals(entityId)) {
                            toCancel.add(notificationRequestId);
                        }
                    });
                    toCancel.forEach(this::cancelAndRemove);
                    break;
            }
        }
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(NotificationRequestId notificationRequestId) {
        cancelAndRemove(notificationRequestId);
    }

    private void cancelAndRemove(NotificationRequestId notificationRequestId) {
        ScheduledRequestMetadata md = scheduledNotificationRequests.remove(notificationRequestId);
        if (md != null) {
            md.getFuture().cancel(false);
        }
    }

    @Override
    protected String getServiceName() {
        return "Notifications scheduler";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "notifications-scheduler";
    }

    @Data
    private static class ScheduledRequestMetadata {
        private final TenantId tenantId;
        private final ScheduledFuture<?> future;
    }

}
