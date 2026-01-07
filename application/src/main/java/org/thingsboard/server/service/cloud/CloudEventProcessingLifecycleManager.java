/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
