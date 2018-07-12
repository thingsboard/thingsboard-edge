/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.msg.core;

import lombok.Data;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;

/**
 * @author Andrew Shvayka
 */
@Data
public class RuleEngineErrorMsg implements ToDeviceMsg {

    private final SessionMsgType inSessionMsgType;
    private final RuleEngineError error;

    @Override
    public boolean isSuccess() {
        return false;
    }

    public SessionMsgType getSessionMsgType() {
        return SessionMsgType.RULE_ENGINE_ERROR;
    }

    public String getErrorMsg() {
        switch (error) {
            case QUEUE_PUT_TIMEOUT:
                return "Timeout during persistence of the message to the queue!";
            case SERVER_ERROR:
                return "Error during processing of message by the server!";
            case TIMEOUT:
                return "Timeout during processing of message by the server!";
            default:
                throw new RuntimeException("Error " + error + " is not supported!");
        }
    }
}
