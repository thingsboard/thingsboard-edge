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
package org.thingsboard.server.service.integration.http;

import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.session.SessionActorToAdaptorMsg;
import org.thingsboard.server.common.msg.session.SessionContext;
import org.thingsboard.server.common.msg.session.SessionCtrlMsg;
import org.thingsboard.server.common.msg.session.SessionType;
import org.thingsboard.server.common.msg.session.ex.SessionException;
import org.thingsboard.server.transport.http.session.HttpSessionId;

/**
 * Created by ashvayka on 05.12.17.
 */
public class IntegrationHttpSessionCtx implements SessionContext {

    private final SessionId sessionId;

    public IntegrationHttpSessionCtx() {
        this.sessionId = new HttpSessionId();
    }

    @Override
    public SessionId getSessionId() {
        return sessionId;
    }

    @Override
    public SessionType getSessionType() {
        return SessionType.SYNC;
    }


    @Override
    public void onMsg(SessionActorToAdaptorMsg msg) throws SessionException {

    }

    @Override
    public void onMsg(SessionCtrlMsg msg) throws SessionException {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getTimeout() {
        return 0;
    }
}
