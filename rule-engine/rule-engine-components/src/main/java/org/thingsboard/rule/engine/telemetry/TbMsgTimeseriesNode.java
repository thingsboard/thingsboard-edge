/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.telemetry.strategy.PersistenceStrategy;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.PersistenceSettings;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.PersistenceSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.PersistenceSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.PersistenceSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration.PersistenceSettings.WebSocketsOnly;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save time series",
        configClazz = TbMsgTimeseriesNodeConfiguration.class,
        nodeDescription = "Saves time series data",
        nodeDetails = "Saves time series telemetry data based on configurable TTL parameter. Expects messages with 'POST_TELEMETRY_REQUEST' message type. " +
                "Timestamp in milliseconds will be taken from metadata.ts, otherwise 'now' message timestamp will be applied. " +
                "Allows stopping updating values for incoming keys in the latest ts_kv table if 'skipLatestPersistence' is set to true.\n " +
                "<br/>" +
                "Enable 'useServerTs' param to use the timestamp of the message processing instead of the timestamp from the message. " +
                "Useful for all sorts of sequential processing if you merge messages from multiple sources (devices, assets, etc).\n" +
                "<br/>" +
                "In the case of sequential processing, the platform guarantees that the messages are processed in the order of their submission to the queue. " +
                "However, the timestamp of the messages originated by multiple devices/servers may be unsynchronized long before they are pushed to the queue. " +
                "The DB layer has certain optimizations to ignore the updates of the \"attributes\" and \"latest values\" tables if the new record has a timestamp that is older than the previous record. " +
                "So, to make sure that all the messages will be processed correctly, one should enable this parameter for sequential message processing scenarios.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeTimeseriesConfig",
        icon = "file_upload",
        version = 1
)
public class TbMsgTimeseriesNode implements TbNode {

    private TbMsgTimeseriesNodeConfiguration config;
    private TbContext ctx;
    private long tenantProfileDefaultStorageTtl;

    private PersistenceSettings persistenceSettings;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgTimeseriesNodeConfiguration.class);
        this.ctx = ctx;
        ctx.addTenantProfileListener(this::onTenantProfileUpdate);
        onTenantProfileUpdate(ctx.getTenantProfile());
        persistenceSettings = config.getPersistenceSettings();
        if (persistenceSettings == null) {
            throw new TbNodeException("Persistence settings cannot be null", true);
        }
    }

    private void onTenantProfileUpdate(TenantProfile tenantProfile) {
        DefaultTenantProfileConfiguration configuration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        tenantProfileDefaultStorageTtl = TimeUnit.DAYS.toSeconds(configuration.getDefaultStorageTtlDays());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(POST_TELEMETRY_REQUEST)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        long ts = computeTs(msg, config.isUseServerTs());

        PersistenceDecision persistenceDecision = makePersistenceDecision(ts, msg.getOriginator().getId());
        boolean saveTimeseries = persistenceDecision.saveTimeseries();
        boolean saveLatest = persistenceDecision.saveLatest();
        boolean sendWsUpdate = persistenceDecision.sendWsUpdate();

        // short-circuit
        if (!saveTimeseries && !saveLatest && !sendWsUpdate) {
            ctx.tellSuccess(msg);
            return;
        }

        String src = msg.getData();
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(src), ts);
        if (tsKvMap.isEmpty()) {
            ctx.tellFailure(msg, new IllegalArgumentException("Msg body is empty: " + src));
            return;
        }
        List<TsKvEntry> tsKvEntryList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }
        String ttlValue = msg.getMetaData().getValue("TTL");
        long ttl = !StringUtils.isEmpty(ttlValue) ? Long.parseLong(ttlValue) : config.getDefaultTTL();
        if (ttl == 0L) {
            ttl = tenantProfileDefaultStorageTtl;
        }
        String overwriteValueStr = msg.getMetaData().getValue("overwriteValue");
        boolean overwriteValue = Boolean.parseBoolean(overwriteValueStr);
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entries(tsKvEntryList)
                .ttl(ttl)
                .saveTimeseries(saveTimeseries)
                .saveLatest(saveLatest)
                .sendWsUpdate(sendWsUpdate)
                .overwriteValue(overwriteValue)
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build());
    }

    public static long computeTs(TbMsg msg, boolean ignoreMetadataTs) {
        return ignoreMetadataTs ? System.currentTimeMillis() : msg.getMetaDataTs();
    }

    private record PersistenceDecision(boolean saveTimeseries, boolean saveLatest, boolean sendWsUpdate) {}

    private PersistenceDecision makePersistenceDecision(long ts, UUID originatorUuid) {
        boolean saveTimeseries;
        boolean saveLatest;
        boolean sendWsUpdate;

        if (persistenceSettings instanceof OnEveryMessage) {
            saveTimeseries = true;
            saveLatest = true;
            sendWsUpdate = true;
        } else if (persistenceSettings instanceof WebSocketsOnly) {
            saveTimeseries = false;
            saveLatest = false;
            sendWsUpdate = true;
        } else if (persistenceSettings instanceof Deduplicate deduplicate) {
            boolean isFirstMsgInInterval = deduplicate.getDeduplicateStrategy().shouldPersist(ts, originatorUuid);
            saveTimeseries = isFirstMsgInInterval;
            saveLatest = isFirstMsgInInterval;
            sendWsUpdate = isFirstMsgInInterval;
        } else if (persistenceSettings instanceof Advanced advanced) {
            saveTimeseries = advanced.timeseries().shouldPersist(ts, originatorUuid);
            saveLatest = advanced.latest().shouldPersist(ts, originatorUuid);
            sendWsUpdate = advanced.webSockets().shouldPersist(ts, originatorUuid);
        } else { // should not happen
            throw new IllegalArgumentException("Unknown persistence settings type: " + persistenceSettings.getClass().getSimpleName());
        }

        return new PersistenceDecision(saveTimeseries, saveLatest, sendWsUpdate);
    }

    @Override
    public void destroy() {
        ctx.removeListeners();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has("persistenceSettings") && !oldConfiguration.has("skipLatestPersistence")) {
                    break;
                }
                hasChanges = true;
                JsonNode skipLatestPersistence = oldConfiguration.get("skipLatestPersistence");
                if (skipLatestPersistence != null && "true".equals(skipLatestPersistence.asText())) {
                    var skipLatestPersistenceSettings = new Advanced(
                            PersistenceStrategy.onEveryMessage(),
                            PersistenceStrategy.skip(),
                            PersistenceStrategy.onEveryMessage()
                    );
                    ((ObjectNode) oldConfiguration).set("persistenceSettings", JacksonUtil.valueToTree(skipLatestPersistenceSettings));
                } else {
                    ((ObjectNode) oldConfiguration).set("persistenceSettings", JacksonUtil.valueToTree(new OnEveryMessage()));
                }
                ((ObjectNode) oldConfiguration).remove("skipLatestPersistence");
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
