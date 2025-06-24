/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.calculated.BaseCalculatedFieldProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class CalculatedFieldCloudProcessor extends BaseCalculatedFieldProcessor {

    public ListenableFuture<Void> processCalculatedFieldMsgFromCloud(TenantId tenantId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(calculatedFieldUpdateMsg.getIdMSB(), calculatedFieldUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);
            return switch (calculatedFieldUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateCalculatedFieldFromCloud(tenantId, calculatedFieldId, calculatedFieldUpdateMsg);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
                    if (calculatedField != null) {
                        edgeCtx.getCalculatedFieldService().deleteCalculatedField(tenantId, calculatedFieldId);
                        pushCalculatedFieldEventToRuleEngine(tenantId, calculatedField, TbMsgType.ENTITY_DELETED);
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(calculatedFieldUpdateMsg.getMsgType());
            };
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateCalculatedFieldFromCloud(TenantId tenantId, CalculatedFieldId calculatedFieldId, CalculatedFieldUpdateMsg calculatedFieldUpdateMsg) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateCalculatedField(tenantId, calculatedFieldId, calculatedFieldUpdateMsg);
        Boolean wasCreated = resultPair.getFirst();
        if (wasCreated) {
            CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(tenantId, calculatedFieldId);
            pushCalculatedFieldEventToRuleEngine(tenantId, calculatedField, TbMsgType.ENTITY_CREATED);
        }
        Boolean calculatedFieldNameWasUpdated = resultPair.getSecond();
        if (calculatedFieldNameWasUpdated) {
            cloudEventService.saveCloudEventAsync(tenantId, CloudEventType.CALCULATED_FIELD, EdgeEventActionType.UPDATED, calculatedFieldId, null, null);
        }
    }

    private void pushCalculatedFieldEventToRuleEngine(TenantId tenantId, CalculatedField calculatedField, TbMsgType msgType) {
        try {
            String calculatedFieldAsString = JacksonUtil.toString(calculatedField);
            pushEntityEventToRuleEngine(tenantId, calculatedField.getId(), null, msgType, calculatedFieldAsString, new TbMsgMetaData());
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push calculatedField action to rule engine: {}", tenantId, calculatedField.getId(), msgType.name(), e);
        }
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                CalculatedField calculatedField = edgeCtx.getCalculatedFieldService().findById(cloudEvent.getTenantId(), calculatedFieldId);
                if (calculatedField != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = EdgeMsgConstructorUtils.constructCalculatedFieldUpdatedMsg(msgType, calculatedField);
                    UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsg);

                    return builder.build();
                } else {
                    log.info("Skipping event as calculatedField was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = EdgeMsgConstructorUtils.constructCalculatedFieldDeleteMsg(calculatedFieldId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsg).build();
            }
        }
        return null;
    }

    public UplinkMsg convertCalculatedFieldRequestEventToUplink(CloudEvent cloudEvent) {
        EntityId entityId = EntityIdFactory.getByCloudEventTypeAndUuid(cloudEvent.getType(), cloudEvent.getEntityId());
        CalculatedFieldRequestMsg calculatedFieldRequestMsg = CalculatedFieldRequestMsg.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name())
                .build();
        UplinkMsg.Builder builder = UplinkMsg.newBuilder()
                .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                .addCalculatedFieldRequestMsg(calculatedFieldRequestMsg);
        return builder.build();
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.CALCULATED_FIELD;
    }

}
