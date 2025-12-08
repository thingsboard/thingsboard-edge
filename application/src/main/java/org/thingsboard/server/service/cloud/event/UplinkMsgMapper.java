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
package org.thingsboard.server.service.cloud.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.cloud.CloudContextComponent;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class UplinkMsgMapper {

    private final CloudContextComponent cloudCtx;

    public List<UplinkMsg> convertCloudEventsToUplink(List<CloudEvent> cloudEvents) {
        log.trace("[{}] event(s) are going to be converted.", cloudEvents.size());
        return cloudEvents.stream()
                .map(this::convertCloudEventToUplink)
                .filter(Objects::nonNull)
                .toList();
    }

    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        log.trace("Converting cloud event [{}]", cloudEvent);
        try {
            return switch (cloudEvent.getAction()) {
                case UPDATED, ADDED, DELETED, ALARM_ACK, ALARM_CLEAR, ALARM_DELETE, CREDENTIALS_UPDATED,
                     RELATION_ADD_OR_UPDATE, RELATION_DELETED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER,
                     ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> convertEntityEventToUplink(cloudEvent);
                case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                        cloudCtx.getTelemetryProcessor().convertTelemetryEventToUplink(cloudEvent.getTenantId(), cloudEvent);
                case ATTRIBUTES_REQUEST -> cloudCtx.getTelemetryProcessor().convertAttributesRequestEventToUplink(cloudEvent);
                case RELATION_REQUEST -> cloudCtx.getRelationProcessor().convertRelationRequestEventToUplink(cloudEvent);
                case CALCULATED_FIELD_REQUEST -> cloudCtx.getCalculatedFieldProcessor().convertCalculatedFieldRequestEventToUplink(cloudEvent);
                case RPC_CALL -> cloudCtx.getDeviceProcessor().convertRpcCallEventToUplink(cloudEvent);
                default -> {
                    log.warn("Unsupported action type [{}]", cloudEvent);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Exception during converting events from queue, skipping event [{}]", cloudEvent, e);
            return null;
        }
    }

    private UplinkMsg convertEntityEventToUplink(CloudEvent cloudEvent) {
        log.trace("Executing convertEntityEventToUplink cloudEvent [{}], edgeEventAction [{}]", cloudEvent, cloudEvent.getAction());
        return cloudCtx.getProcessor(cloudEvent.getType()).convertCloudEventToUplink(cloudEvent);
    }
}
