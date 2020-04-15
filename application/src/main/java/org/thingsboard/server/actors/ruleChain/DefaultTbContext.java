/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.actors.ruleChain;

import akka.actor.ActorRef;
import com.datastax.driver.core.ResultSetFuture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.integration.api.data.DefaultIntegrationDownlinkMsg;
import org.thingsboard.js.api.JsScriptType;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.ReportService;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import scala.concurrent.duration.Duration;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 19.03.18.
 */
@Slf4j
class DefaultTbContext implements TbContext, TbPeContext {

    public final static ObjectMapper mapper = new ObjectMapper();

    private final ActorSystemContext mainCtx;
    private final RuleNodeCtx nodeCtx;

    public DefaultTbContext(ActorSystemContext mainCtx, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.nodeCtx = nodeCtx;
    }

    @Override
    public void tellSuccess(TbMsg msg) {
        tellNext(msg, Collections.singleton(TbRelationTypes.SUCCESS), null);
    }

    @Override
    public void tellNext(TbMsg msg, String relationType) {
        tellNext(msg, Collections.singleton(relationType), null);
    }

    @Override
    public void tellNext(TbMsg msg, Set<String> relationTypes) {
        tellNext(msg, relationTypes, null);
    }

    private void tellNext(TbMsg msg, Set<String> relationTypes, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            relationTypes.forEach(relationType -> mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, relationType, th));
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), relationTypes, msg, th != null ? th.getMessage() : null), nodeCtx.getSelfActor());
    }

    @Override
    public void tellSelf(TbMsg msg, long delayMs) {
        //TODO: add persistence layer
        scheduleMsgWithDelay(new RuleNodeToSelfMsg(msg), delayMs, nodeCtx.getSelfActor());
    }

    private void scheduleMsgWithDelay(Object msg, long delayInMs, ActorRef target) {
        mainCtx.getScheduler().scheduleOnce(Duration.create(delayInMs, TimeUnit.MILLISECONDS), target, msg, mainCtx.getActorSystem().dispatcher(), nodeCtx.getSelfActor());
    }

    @Override
    public void enqueue(TbMsg tbMsg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueue(tpi, tbMsg, onFailure, onSuccess);
    }

    @Override
    public void enqueue(TbMsg tbMsg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, queueName, getTenantId(), tbMsg.getOriginator());
        enqueue(tpi, tbMsg, onFailure, onSuccess);
    }

    private void enqueue(TopicPartitionInfo tpi, TbMsg tbMsg, Consumer<Throwable> onFailure, Runnable onSuccess) {
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(getTenantId().getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        mainCtx.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(),  msg, new SimpleTbQueueCallback(onSuccess, onFailure));
    }

    @Override
    public void enqueueForTellFailure(TbMsg tbMsg, String failureMessage) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(TbRelationTypes.FAILURE), failureMessage, null, null);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String relationType) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, null, null);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, Set<String> relationTypes) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, null, null);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, onSuccess, onFailure);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, onSuccess, onFailure);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, queueName, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, onSuccess, onFailure);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, queueName, getTenantId(), tbMsg.getOriginator());
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, onSuccess, onFailure);
    }

    private void enqueueForTellNext(TopicPartitionInfo tpi, TbMsg tbMsg, Set<String> relationTypes, String failureMessage, Runnable onSuccess, Consumer<Throwable> onFailure) {
        RuleChainId ruleChainId = nodeCtx.getSelf().getRuleChainId();
        RuleNodeId ruleNodeId = nodeCtx.getSelf().getId();
        tbMsg = TbMsg.newMsg(tbMsg, ruleChainId, ruleNodeId);
        TransportProtos.ToRuleEngineMsg.Builder msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(getTenantId().getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg))
                .addAllRelationTypes(relationTypes);
        if (failureMessage != null) {
            msg.setFailureMessage(failureMessage);
        }
        mainCtx.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(),  msg.build(), new SimpleTbQueueCallback(onSuccess, onFailure));
    }

    @Override
    public void ack(TbMsg tbMsg) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, "ACK", null);
        }
        tbMsg.getCallback().onSuccess();
    }

    @Override
    public boolean isLocalEntity(EntityId entityId) {
        return mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), entityId).isMyPartition();
    }

    @Override
    public ScriptEngine createAttributesJsScriptEngine(String script) {
        return new RuleNodeJsScriptEngine(mainCtx.getJsSandbox(), nodeCtx.getSelf().getId(), JsScriptType.ATTRIBUTES_SCRIPT, script);
    }

    @Override
    public void tellFailure(TbMsg msg, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, TbRelationTypes.FAILURE, th);
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), Collections.singleton(TbRelationTypes.FAILURE),
                msg, th != null ? th.getMessage() : null), nodeCtx.getSelfActor());
    }

    public void updateSelf(RuleNode self) {
        nodeCtx.setSelf(self);
    }

    @Override
    public TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return TbMsg.newMsg(type, originator, metaData, data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId());
    }

    @Override
    public TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return TbMsg.transformMsg(origMsg, type, originator, metaData, data);
    }

    public TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId) {
        return entityCreatedMsg(customer, customer.getId(), ruleNodeId);
    }

    public TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId) {
        return entityCreatedMsg(device, device.getId(), ruleNodeId);
    }

    public TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId) {
        return entityCreatedMsg(asset, asset.getId(), ruleNodeId);
    }

    public TbMsg alarmCreatedMsg(Alarm alarm, RuleNodeId ruleNodeId) {
        return entityCreatedMsg(alarm, alarm.getId(), ruleNodeId);
    }

    public <E, I extends EntityId> TbMsg entityCreatedMsg(E entity, I id, RuleNodeId ruleNodeId) {
        try {
            return TbMsg.newMsg(DataConstants.ENTITY_CREATED, id, getActionMetaData(ruleNodeId), mapper.writeValueAsString(mapper.valueToTree(entity)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to process " + id.getEntityType().name().toLowerCase() + " created msg: " + e);
        }
    }

    @Override
    public RuleNodeId getSelfId() {
        return nodeCtx.getSelf().getId();
    }

    @Override
    public TenantId getTenantId() {
        return nodeCtx.getTenantId();
    }

    @Override
    public ListeningExecutor getJsExecutor() {
        return mainCtx.getJsExecutor();
    }

    @Override
    public ListeningExecutor getMailExecutor() {
        return mainCtx.getMailExecutor();
    }

    @Override
    public ListeningExecutor getDbCallbackExecutor() {
        return mainCtx.getDbCallbackExecutor();
    }

    @Override
    public ListeningExecutor getExternalCallExecutor() {
        return mainCtx.getExternalCallExecutorService();
    }

    @Override
    public ScriptEngine createJsScriptEngine(String script, String... argNames) {
        return new RuleNodeJsScriptEngine(mainCtx.getJsSandbox(), nodeCtx.getSelf().getId(), script, argNames);
    }

    @Override
    public EventService getEventService() {
        return mainCtx.getEventService();
    }

    @Override
    public void logJsEvalRequest() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeRequestsCount().incrementAndGet();
        }
    }

    @Override
    public void logJsEvalResponse() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeResponsesCount().incrementAndGet();
        }
    }

    @Override
    public void logJsEvalFailure() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeFailuresCount().incrementAndGet();
        }
    }

    @Override
    public String getServiceId() {
        return mainCtx.getServiceInfoProvider().getServiceId();
    }

    @Override
    public AttributesService getAttributesService() {
        return mainCtx.getAttributesService();
    }

    @Override
    public CustomerService getCustomerService() {
        return mainCtx.getCustomerService();
    }

    @Override
    public TenantService getTenantService() {
        return mainCtx.getTenantService();
    }

    @Override
    public UserService getUserService() {
        return mainCtx.getUserService();
    }

    @Override
    public AssetService getAssetService() {
        return mainCtx.getAssetService();
    }

    @Override
    public DeviceService getDeviceService() {
        return mainCtx.getDeviceService();
    }

    @Override
    public DashboardService getDashboardService() {
        return mainCtx.getDashboardService();
    }

    @Override
    public AlarmService getAlarmService() {
        return mainCtx.getAlarmService();
    }

    @Override
    public RuleChainService getRuleChainService() {
        return mainCtx.getRuleChainService();
    }

    @Override
    public TimeseriesService getTimeseriesService() {
        return mainCtx.getTsService();
    }

    @Override
    public RuleEngineTelemetryService getTelemetryService() {
        return mainCtx.getTsSubService();
    }

    @Override
    public RelationService getRelationService() {
        return mainCtx.getRelationService();
    }

    @Override
    public EntityViewService getEntityViewService() {
        return mainCtx.getEntityViewService();
    }

    @Override
    public EventLoopGroup getSharedEventLoop() {
        return mainCtx.getSharedEventLoopGroupService().getSharedEventLoopGroup();
    }

    @Override
    public MailService getMailService() {
        return mainCtx.getMailService();
    }

    @Override
    public RuleEngineRpcService getRpcService() {
        return mainCtx.getTbRuleEngineDeviceRpcService();
    }

    @Override
    public TbPeContext getPeContext() {
        return this;
    }

    @Override
    public IntegrationService getIntegrationService() {
        return mainCtx.getIntegrationService();
    }

    @Override
    public EntityGroupService getEntityGroupService() {
        return mainCtx.getEntityGroupService();
    }

    @Override
    public ReportService getReportService() {
        return mainCtx.getReportService();
    }

    @Override
    public BlobEntityService getBlobEntityService() {
        return mainCtx.getBlobEntityService();
    }

    @Override
    public GroupPermissionService getGroupPermissionService() {
        return mainCtx.getGroupPermissionService();
    }

    @Override
    public RoleService getRoleService() {
        return mainCtx.getRoleService();
    }

    @Override
    public EntityId getOwner(TenantId tenantId, EntityId entityId) {
        return mainCtx.getOwnersCacheService().getOwner(tenantId, entityId);
    }

    @Override
    public void clearOwners(EntityId entityId) {
        mainCtx.getOwnersCacheService().clearOwners(entityId);
    }

    @Override
    public Set<EntityId> getChildOwners(TenantId tenantId, EntityId parentOwnerId) {
        return mainCtx.getOwnersCacheService().getChildOwners(tenantId, parentOwnerId);
    }

    @Override
    public void changeDashboardOwner(TenantId tenantId, EntityId targetOwnerId, Dashboard entity) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeDashboardOwner(tenantId, targetOwnerId, entity);
    }

    @Override
    public void changeUserOwner(TenantId tenantId, EntityId targetOwnerId, User entity) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeUserOwner(tenantId, targetOwnerId, entity);
    }

    @Override
    public void changeCustomerOwner(TenantId tenantId, EntityId targetOwnerId, Customer entity) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeCustomerOwner(tenantId, targetOwnerId, entity);
    }

    @Override
    public void changeEntityViewOwner(TenantId tenantId, EntityId targetOwnerId, EntityView entity) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeEntityViewOwner(tenantId, targetOwnerId, entity);
    }

    @Override
    public void changeAssetOwner(TenantId tenantId, EntityId targetOwnerId, Asset entity) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeAssetOwner(tenantId, targetOwnerId, entity);
    }

    @Override
    public void changeDeviceOwner(TenantId tenantId, EntityId targetOwnerId, Device entity) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeDeviceOwner(tenantId, targetOwnerId, entity);
    }

    @Override
    public void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId, EntityType entityType) throws ThingsboardException {
        mainCtx.getOwnersCacheService().changeEntityOwner(tenantId, targetOwnerId, entityId, entityType);
    }

    @Override
    public void pushToIntegration(IntegrationId integrationId, TbMsg msg, FutureCallback<Void> callback) {
        boolean restApiCall = msg.getType().equals(DataConstants.RPC_CALL_FROM_SERVER_TO_DEVICE);
        UUID requestUUID;
        if (restApiCall) {
            String tmp = msg.getMetaData().getValue("requestUUID");
            requestUUID = !StringUtils.isEmpty(tmp) ? UUID.fromString(tmp) : UUID.randomUUID();
            tmp = msg.getMetaData().getValue("oneway");
            boolean oneway = !StringUtils.isEmpty(tmp) && Boolean.parseBoolean(tmp);
            if (!oneway) {
                throw new RuntimeException("Only oneway RPC calls are supported in the integration!");
            }
        } else {
            requestUUID = null;
        }

        mainCtx.getPlatformIntegrationService().onDownlinkMsg(new DefaultIntegrationDownlinkMsg(getTenantId(), integrationId, msg), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void aVoid) {
                if (restApiCall) {
                    FromDeviceRpcResponse response = new FromDeviceRpcResponse(requestUUID, null, null);
                    mainCtx.getTbRuleEngineDeviceRpcService().processRpcResponseFromDevice(response);
                }
                callback.onSuccess(aVoid);
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (restApiCall) {
                    FromDeviceRpcResponse response = new FromDeviceRpcResponse(requestUUID, null, RpcError.INTERNAL);
                    mainCtx.getTbRuleEngineDeviceRpcService().processRpcResponseFromDevice(response);
                }
                callback.onFailure(throwable);
            }
        });
    }

    @Override
    public CassandraCluster getCassandraCluster() {
        return mainCtx.getCassandraCluster();
    }

    @Override
    public ResultSetFuture submitCassandraTask(CassandraStatementTask task) {
        return mainCtx.getCassandraBufferedRateExecutor().submit(task);
    }

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return mainCtx.getRedisTemplate();
    }


    private TbMsgMetaData getActionMetaData(RuleNodeId ruleNodeId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ruleNodeId", ruleNodeId.toString());
        return metaData;
    }

    private class SimpleTbQueueCallback implements TbQueueCallback {
        private final Runnable onSuccess;
        private final Consumer<Throwable> onFailure;

        public SimpleTbQueueCallback(Runnable onSuccess, Consumer<Throwable> onFailure) {
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (onFailure != null) {
                onFailure.accept(t);
            } else {
                log.debug("[{}] Failed to put item into queue", nodeCtx.getTenantId(), t);
            }
        }
    }
}
