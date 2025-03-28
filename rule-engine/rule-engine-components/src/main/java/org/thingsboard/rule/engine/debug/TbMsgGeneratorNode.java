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
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
        version = 2,
        hasQueueName = true,
        nodeDescription = "Periodically generates messages",
        nodeDetails = "Generates messages with configurable period. Javascript function used for message generation.",
        inEnabled = false,
        configDirective = "tbActionNodeGeneratorConfig",
        icon = "repeat"
)

public class TbMsgGeneratorNode implements TbNode {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW,
            EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.EDGE, EntityType.RULE_NODE,
            EntityType.CONVERTER, EntityType.INTEGRATION, EntityType.SCHEDULER_EVENT, EntityType.BLOB_ENTITY, EntityType.ROLE, EntityType.ENTITY_GROUP);

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
        this.config = TbNodeUtils.convert(configuration, TbMsgGeneratorNodeConfiguration.class);
        this.delay = TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds());
        this.currentMsgCount = 0;
        this.queueName = ctx.getQueueName();
        if (!supportedEntityTypes.contains(config.getOriginatorType())) {
            throw new TbNodeException("Originator type '" + config.getOriginatorType() + "' is not supported.", true);
        }
        originatorId = getOriginatorId(ctx);
        log.debug("[{}] Initializing generator with config {}", originatorId, configuration);
        updateGeneratorState(ctx);
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        log.debug("[{}] Handling partition change msg: {}", originatorId, msg);
        updateGeneratorState(ctx);
    }

    private void updateGeneratorState(TbContext ctx) {
        log.trace("[{}] Updating generator state, config {}", originatorId, config);
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
        log.trace("[{}] onMsg. Expected msg id: {}, msg: {}, config: {}", originatorId, nextTickId, msg, config);
        if (initialized.get() && msg.isTypeOf(TbMsgType.GENERATOR_NODE_SELF_MSG) && msg.getId().equals(nextTickId)) {
            TbStopWatch sw = TbStopWatch.create();
            withCallback(generate(ctx, msg),
                    m -> {
                        log.trace("onMsg onSuccess callback, took {}ms, config {}, msg {}", sw.stopAndGetTotalTimeMillis(), config, msg);
                        if (initialized.get() && (config.getMsgCount() == TbMsgGeneratorNodeConfiguration.UNLIMITED_MSG_COUNT || currentMsgCount < config.getMsgCount())) {
                            if (getEntityByEntityType(ctx, msg, originatorId.getEntityType()) != null) {
                                ctx.enqueueForTellNext(m, TbNodeConnectionType.SUCCESS);
                                scheduleTickMsg(ctx, msg);
                                currentMsgCount++;
                            } else {
                                ctx.tellFailure(msg, new RuntimeException("Unsupported originator entity type " + originatorId.getEntityType()));
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

    private BaseData getEntityByEntityType(TbContext ctx, TbMsg msg, EntityType entityType) {
        try {
            TenantId tenantId = ctx.getTenantId();
            BaseData entity;
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
                case RULE_NODE:
                    entity = ctx.getRuleChainService().findRuleNodeById(tenantId, (RuleNodeId) originatorId);
                    break;
                default:
                    return null;
            }
            return entity;
        } catch (Exception ignored) {}
        return null;
    }

    private void scheduleTickMsg(TbContext ctx, TbMsg msg) {
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
        log.trace("[{}] Scheduled tick msg with delay {}, msg: {}, config: {}", originatorId, curDelay, tickMsg, config);
    }

    private ListenableFuture<TbMsg> generate(TbContext ctx, TbMsg msg) {
        log.trace("generate, config {}", config);
        if (prevMsg == null) {
            prevMsg = ctx.newMsg(queueName, TbMsg.EMPTY_STRING, originatorId, msg.getCustomerId(), TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        }
        if (initialized.get()) {
            return Futures.transformAsync(scriptEngine.executeGenerateAsync(prevMsg), generated -> {
                log.trace("generate process response, generated {}, config {}", generated, config);
                prevMsg = ctx.newMsg(queueName, generated.getType(), originatorId, msg.getCustomerId(), generated.getMetaData(), generated.getData());
                return Futures.immediateFuture(prevMsg);
            }, MoreExecutors.directExecutor()); //usually it runs on js-executor-remote-callback thread pool
        }
        return Futures.immediateFuture(prevMsg);

    }

    private CustomerId getCustomerIdFromMsg(TbMsg msg) {
        return msg != null ? msg.getCustomerId() : null;
    }

    private EntityId getOriginatorId(TbContext ctx) throws TbNodeException {
        if (EntityType.RULE_NODE.equals(config.getOriginatorType())) {
            return ctx.getSelfId();
        }
        if (EntityType.TENANT.equals(config.getOriginatorType())) {
            return ctx.getTenantId();
        }
        if (StringUtils.isBlank(config.getOriginatorId())) {
            throw new TbNodeException("Originator entity must be selected.", true);
        }
        var entityId = EntityIdFactory.getByTypeAndUuid(config.getOriginatorType(), config.getOriginatorId());
        ctx.checkTenantEntity(entityId);
        return entityId;
    }

    @Override
    public void destroy() {
        log.debug("[{}] Stopping generator", originatorId);
        initialized.set(false);
        prevMsg = null;
        nextTickId = null;
        lastScheduledTs = 0;
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
            case 1:
                String originatorType = "originatorType";
                String originatorId = "originatorId";
                boolean hasType = oldConfiguration.hasNonNull(originatorType);
                boolean hasOriginatorId = oldConfiguration.hasNonNull(originatorId) &&
                        StringUtils.isNotBlank(oldConfiguration.get(originatorId).asText());
                boolean hasOriginatorFields = hasType && hasOriginatorId;
                if (!hasOriginatorFields) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(originatorType, EntityType.RULE_NODE.name());
                }
                String groupOwnerId = "groupOwnerId";
                String groupType = "groupType";
                if (oldConfiguration.has(groupType) || oldConfiguration.has(groupOwnerId)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).remove(List.of(groupOwnerId, groupType));
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }
}
