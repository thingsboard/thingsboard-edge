/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.debug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.DataConstants.QUEUE_NAME;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generator",
        configClazz = TbMsgGeneratorNodeConfiguration.class,
        version = 1,
        hasQueueName = true,
        nodeDescription = "Periodically generates messages",
        nodeDetails = "Generates messages with configurable period. Javascript function used for message generation.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGeneratorConfig",
        icon = "repeat"
)

public class TbMsgGeneratorNode implements TbNode {

    private TbMsgGeneratorNodeConfiguration config;
    private ScriptEngine scriptEngine;
    private long delay;
    private long lastScheduledTs;
    private int currentMsgCount;
    private EntityId originatorId;
    private UUID nextTickId;
    private TbMsg prevMsg;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private String queueName;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        log.trace("init generator with config {}", configuration);
        this.config = TbNodeUtils.convert(configuration, TbMsgGeneratorNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds());
        this.currentMsgCount = 0;
        this.queueName = ctx.getQueueName();
        if (!StringUtils.isEmpty(config.getOriginatorId())) {
            originatorId = EntityIdFactory.getByTypeAndUuid(config.getOriginatorType(), config.getOriginatorId());
            ctx.checkTenantEntity(originatorId);
        } else {
            originatorId = ctx.getSelfId();
        }
        updateGeneratorState(ctx);
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.trace("onPartitionChangeMsg, PartitionChangeMsg {}, config {}", msg, config);
        updateGeneratorState(ctx);
    }

    private void updateGeneratorState(TbContext ctx) {
        log.trace("updateGeneratorState, config {}", config);
        if (ctx.isLocalEntity(originatorId)) {
            if (initialized.compareAndSet(false, true)) {
                this.scriptEngine = ctx.createScriptEngine(config.getScriptLang(),
                        ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript(), "prevMsg", "prevMetadata", "prevMsgType");
                scheduleTickMsg(ctx, null);
            }
        } else if (initialized.compareAndSet(true, false)) {
            destroy();
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        log.trace("onMsg, config {}, msg {}", config, msg);
        if (initialized.get() && msg.isTypeOf(TbMsgType.GENERATOR_NODE_SELF_MSG) && msg.getId().equals(nextTickId)) {
            TbStopWatch sw = TbStopWatch.create();
            withCallback(generate(ctx, msg),
                    m -> {
                        log.trace("onMsg onSuccess callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            if (getEntityByEntityType(ctx, originatorId.getEntityType()) != null) {
                                ctx.enqueueForTellNext(m, TbNodeConnectionType.SUCCESS);
                                scheduleTickMsg(ctx, msg);
                                currentMsgCount++;
                            }
                        }
                    },
                    t -> {
                        log.trace("onMsg onFailure callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg, t);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            ctx.tellFailure(msg, t);
                            scheduleTickMsg(ctx, msg);
                            currentMsgCount++;
                        }
                    });
        }
    }

    private HasTenantId getEntityByEntityType(TbContext ctx, EntityType entityType) {
        TenantId tenantId = ctx.getTenantId();
        HasTenantId entity;
        switch (entityType) {
            case DEVICE:
                entity = ctx.getDeviceService().findDeviceById(tenantId, (DeviceId) originatorId);
                break;
            case ASSET:
                entity = ctx.getAssetService().findAssetById(tenantId, (AssetId) originatorId);
                break;
            case ENTITY_VIEW:
                entity = ctx.getEntityViewService().findEntityViewById(tenantId, (EntityViewId) originatorId);
                break;
            case TENANT:
                entity = ctx.getTenantService().findTenantById(tenantId);
                break;
            case CUSTOMER:
                entity = ctx.getCustomerService().findCustomerById(tenantId, (CustomerId) originatorId);
                break;
            case DASHBOARD:
                entity = ctx.getDashboardService().findDashboardById(tenantId, (DashboardId) originatorId);
                break;
            case USER:
                entity = ctx.getUserService().findUserById(tenantId, (UserId) originatorId);
                break;
            case EDGE:
                entity = ctx.getEdgeService().findEdgeById(tenantId, (EdgeId) originatorId);
                break;
            default:
                throw new RuntimeException("Unsupported originator entity type");
        }
        return entity;
    }

    private void scheduleTickMsg(TbContext ctx, TbMsg msg) {
        log.trace("scheduleTickMsg, config {}", config);
        long curTs = System.currentTimeMillis();
        if (lastScheduledTs == 0L) {
            lastScheduledTs = curTs;
        }
        lastScheduledTs = lastScheduledTs + delay;
        long curDelay = Math.max(0L, (lastScheduledTs - curTs));
        TbMsg tickMsg = ctx.newMsg(queueName, TbMsgType.GENERATOR_NODE_SELF_MSG, ctx.getSelfId(),
                getCustomerIdFromMsg(msg), TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

    private ListenableFuture<TbMsg> generate(TbContext ctx, TbMsg msg) {
        log.trace("generate, config {}", config);
        if (prevMsg == null) {
            prevMsg = ctx.newMsg(queueName, TbMsg.EMPTY_STRING, originatorId, msg.getCustomerId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        }
        if (initialized.get()) {
            ctx.logJsEvalRequest();
            return Futures.transformAsync(scriptEngine.executeGenerateAsync(prevMsg), generated -> {
                log.trace("generate process response, generated {}, config {}", generated, config);
                ctx.logJsEvalResponse();
                prevMsg = ctx.newMsg(queueName, generated.getType(), originatorId, msg.getCustomerId(), generated.getMetaData(), generated.getData());
                return Futures.immediateFuture(prevMsg);
            }, MoreExecutors.directExecutor()); //usually it runs on js-executor-remote-callback thread pool
        }
        return Futures.immediateFuture(prevMsg);

    }

    private CustomerId getCustomerIdFromMsg(TbMsg msg) {
        return msg != null ? msg.getCustomerId() : null;
    }

    @Override
    public void destroy() {
        log.trace("destroy, config {}", config);
        prevMsg = null;
        if (scriptEngine != null) {
            scriptEngine.destroy();
            scriptEngine = null;
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (oldConfiguration.has(QUEUE_NAME)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(QUEUE_NAME);
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }
}
