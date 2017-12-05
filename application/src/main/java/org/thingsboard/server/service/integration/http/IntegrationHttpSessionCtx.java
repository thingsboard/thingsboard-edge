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
