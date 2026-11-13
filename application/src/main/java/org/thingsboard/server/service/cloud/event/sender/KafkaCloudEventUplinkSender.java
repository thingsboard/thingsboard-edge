// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.sender;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.dao.cloud.CloudEventService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaCloudEventUplinkSender implements CloudEventUplinkSender {

    private final CloudEventService kafkaEventService;

    @Override
    public ListenableFuture<Boolean> sendCloudEvents(List<CloudEvent> cloudEvents, boolean isGeneralMsg) {
        for (CloudEvent cloudEvent : cloudEvents) {
            if (isGeneralMsg) {
                kafkaEventService.saveAsync(cloudEvent);
            } else {
                kafkaEventService.saveTsKvAsync(cloudEvent);
            }
        }
        return Futures.immediateFuture(Boolean.FALSE);
    }

    @Override
    public void init() {
    }

    @Override
    public void shutdown() {
    }
}
