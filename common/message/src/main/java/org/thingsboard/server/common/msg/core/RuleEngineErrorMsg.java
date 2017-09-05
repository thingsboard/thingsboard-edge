/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.common.msg.core;

import lombok.Data;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;

/**
 * @author Andrew Shvayka
 */
@Data
public class RuleEngineErrorMsg implements ToDeviceMsg {

    private final MsgType inMsgType;
    private final RuleEngineError error;

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_ENGINE_ERROR;
    }

    public String getErrorMsg() {
        switch (error) {
            case NO_RULES:
                return "No rules configured!";
            case NO_ACTIVE_RULES:
                return "No active rules!";
            case NO_FILTERS_MATCHED:
                return "No rules that match current message!";
            case NO_REQUEST_FROM_ACTIONS:
                return "Rule filters match, but no plugin message produced by rule action!";
            case NO_TWO_WAY_ACTIONS:
                return "Rule filters match, but no rule with two-way action configured!";
            case NO_RESPONSE_FROM_ACTIONS:
                return "Rule filters match, message processed by plugin, but no response produced by rule action!";
            case PLUGIN_TIMEOUT:
                return "Timeout during processing of message by plugin!";
            default:
                throw new RuntimeException("Error " + error + " is not supported!");
        }
    }
}
