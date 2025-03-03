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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldEntityLifecycleMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public class CalculatedFieldManagerActor extends AbstractCalculatedFieldActor {

    private final CalculatedFieldManagerMessageProcessor processor;

    public CalculatedFieldManagerActor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext, tenantId);
        this.processor = new CalculatedFieldManagerMessageProcessor(systemContext, tenantId);
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        log.debug("[{}] Starting CF manager actor.", processor.tenantId);
        try {
            processor.init(ctx);
            log.debug("[{}] CF manager actor started.", processor.tenantId);
        } catch (Exception e) {
            log.warn("[{}] Unknown failure", processor.tenantId, e);
            throw new TbActorException("Failed to initialize manager actor", e);
        }
    }

    @Override
    protected boolean doProcessCfMsg(ToCalculatedFieldSystemMsg msg) throws CalculatedFieldException {
        switch (msg.getMsgType()) {
            case CF_PARTITIONS_CHANGE_MSG:
                processor.onPartitionChange((CalculatedFieldPartitionChangeMsg) msg);
                break;
            case CF_INIT_MSG:
                processor.onFieldInitMsg((CalculatedFieldInitMsg) msg);
                break;
            case CF_LINK_INIT_MSG:
                processor.onLinkInitMsg((CalculatedFieldLinkInitMsg) msg);
                break;
            case CF_STATE_RESTORE_MSG:
                processor.onStateRestoreMsg((CalculatedFieldStateRestoreMsg) msg);
                break;
            case CF_ENTITY_LIFECYCLE_MSG:
                processor.onEntityLifecycleMsg((CalculatedFieldEntityLifecycleMsg) msg);
                break;
            case CF_TELEMETRY_MSG:
                processor.onTelemetryMsg((CalculatedFieldTelemetryMsg) msg);
                break;
            case CF_LINKED_TELEMETRY_MSG:
                processor.onLinkedTelemetryMsg((CalculatedFieldLinkedTelemetryMsg) msg);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    void logProcessingException(Exception e) {
        log.warn("[{}] Processing failure", tenantId, e);
    }

}
