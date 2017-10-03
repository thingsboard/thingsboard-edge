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
package org.thingsboard.server.extensions.api.plugins.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;
import org.thingsboard.server.extensions.api.plugins.ws.SessionEvent;
import org.thingsboard.server.extensions.api.plugins.ws.WsSessionMetaData;
import org.thingsboard.server.extensions.api.plugins.ws.msg.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DefaultWebsocketMsgHandler implements WebsocketMsgHandler {

    protected final ObjectMapper jsonMapper = new ObjectMapper();

    protected final Map<String, WsSessionMetaData> wsSessionsMap = new HashMap<>();

    @Override
    public void process(PluginContext ctx, PluginWebsocketMsg<?> wsMsg) {
        PluginWebsocketSessionRef sessionRef = wsMsg.getSessionRef();
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing: {}", sessionRef.getSessionId(), wsMsg);
        } else {
            log.debug("[{}] Processing: {}", sessionRef.getSessionId(), wsMsg.getClass().getSimpleName());
        }
        if (wsMsg instanceof SessionEventPluginWebSocketMsg) {
            handleWebSocketSessionEvent(ctx, sessionRef, (SessionEventPluginWebSocketMsg) wsMsg);
        } else if (wsMsg instanceof TextPluginWebSocketMsg || wsMsg instanceof BinaryPluginWebSocketMsg) {
            handleWebSocketMsg(ctx, sessionRef, wsMsg);
        } else if (wsMsg instanceof PongPluginWebsocketMsg) {
            handleWebSocketPongEvent(ctx, sessionRef);
        }
    }

    protected void handleWebSocketMsg(PluginContext ctx, PluginWebsocketSessionRef sessionRef, PluginWebsocketMsg<?> wsMsg) {
        throw new RuntimeException("Web-sockets are not supported by current plugin!");
    }

    protected void cleanupWebSocketSession(PluginContext ctx, String sessionId) {

    }

    protected void handleWebSocketSessionEvent(PluginContext ctx, PluginWebsocketSessionRef sessionRef, SessionEventPluginWebSocketMsg wsMsg) {
        String sessionId = sessionRef.getSessionId();
        SessionEvent event = wsMsg.getPayload();
        log.debug("[{}] Processing: {}", sessionId, event);
        switch (event.getEventType()) {
            case ESTABLISHED:
                wsSessionsMap.put(sessionId, new WsSessionMetaData(sessionRef));
                break;
            case ERROR:
                log.debug("[{}] Unknown websocket session error: {}. ", sessionId, event.getError().orElse(null));
                break;
            case CLOSED:
                wsSessionsMap.remove(sessionId);
                cleanupWebSocketSession(ctx, sessionId);
                break;
        }
    }

    protected void handleWebSocketPongEvent(PluginContext ctx, PluginWebsocketSessionRef sessionRef) {
        String sessionId = sessionRef.getSessionId();
        WsSessionMetaData sessionMD = wsSessionsMap.get(sessionId);
        if (sessionMD != null) {
            log.debug("[{}] Updating session metadata: {}", sessionId, sessionRef);
            sessionMD.setSessionRef(sessionRef);
            sessionMD.setLastActivityTime(System.currentTimeMillis());
        }
    }

    public void clear(PluginContext ctx) {
        wsSessionsMap.values().forEach(v -> {
            try {
                ctx.close(v.getSessionRef());
            } catch (IOException e) {
                log.debug("[{}] Failed to close session: {}", v.getSessionRef().getSessionId(), e.getMessage(), e);
            }
        });
        wsSessionsMap.clear();
    }
}