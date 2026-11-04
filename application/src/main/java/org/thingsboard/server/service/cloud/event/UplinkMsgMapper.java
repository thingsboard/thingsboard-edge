// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.cloud.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.cloud.CloudContextComponent;
import org.thingsboard.server.service.cloud.rpc.processor.EntityGroupCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.GroupPermissionCloudProcessor;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class UplinkMsgMapper {

    private final CloudContextComponent cloudCtx;
    private final EntityGroupCloudProcessor entityGroupProcessor;
    private final GroupPermissionCloudProcessor groupPermissionProcessor;

    public List<UplinkMsg> convertCloudEventsToUplink(List<CloudEvent> cloudEvents) {
        cloudEvents = EdgeMsgConstructorUtils.mergeAndFilterUplinkDuplicates(cloudEvents);
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
                     RELATION_ADD_OR_UPDATE, RELATION_DELETED, ADDED_TO_ENTITY_GROUP, REMOVED_FROM_ENTITY_GROUP,
                     ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> convertEntityEventToUplink(cloudEvent);
                case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                        cloudCtx.getTelemetryProcessor().convertTelemetryEventToUplink(cloudEvent.getTenantId(), cloudEvent);
                case ATTRIBUTES_REQUEST -> cloudCtx.getTelemetryProcessor().convertAttributesRequestEventToUplink(cloudEvent);
                case GROUP_ENTITIES_REQUEST -> entityGroupProcessor.processGroupEntitiesRequestMsgToCloud(cloudEvent);
                case GROUP_PERMISSIONS_REQUEST -> groupPermissionProcessor.processEntityGroupPermissionsRequestMsgToCloud(cloudEvent);
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
