/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
