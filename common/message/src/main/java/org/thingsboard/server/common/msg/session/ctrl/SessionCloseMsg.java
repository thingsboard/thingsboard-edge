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
package org.thingsboard.server.common.msg.session.ctrl;

import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;

public class SessionCloseMsg implements SessionCtrlMsg {

    private final SessionId sessionId;
    private final boolean revoked;
    private final boolean timeout;

    public static SessionCloseMsg onDisconnect(SessionId sessionId) {
        return new SessionCloseMsg(sessionId, false, false);
    }

    public static SessionCloseMsg onError(SessionId sessionId) {
        return new SessionCloseMsg(sessionId, false, false);
    }

    public static SessionCloseMsg onTimeout(SessionId sessionId) {
        return new SessionCloseMsg(sessionId, false, true);
    }

    public static SessionCloseMsg onCredentialsRevoked(SessionId sessionId) {
        return new SessionCloseMsg(sessionId, true, false);
    }

    private SessionCloseMsg(SessionId sessionId, boolean unauthorized, boolean timeout) {
        super();
        this.sessionId = sessionId;
        this.revoked = unauthorized;
        this.timeout = timeout;
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    public boolean isCredentialsRevoked() {
        return revoked;
    }

    public boolean isTimeout() {
        return timeout;
    }

}
