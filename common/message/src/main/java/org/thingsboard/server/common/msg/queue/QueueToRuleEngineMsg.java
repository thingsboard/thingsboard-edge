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
package org.thingsboard.server.common.msg.queue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbRuleEngineActorMsg;

import java.util.Set;

/**
 * Created by ashvayka on 15.03.18.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class QueueToRuleEngineMsg extends TbRuleEngineActorMsg {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final Set<String> relationTypes;
    @Getter
    private final String failureMessage;

    public QueueToRuleEngineMsg(TenantId tenantId, TbMsg tbMsg, Set<String> relationTypes, String failureMessage) {
        super(tbMsg);
        this.tenantId = tenantId;
        this.relationTypes = relationTypes;
        this.failureMessage = failureMessage;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.QUEUE_TO_RULE_ENGINE_MSG;
    }

    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message;
        if (msg.getRuleChainId() != null) {
            message = reason == TbActorStopReason.STOPPED ?
                    String.format("Rule chain [%s] stopped", msg.getRuleChainId().getId()) :
                    String.format("Failed to initialize rule chain [%s]!", msg.getRuleChainId().getId());
        } else {
            message = reason == TbActorStopReason.STOPPED ? "Rule chain stopped" : "Failed to initialize rule chain!";
        }
        msg.getCallback().onFailure(new RuleEngineException(message));
    }

    public boolean isTellNext() {
        return relationTypes != null && !relationTypes.isEmpty();
    }

}
