/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.rule.engine.debug;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generator",
        configClazz = TbMsgGeneratorNodeConfiguration.class,
        nodeDescription = "Periodically generates messages",
        nodeDetails = "Generates messages with configurable period. Javascript function used for message generation.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbActionNodeGeneratorConfig",
        icon = "repeat"
)

public class TbMsgGeneratorNode implements TbNode {

    public static final String TB_MSG_GENERATOR_NODE_MSG = "TbMsgGeneratorNodeMsg";

    private TbMsgGeneratorNodeConfiguration config;
    private ScriptEngine jsEngine;
    private long delay;
    private EntityId originatorId;
    private UUID nextTickId;
    private TbMsg prevMsg;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgGeneratorNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds());
        if (!StringUtils.isEmpty(config.getOriginatorId())) {
            originatorId = EntityIdFactory.getByTypeAndUuid(config.getOriginatorType(), config.getOriginatorId());
        } else {
            originatorId = ctx.getSelfId();
        }
        this.jsEngine = ctx.createJsScriptEngine(config.getJsScript(), "prevMsg", "prevMetadata", "prevMsgType");
        sentTickMsg(ctx);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.getType().equals(TB_MSG_GENERATOR_NODE_MSG) && msg.getId().equals(nextTickId)) {
            withCallback(generate(ctx),
                    m -> {ctx.tellNext(m, SUCCESS); sentTickMsg(ctx);},
                    t -> {ctx.tellFailure(msg, t); sentTickMsg(ctx);});
        }
    }

    private void sentTickMsg(TbContext ctx) {
        TbMsg tickMsg = ctx.newMsg(TB_MSG_GENERATOR_NODE_MSG, ctx.getSelfId(), new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, delay);
    }

    private ListenableFuture<TbMsg> generate(TbContext ctx) {
        return ctx.getJsExecutor().executeAsync(() -> {
            if (prevMsg == null) {
                prevMsg = ctx.newMsg( "", originatorId, new TbMsgMetaData(), "{}");
            }
            TbMsg generated = jsEngine.executeGenerate(prevMsg);
            prevMsg = ctx.newMsg(generated.getType(), originatorId, generated.getMetaData(), generated.getData());
            return prevMsg;
        });
    }

    @Override
    public void destroy() {
        prevMsg = null;
        if (jsEngine != null) {
            jsEngine.destroy();
        }
    }
}
