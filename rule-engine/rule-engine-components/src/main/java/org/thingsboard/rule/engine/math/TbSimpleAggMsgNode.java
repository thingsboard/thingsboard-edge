/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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
package org.thingsboard.rule.engine.math;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "aggregation",
        configClazz = TbSimpleAggMsgNodeConfiguration.class,
        nodeDescription = "Calculates MIN/MAX/AVG/SUM based on the incoming data",
        nodeDetails = "Calculates MIN/MAX/AVG/SUM based on the incoming data"
)
public class TbSimpleAggMsgNode implements TbNode {

    private final JsonParser gson = new JsonParser();
    private TbSimpleAggMsgNodeConfiguration config;
    private long intervalDuration;
    private TbIntervalTable intervals;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSimpleAggMsgNodeConfiguration.class);
        this.intervalDuration = TimeUnit.valueOf(config.getIntervalTimeUnit()).toMillis(config.getIntervalValue());
        this.intervals = new TbIntervalTable(ctx, config, gson);
        //TODO: schedule periodic message to report aggregated telemetry
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        EntityId entityId = msg.getOriginator();
        long ts = extractTs(msg);
        double value = extractValue(msg);
        //TODO: forward message to different server if needed.

        TbIntervalState state = intervals.getByEntityIdAndTs(entityId, ts);
        //TODO: update the current state
        //TODO: make a decision if we want to save the state or later
        //TODO: ack incoming message
    }

    private long extractTs(TbMsg msg) {
        String ts = msg.getMetaData().getValue("ts");
        if (!StringUtils.isEmpty(ts)) {
            return Long.parseLong(ts);
        } else {
            return System.currentTimeMillis();
        }
    }

    private double extractValue(TbMsg msg) {
        JsonElement jsonElement = gson.parse(msg.getData());
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("Incoming message is not a json object!");
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has(config.getValueKey())) {
            throw new IllegalArgumentException("Incoming message does not contain " + config.getValueKey() + "!");
        }
        return jsonObject.get(config.getValueKey()).getAsDouble();
    }

    @Override
    public void destroy() {

    }
}
