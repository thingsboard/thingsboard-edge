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
package org.thingsboard.server.actors.calculatedField;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;

@Slf4j
public class CalculatedFieldEntityActor extends AbstractCalculatedFieldActor {

    private final CalculatedFieldEntityMessageProcessor processor;

    CalculatedFieldEntityActor(ActorSystemContext systemContext, TenantId tenantId, EntityId entityId) {
        super(systemContext, tenantId);
        this.processor = new CalculatedFieldEntityMessageProcessor(systemContext, tenantId, entityId);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}][{}] Starting CF entity actor.", processor.tenantId, processor.entityId);
        try {
            processor.init(ctx);
            log.debug("[{}][{}] CF entity actor started.", processor.tenantId, processor.entityId);
        } catch (Exception e) {
            log.warn("[{}][{}] Unknown failure", processor.tenantId, processor.entityId, e);
            throw new TbActorException("Failed to initialize CF entity actor", e);
        }
    }

    @Override
    protected boolean doProcessCfMsg(ToCalculatedFieldSystemMsg msg) throws CalculatedFieldException {
        switch (msg.getMsgType()) {
            case CF_PARTITIONS_CHANGE_MSG:
                processor.process((CalculatedFieldPartitionChangeMsg) msg);
                break;
            case CF_STATE_RESTORE_MSG:
                processor.process((CalculatedFieldStateRestoreMsg) msg);
                break;
            case CF_ENTITY_INIT_CF_MSG:
                processor.process((EntityInitCalculatedFieldMsg) msg);
                break;
            case CF_ENTITY_DELETE_MSG:
                processor.process((CalculatedFieldEntityDeleteMsg) msg);
                break;
            case CF_ENTITY_TELEMETRY_MSG:
                processor.process((EntityCalculatedFieldTelemetryMsg) msg);
                break;
            case CF_LINKED_TELEMETRY_MSG:
                processor.process((EntityCalculatedFieldLinkedTelemetryMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    void logProcessingException(Exception e) {
        log.warn("[{}][{}] Processing failure", tenantId, processor.entityId, e);
    }
}
