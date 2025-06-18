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
package org.thingsboard.rule.engine.telemetry;

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "calculated fields",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Pushes incoming messages to calculated fields service",
        nodeDetails = "Node enables the processing of calculated fields without persisting incoming messages to the database. " +
                "By default, the processing of calculated fields is triggered by the <b>save attributes</b> and <b>save time series</b> nodes. " +
                "This rule node accepts the same messages as these nodes but allows you to trigger the processing of calculated " +
                "fields independently, ensuring that derived data can be computed and utilized in real time without storing the original message in the database.",
        configDirective = "tbNodeEmptyConfig",
        icon = "published_with_changes"
)
public class TbCalculatedFieldsNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        switch (msg.getInternalType()) {
            case POST_TELEMETRY_REQUEST -> processPostTelemetryRequest(ctx, msg);
            case POST_ATTRIBUTES_REQUEST -> processPostAttributesRequest(ctx, msg);
            default -> ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
        }
    }

    private void processPostTelemetryRequest(TbContext ctx, TbMsg msg) {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(msg.getData()), System.currentTimeMillis());

        if (tsKvMap.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }

        List<TsKvEntry> tsKvEntryList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }

        TimeseriesSaveRequest timeseriesSaveRequest = TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entries(tsKvEntryList)
                .strategy(TimeseriesSaveRequest.Strategy.CF_ONLY)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();

        ctx.getTelemetryService().saveTimeseries(timeseriesSaveRequest);
    }

    private void processPostAttributesRequest(TbContext ctx, TbMsg msg) {
        List<AttributeKvEntry> newAttributes = new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(msg.getData())));

        if (newAttributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }

        AttributesSaveRequest attributesSaveRequest = AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(msg.getMetaData().getValue(SCOPE)))
                .entries(newAttributes)
                .strategy(AttributesSaveRequest.Strategy.CF_ONLY)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();
        ctx.getTelemetryService().saveAttributes(attributesSaveRequest);
    }

}
