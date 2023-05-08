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
package org.thingsboard.rule.engine.debug;

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
import org.thingsboard.server.common.data.HasEntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generator",
        configClazz = TbMsgGeneratorNodeConfiguration.class,
        nodeDescription = "Periodically generates messages",
        nodeDetails = "Generates messages with configurable period. Javascript function used for message generation.",
        inEnabled = false,
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGeneratorConfig",
        icon = "repeat"
)

public class TbMsgGeneratorNode implements TbNode {

    private static final String TB_MSG_GENERATOR_NODE_MSG = "TbMsgGeneratorNodeMsg";

    private TbMsgGeneratorNodeConfiguration config;
    private ScriptEngine scriptEngine;
    private long delay;
    private long lastScheduledTs;
    private int currentMsgCount;
    private EntityId originatorId;
    private UUID nextTickId;
    private TbMsg prevMsg;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        log.trace("init generator with config {}", configuration);
        this.config = TbNodeUtils.convert(configuration, TbMsgGeneratorNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds());
        this.currentMsgCount = 0;
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
        if (initialized.get() && msg.getType().equals(TB_MSG_GENERATOR_NODE_MSG) && msg.getId().equals(nextTickId)) {
            TbStopWatch sw = TbStopWatch.create();
            withCallback(generate(ctx, msg),
                    m -> {
                        log.trace("onMsg onSuccess callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            if (getEntityByEntityType(ctx, originatorId.getEntityType()) != null) {
                                ctx.enqueueForTellNext(m, SUCCESS);
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

    private HasEntityType getEntityByEntityType(TbContext ctx, EntityType entityType) {
        TenantId tenantId = ctx.getTenantId();
        HasEntityType entity;
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
            case CONVERTER:
                entity = ctx.getPeContext().getConverterService().findConverterById(tenantId, (ConverterId) originatorId);
                break;
            case INTEGRATION:
                entity = ctx.getPeContext().getIntegrationService().findIntegrationById(tenantId, (IntegrationId) originatorId);
                break;
            case SCHEDULER_EVENT:
                entity = ctx.getPeContext().getSchedulerEventService().findSchedulerEventById(tenantId, (SchedulerEventId) originatorId);
                break;
            case BLOB_ENTITY:
                entity = ctx.getPeContext().getBlobEntityService().findBlobEntityById(tenantId, (BlobEntityId) originatorId);
                break;
            case ROLE:
                entity = ctx.getPeContext().getRoleService().findRoleById(tenantId, (RoleId) originatorId);
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
        TbMsg tickMsg = ctx.newMsg(config.getQueueName(), TB_MSG_GENERATOR_NODE_MSG, ctx.getSelfId(),
                msg != null ? msg.getCustomerId() : null, new TbMsgMetaData(), "");
        nextTickId = tickMsg.getId();
        ctx.tellSelf(tickMsg, curDelay);
    }

    private ListenableFuture<TbMsg> generate(TbContext ctx, TbMsg msg) {
        log.trace("generate, config {}", config);
        if (prevMsg == null) {
            prevMsg = ctx.newMsg(config.getQueueName(), "", originatorId, msg.getCustomerId(), new TbMsgMetaData(), "{}");
        }
        if (initialized.get()) {
            ctx.logJsEvalRequest();
            return Futures.transformAsync(scriptEngine.executeGenerateAsync(prevMsg), generated -> {
                log.trace("generate process response, generated {}, config {}", generated, config);
                ctx.logJsEvalResponse();
                prevMsg = ctx.newMsg(config.getQueueName(), generated.getType(), originatorId, msg.getCustomerId(), generated.getMetaData(), generated.getData());
                return Futures.immediateFuture(prevMsg);
            }, MoreExecutors.directExecutor()); //usually it runs on js-executor-remote-callback thread pool
        }
        return Futures.immediateFuture(prevMsg);

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
}
