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
package org.thingsboard.server.extensions.core.plugin.telemetry.handlers;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.core.*;
import org.thingsboard.server.common.msg.kv.BasicAttributeKVMsg;
import org.thingsboard.server.extensions.api.plugins.PluginCallback;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.DefaultRuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.GetAttributesRequestRuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.TelemetryUploadRequestRuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.UpdateAttributesRequestRuleToPluginMsg;
import org.thingsboard.server.extensions.core.plugin.telemetry.SubscriptionManager;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionType;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TelemetryRuleMsgHandler extends DefaultRuleMsgHandler {
    private final SubscriptionManager subscriptionManager;

    public TelemetryRuleMsgHandler(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void handleGetAttributesRequest(PluginContext ctx, TenantId tenantId, RuleId ruleId, GetAttributesRequestRuleToPluginMsg msg) {
        GetAttributesRequest request = msg.getPayload();

        BiPluginCallBack<List<AttributeKvEntry>, List<AttributeKvEntry>> callback = new BiPluginCallBack<List<AttributeKvEntry>, List<AttributeKvEntry>>() {

            @Override
            public void onSuccess(PluginContext ctx, List<AttributeKvEntry> clientAttributes, List<AttributeKvEntry> sharedAttributes) {
                BasicGetAttributesResponse response = BasicGetAttributesResponse.onSuccess(request.getMsgType(),
                        request.getRequestId(), BasicAttributeKVMsg.from(clientAttributes, sharedAttributes));
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, response));
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error("Failed to process get attributes request", e);
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, BasicStatusCodeResponse.onError(request.getMsgType(), request.getRequestId(), e)));
            }
        };

        getAttributeKvEntries(ctx, msg.getDeviceId(), DataConstants.CLIENT_SCOPE, request.getClientAttributeNames(), callback.getV1Callback());
        getAttributeKvEntries(ctx, msg.getDeviceId(), DataConstants.SHARED_SCOPE, request.getSharedAttributeNames(), callback.getV2Callback());
    }

    private void getAttributeKvEntries(PluginContext ctx, DeviceId deviceId, String scope, Optional<Set<String>> names, PluginCallback<List<AttributeKvEntry>> callback) {
        if (names.isPresent()) {
            if (!names.get().isEmpty()) {
                ctx.loadAttributes(deviceId, scope, new ArrayList<>(names.get()), callback);
            } else {
                ctx.loadAttributes(deviceId, scope, callback);
            }
        } else {
            callback.onSuccess(ctx, Collections.emptyList());
        }
    }

    @Override
    public void handleTelemetryUploadRequest(PluginContext ctx, TenantId tenantId, RuleId ruleId, TelemetryUploadRequestRuleToPluginMsg msg) {
        TelemetryUploadRequest request = msg.getPayload();
        List<TsKvEntry> tsKvEntries = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> entry : request.getData().entrySet()) {
            for (KvEntry kv : entry.getValue()) {
                tsKvEntries.add(new BasicTsKvEntry(entry.getKey(), kv));
            }
        }
        ctx.saveTsData(msg.getDeviceId(), tsKvEntries, msg.getTtl(), new PluginCallback<Void>() {
            @Override
            public void onSuccess(PluginContext ctx, Void data) {
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, BasicStatusCodeResponse.onSuccess(request.getMsgType(), request.getRequestId())));
                subscriptionManager.onLocalSubscriptionUpdate(ctx, msg.getDeviceId(), SubscriptionType.TIMESERIES, s -> {
                    List<TsKvEntry> subscriptionUpdate = new ArrayList<TsKvEntry>();
                    for (Map.Entry<Long, List<KvEntry>> entry : request.getData().entrySet()) {
                        for (KvEntry kv : entry.getValue()) {
                            if (s.isAllKeys() || s.getKeyStates().containsKey((kv.getKey()))) {
                                subscriptionUpdate.add(new BasicTsKvEntry(entry.getKey(), kv));
                            }
                        }
                    }
                    return subscriptionUpdate;
                });
            }

            @Override
            public void onFailure(PluginContext ctx, Exception e) {
                log.error("Failed to process telemetry upload request", e);
                ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, BasicStatusCodeResponse.onError(request.getMsgType(), request.getRequestId(), e)));
            }
        });
    }

    @Override
    public void handleUpdateAttributesRequest(PluginContext ctx, TenantId tenantId, RuleId ruleId, UpdateAttributesRequestRuleToPluginMsg msg) {
        UpdateAttributesRequest request = msg.getPayload();
        ctx.saveAttributes(msg.getTenantId(), msg.getDeviceId(), DataConstants.CLIENT_SCOPE, request.getAttributes().stream().collect(Collectors.toList()),
                new PluginCallback<Void>() {
                    @Override
                    public void onSuccess(PluginContext ctx, Void value) {
                        ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, BasicStatusCodeResponse.onSuccess(request.getMsgType(), request.getRequestId())));

                        subscriptionManager.onLocalSubscriptionUpdate(ctx, msg.getDeviceId(), SubscriptionType.ATTRIBUTES, s -> {
                            List<TsKvEntry> subscriptionUpdate = new ArrayList<TsKvEntry>();
                            for (AttributeKvEntry kv : request.getAttributes()) {
                                if (s.isAllKeys() || s.getKeyStates().containsKey(kv.getKey())) {
                                    subscriptionUpdate.add(new BasicTsKvEntry(kv.getLastUpdateTs(), kv));
                                }
                            }
                            return subscriptionUpdate;
                        });
                    }

                    @Override
                    public void onFailure(PluginContext ctx, Exception e) {
                        log.error("Failed to process attributes update request", e);
                        ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId, BasicStatusCodeResponse.onError(request.getMsgType(), request.getRequestId(), e)));
                    }
                });
    }
}