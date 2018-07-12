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

import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;

public class BasicCommandAckResponse extends BasicResponseMsg<Integer> implements StatusCodeResponse {

    private static final long serialVersionUID = 1L;

    public static BasicCommandAckResponse onSuccess(SessionMsgType requestMsgType, Integer requestId) {
        return BasicCommandAckResponse.onSuccess(requestMsgType, requestId, 200);
    }

    public static BasicCommandAckResponse onSuccess(SessionMsgType requestMsgType, Integer requestId, Integer code) {
        return new BasicCommandAckResponse(requestMsgType, requestId, true, null, code);
    }

    public static BasicCommandAckResponse onError(SessionMsgType requestMsgType, Integer requestId, Exception error) {
        return new BasicCommandAckResponse(requestMsgType, requestId, false, error, null);
    }

    private BasicCommandAckResponse(SessionMsgType requestMsgType, Integer requestId, boolean success, Exception error, Integer code) {
        super(requestMsgType, requestId, SessionMsgType.TO_DEVICE_RPC_RESPONSE_ACK, success, error, code);
    }

    @Override
    public String toString() {
        return "BasicStatusCodeResponse []";
    }
}
