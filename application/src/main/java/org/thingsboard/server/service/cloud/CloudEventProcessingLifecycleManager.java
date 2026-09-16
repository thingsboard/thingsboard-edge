// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.eventsourcing.GrpcConnectionEstablishedEvent;
import org.thingsboard.server.dao.eventsourcing.StopCloudEventProcessingEvent;
import org.thingsboard.server.service.cloud.event.runner.CloudEventUplinkProcessingRunner;
import org.thingsboard.server.service.cloud.event.sender.CloudEventUplinkSender;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CloudEventProcessingLifecycleManager {

    private final CloudEventUplinkProcessingRunner cloudEventUplinkProcessingRunner;
    private final List<CloudEventUplinkSender> cloudEventUplinkSenders;

    @EventListener(GrpcConnectionEstablishedEvent.class)
    public void handleConnectionEvent() {
        cloudEventUplinkSenders.forEach(CloudEventUplinkSender::init);
        cloudEventUplinkProcessingRunner.init();
    }

    @EventListener(StopCloudEventProcessingEvent.class)
    public void handleStopEvent() {
        cloudEventUplinkProcessingRunner.shutdown();
        cloudEventUplinkSenders.forEach(CloudEventUplinkSender::shutdown);
    }
}
