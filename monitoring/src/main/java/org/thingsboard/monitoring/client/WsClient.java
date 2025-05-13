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
package org.thingsboard.monitoring.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.data.cmd.CmdsWrapper;
import org.thingsboard.monitoring.data.cmd.EntityDataCmd;
import org.thingsboard.monitoring.data.cmd.EntityDataUpdate;
import org.thingsboard.monitoring.data.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;

import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class WsClient extends WebSocketClient implements AutoCloseable {

    public final List<JsonNode> lastMsgs = new ArrayList<>();
    private CountDownLatch reply;
    private CountDownLatch update;

    private final Lock updateLock = new ReentrantLock();

    private final long requestTimeoutMs;

    public WsClient(URI serverUri, long requestTimeoutMs) {
        super(serverUri);
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String s) {
        if (s == null) {
            return;
        }
        updateLock.lock();
        try {
            JsonNode msg = JacksonUtil.toJsonNode(s);
            lastMsgs.add(msg);
            log.trace("Received new msg: {}", msg.toPrettyString());
            if (update != null) {
                update.countDown();
            }
            if (reply != null) {
                reply.countDown();
            }
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        log.debug("WebSocket client is closed");
    }

    @Override
    public void onError(Exception e) {
        log.error("WebSocket client error:", e);
    }

    public void registerWaitForUpdates(int count) {
        updateLock.lock();
        try {
            lastMsgs.clear();
            update = new CountDownLatch(count);
        } finally {
            updateLock.unlock();
        }
        log.trace("Registered wait for update");
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        updateLock.lock();
        try {
            lastMsgs.clear();
            reply = new CountDownLatch(1);
        } finally {
            updateLock.unlock();
        }
        super.send(text);
    }

    public WsClient subscribeForTelemetry(List<UUID> devices, List<String> keys) {
        EntityDataCmd cmd = new EntityDataCmd();
        cmd.setCmdId(RandomUtils.nextInt(0, 1000));

        EntityListFilter devicesFilter = new EntityListFilter();
        devicesFilter.setEntityType(EntityType.DEVICE);
        devicesFilter.setEntityList(devices.stream().map(UUID::toString).collect(Collectors.toList()));
        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, null);
        EntityDataQuery devicesQuery = new EntityDataQuery(devicesFilter, pageLink, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        cmd.setQuery(devicesQuery);

        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(keys.stream().map(key -> new EntityKey(EntityKeyType.TIME_SERIES, key)).toList());
        cmd.setLatestCmd(latestCmd);

        CmdsWrapper wrapper = new CmdsWrapper();
        wrapper.setEntityDataCmds(List.of(cmd));
        send(JacksonUtil.toString(wrapper));
        return this;
    }

    public List<JsonNode> waitForUpdates(long ms) {
        log.trace("update latch count: {}", update.getCount());
        try {
            if (update.await(ms, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for update");
                return getLastMsgs();
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No update arrived within {} ms", ms);
        return null;
    }

    public JsonNode waitForReply() {
        try {
            if (reply.await(requestTimeoutMs, TimeUnit.MILLISECONDS)) {
                log.trace("Waited for reply");
                List<JsonNode> lastMsgs = getLastMsgs();
                return lastMsgs.isEmpty() ? null : lastMsgs.get(0);
            }
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        log.trace("No reply arrived within {} ms", requestTimeoutMs);
        throw new IllegalStateException("No WS reply arrived within " + requestTimeoutMs + " ms");
    }

    private List<JsonNode> getLastMsgs() {
        if (lastMsgs.isEmpty()) {
            return lastMsgs;
        }
        List<JsonNode> errors = lastMsgs.stream()
                .map(msg -> msg.get("errorMsg"))
                .filter(errorMsg -> errorMsg != null && !errorMsg.isNull() && StringUtils.isNotEmpty(errorMsg.asText()))
                .toList();
        if (!errors.isEmpty()) {
            throw new RuntimeException("WS error from server: " + errors.stream()
                    .map(JsonNode::asText)
                    .collect(Collectors.joining(", ")));
        }
        return lastMsgs;
    }

    public Map<String, String> getLatest(UUID deviceId) {
        Map<String, String> updates = new HashMap<>();
        getLastMsgs().forEach(msg -> {
            EntityDataUpdate update = JacksonUtil.treeToValue(msg, EntityDataUpdate.class);
            Map<String, String> latest = update.getLatest(deviceId);
            updates.putAll(latest);
        });
        return updates;
    }

    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }

}