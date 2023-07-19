/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.action;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "message count",
        configClazz = TbMsgCountNodeConfiguration.class,
        nodeDescription = "Count incoming messages",
        nodeDetails = "Count incoming messages for specified interval and produces POST_TELEMETRY_REQUEST msg with messages count",
        icon = "functions",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgCountConfig"
)
public class TbMsgCountNode implements TbNode {

    private AtomicLong messagesProcessed = new AtomicLong(0);
    private final Gson gson = new Gson();
    private UUID nextTickId;
    private long delay;
    private String telemetryPrefix;
    private long lastScheduledTs;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        TbMsgCountNodeConfiguration config = TbNodeUtils.convert(configuration, TbMsgCountNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getInterval());
        this.telemetryPrefix = config.getTelemetryPrefix();
        scheduleTickMsg(ctx, null);

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getType().equals(TbMsgType.MSG_COUNT_SELF_MSG.name()) && msg.getId().equals(nextTickId)) {
            JsonObject telemetryJson = new JsonObject();
            telemetryJson.addProperty(this.telemetryPrefix + "_" + ctx.getServiceId(), messagesProcessed.longValue());

            messagesProcessed = new AtomicLong(0);

            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("delta", Long.toString(System.currentTimeMillis() - lastScheduledTs + delay));

            TbMsg tbMsg = TbMsg.newMsg(msg.getQueueName(), TbMsgType.POST_TELEMETRY_REQUEST, ctx.getTenantId(), msg.getCustomerId(), metaData, gson.toJson(telemetryJson));
            ctx.enqueueForTellNext(tbMsg, TbNodeConnectionType.SUCCESS);
            scheduleTickMsg(ctx, tbMsg);
        } else {
            messagesProcessed.incrementAndGet();
            ctx.ack(msg);
        }
    }

    private void scheduleTickMsg(TbContext ctx, TbMsg msg) {
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, ctx.getSelfId(), msg != null ? msg.getCustomerId() : null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

}
