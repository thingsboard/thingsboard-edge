/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.actors.ruleChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Arrays;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineApiUsageStateService;
import org.thingsboard.rule.engine.api.RuleEngineAssetProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.rule.engine.util.TenantIdLoader;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasRuleEngineProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.TbMsgProcessingStackItem;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import org.thingsboard.server.service.script.RuleNodeTbelScriptEngine;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 19.03.18.
 */
@Slf4j
class DefaultTbContext implements TbContext {

    public final static ObjectMapper mapper = new ObjectMapper();

    private final ActorSystemContext mainCtx;
    private final String ruleChainName;
    private final RuleNodeCtx nodeCtx;

    public DefaultTbContext(ActorSystemContext mainCtx, String ruleChainName, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.ruleChainName = ruleChainName;
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
        msg.getCallback().onProcessingEnd(nodeCtx.getSelf().getId());
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId(), relationTypes, msg, th != null ? th.getMessage() : null));
    }

    @Override
    public void tellSelf(TbMsg msg, long delayMs) {
        //TODO: add persistence layer
        scheduleMsgWithDelay(new RuleNodeToSelfMsg(this, msg), delayMs, nodeCtx.getSelfActor());
    }

    @Override
    public void input(TbMsg msg, RuleChainId ruleChainId) {
        msg.pushToStack(nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId());
        nodeCtx.getChainActor().tell(new RuleChainInputMsg(ruleChainId, msg));
    }

    @Override
    public void output(TbMsg msg, String relationType) {
        TbMsgProcessingStackItem item = msg.popFormStack();
        if (item == null) {
            ack(msg);
        } else {
            if (nodeCtx.getSelf().isDebugMode()) {
                mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, relationType);
            }
            nodeCtx.getChainActor().tell(new RuleChainOutputMsg(item.getRuleChainId(), item.getRuleNodeId(), relationType, msg));
        }
    }

    @Override
    public void enqueue(TbMsg tbMsg, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), tbMsg.getOriginator());
        enqueue(tpi, tbMsg, onFailure, onSuccess);
    }

    @Override
    public void enqueue(TbMsg tbMsg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg, queueName);
        enqueue(tpi, tbMsg, onFailure, onSuccess);
    }

    private void enqueue(TopicPartitionInfo tpi, TbMsg tbMsg, Consumer<Throwable> onFailure, Runnable onSuccess) {
        if (!tbMsg.isValid()) {
            log.trace("[{}] Skip invalid message: {}", getTenantId(), tbMsg);
            if (onFailure != null) {
                onFailure.accept(new IllegalArgumentException("Source message is no longer valid!"));
            }
            return;
        }
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(getTenantId().getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, "To Root Rule Chain");
        }
        mainCtx.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), msg, new SimpleTbQueueCallback(onSuccess, onFailure));
    }

    @Override
    public void enqueueForTellFailure(TbMsg tbMsg, String failureMessage) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(TbRelationTypes.FAILURE), failureMessage, null, null);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String relationType) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, null, null);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, Set<String> relationTypes) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, null, null);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, Collections.singleton(relationType), null, onSuccess, onFailure);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg);
        enqueueForTellNext(tpi, tbMsg, relationTypes, null, onSuccess, onFailure);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg, queueName);
        enqueueForTellNext(tpi, queueName, tbMsg, Collections.singleton(relationType), null, onSuccess, onFailure);
    }

    @Override
    public void enqueueForTellNext(TbMsg tbMsg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure) {
        TopicPartitionInfo tpi = resolvePartition(tbMsg, queueName);
        enqueueForTellNext(tpi, queueName, tbMsg, relationTypes, null, onSuccess, onFailure);
    }

    private TopicPartitionInfo resolvePartition(TbMsg tbMsg, String queueName) {
        return mainCtx.resolve(ServiceType.TB_RULE_ENGINE, queueName, getTenantId(), tbMsg.getOriginator());
    }

    private TopicPartitionInfo resolvePartition(TbMsg tbMsg) {
        return resolvePartition(tbMsg, tbMsg.getQueueName());
    }

    private void enqueueForTellNext(TopicPartitionInfo tpi, TbMsg source, Set<String> relationTypes, String failureMessage, Runnable onSuccess, Consumer<Throwable> onFailure) {
        enqueueForTellNext(tpi, source.getQueueName(), source, relationTypes, failureMessage, onSuccess, onFailure);
    }

    private void enqueueForTellNext(TopicPartitionInfo tpi, String queueName, TbMsg source, Set<String> relationTypes, String failureMessage, Runnable onSuccess, Consumer<Throwable> onFailure) {
        if (!source.isValid()) {
            log.trace("[{}] Skip invalid message: {}", getTenantId(), source);
            if (onFailure != null) {
                onFailure.accept(new IllegalArgumentException("Source message is no longer valid!"));
            }
            return;
        }
        RuleChainId ruleChainId = nodeCtx.getSelf().getRuleChainId();
        RuleNodeId ruleNodeId = nodeCtx.getSelf().getId();
        TbMsg tbMsg = TbMsg.newMsg(source, queueName, ruleChainId, ruleNodeId);
        TransportProtos.ToRuleEngineMsg.Builder msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(getTenantId().getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg))
                .addAllRelationTypes(relationTypes);
        if (failureMessage != null) {
            msg.setFailureMessage(failureMessage);
        }
        if (nodeCtx.getSelf().isDebugMode()) {
            relationTypes.forEach(relationType ->
                    mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, relationType, null, failureMessage));
        }
        mainCtx.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), msg.build(), new SimpleTbQueueCallback(onSuccess, onFailure));
    }

    @Override
    public void ack(TbMsg tbMsg) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), tbMsg, "ACK", null);
        }
        tbMsg.getCallback().onProcessingEnd(nodeCtx.getSelf().getId());
        tbMsg.getCallback().onSuccess();
    }

    @Override
    public boolean isLocalEntity(EntityId entityId) {
        return mainCtx.resolve(ServiceType.TB_RULE_ENGINE, getTenantId(), entityId).isMyPartition();
    }

    private void scheduleMsgWithDelay(TbActorMsg msg, long delayInMs, TbActorRef target) {
        mainCtx.scheduleMsgWithDelay(target, msg, delayInMs);
    }

    @Override
    public void tellFailure(TbMsg msg, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, TbRelationTypes.FAILURE, th);
        }
        String failureMessage;
        if (th != null) {
            if (!StringUtils.isEmpty(th.getMessage())) {
                failureMessage = th.getMessage();
            } else {
                failureMessage = th.getClass().getSimpleName();
            }
        } else {
            failureMessage = null;
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getRuleChainId(),
                nodeCtx.getSelf().getId(), Collections.singleton(TbRelationTypes.FAILURE),
                msg, failureMessage));
    }

    public void updateSelf(RuleNode self) {
        nodeCtx.setSelf(self);
    }

    @Override
    public TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

    @Override
    public TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return TbMsg.newMsg(queueName, type, originator, customerId, metaData, data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId());
    }

    @Override
    public TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return TbMsg.transformMsg(origMsg, type, originator, metaData, data);
    }

    public TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId) {
        return entityActionMsg(customer, customer.getId(), ruleNodeId, DataConstants.ENTITY_CREATED);
    }

    public TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId) {
        DeviceProfile deviceProfile = null;
        if (device.getDeviceProfileId() != null) {
            deviceProfile = mainCtx.getDeviceProfileCache().find(device.getDeviceProfileId());
        }
        return entityActionMsg(device, device.getId(), ruleNodeId, DataConstants.ENTITY_CREATED, deviceProfile);
    }

    public TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId) {
        AssetProfile assetProfile = null;
        if (asset.getAssetProfileId() != null) {
            assetProfile = mainCtx.getAssetProfileCache().find(asset.getAssetProfileId());
        }
        return entityActionMsg(asset, asset.getId(), ruleNodeId, DataConstants.ENTITY_CREATED, assetProfile);
    }

    public TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, String action) {
        HasRuleEngineProfile profile = null;
        if (EntityType.DEVICE.equals(alarm.getOriginator().getEntityType())) {
            DeviceId deviceId = new DeviceId(alarm.getOriginator().getId());
            profile = mainCtx.getDeviceProfileCache().get(getTenantId(), deviceId);
        } else if (EntityType.ASSET.equals(alarm.getOriginator().getEntityType())) {
            AssetId assetId = new AssetId(alarm.getOriginator().getId());
            profile = mainCtx.getAssetProfileCache().get(getTenantId(), assetId);
        }
        return entityActionMsg(alarm, alarm.getOriginator(), ruleNodeId, action, profile);
    }

    public TbMsg attributesUpdatedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<AttributeKvEntry> attributes) {
        ObjectNode entityNode = JacksonUtil.newObjectNode();
        if (attributes != null) {
            attributes.forEach(attributeKvEntry -> JacksonUtil.addKvEntry(entityNode, attributeKvEntry));
        }
        return attributesActionMsg(originator, ruleNodeId, scope, DataConstants.ATTRIBUTES_UPDATED, JacksonUtil.toString(entityNode));
    }

    public TbMsg attributesDeletedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<String> keys) {
        ObjectNode entityNode = JacksonUtil.newObjectNode();
        ArrayNode attrsArrayNode = entityNode.putArray("attributes");
        if (keys != null) {
            keys.forEach(attrsArrayNode::add);
        }
        return attributesActionMsg(originator, ruleNodeId, scope, DataConstants.ATTRIBUTES_DELETED, JacksonUtil.toString(entityNode));
    }

    private TbMsg attributesActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, String action, String msgData) {
        TbMsgMetaData tbMsgMetaData = getActionMetaData(ruleNodeId);
        tbMsgMetaData.putValue("scope", scope);
        HasRuleEngineProfile profile = null;
        if (EntityType.DEVICE.equals(originator.getEntityType())) {
            DeviceId deviceId = new DeviceId(originator.getId());
            profile = mainCtx.getDeviceProfileCache().get(getTenantId(), deviceId);
        } else if (EntityType.ASSET.equals(originator.getEntityType())) {
            AssetId assetId = new AssetId(originator.getId());
            profile = mainCtx.getAssetProfileCache().get(getTenantId(), assetId);
        }
        return entityActionMsg(originator, tbMsgMetaData, msgData, action, profile);
    }

    @Override
    public void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId) {
        mainCtx.getClusterService().onEdgeEventUpdate(tenantId, edgeId);
    }

    public <E, I extends EntityId> TbMsg entityActionMsg(E entity, I id, RuleNodeId ruleNodeId, String action) {
        return entityActionMsg(entity, id, ruleNodeId, action, null);
    }

    public <E, I extends EntityId, K extends HasRuleEngineProfile> TbMsg entityActionMsg(E entity, I id, RuleNodeId ruleNodeId, String action, K profile) {
        try {
            return entityActionMsg(id, getActionMetaData(ruleNodeId), mapper.writeValueAsString(mapper.valueToTree(entity)), action, profile);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to process " + id.getEntityType().name().toLowerCase() + " " + action + " msg: " + e);
        }
    }

    private <I extends EntityId, K extends HasRuleEngineProfile> TbMsg entityActionMsg(I id, TbMsgMetaData msgMetaData, String msgData, String action, K profile) {
        String defaultQueueName = null;
        RuleChainId defaultRuleChainId = null;
        if (profile != null) {
            defaultQueueName = profile.getDefaultQueueName();
            defaultRuleChainId = profile.getDefaultRuleChainId();
        }
        return TbMsg.newMsg(defaultQueueName, action, id, msgMetaData, msgData, defaultRuleChainId, null);
    }

    @Override
    public RuleNodeId getSelfId() {
        return nodeCtx.getSelf().getId();
    }

    @Override
    public RuleNode getSelf() {
        return nodeCtx.getSelf();
    }

    @Override
    public String getRuleChainName() {
        return ruleChainName;
    }

    @Override
    public TenantId getTenantId() {
        return nodeCtx.getTenantId();
    }

    @Override
    public ListeningExecutor getMailExecutor() {
        return mainCtx.getMailExecutor();
    }

    @Override
    public ListeningExecutor getSmsExecutor() {
        return mainCtx.getSmsExecutor();
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
    @Deprecated
    public ScriptEngine createJsScriptEngine(String script, String... argNames) {
        return new RuleNodeJsScriptEngine(getTenantId(), mainCtx.getJsInvokeService(), script, argNames);
    }

    private ScriptEngine createTbelScriptEngine(String script, String... argNames) {
        if (mainCtx.getTbelInvokeService() == null) {
            throw new RuntimeException("TBEL execution is disabled!");
        }
        return new RuleNodeTbelScriptEngine(getTenantId(), mainCtx.getTbelInvokeService(), script, argNames);
    }

    @Override
    public ScriptEngine createScriptEngine(ScriptLanguage scriptLang, String script, String... argNames) {
        if (scriptLang == null) {
            scriptLang = ScriptLanguage.JS;
        }
        if (StringUtils.isBlank(script)) {
            throw new RuntimeException(scriptLang.name() + " script is blank!");
        }
        switch (scriptLang) {
            case JS:
                return createJsScriptEngine(script, argNames);
            case TBEL:
                if (Arrays.isNullOrEmpty(argNames)) {
                    return createTbelScriptEngine(script, "msg", "metadata", "msgType");
                } else {
                    return createTbelScriptEngine(script, argNames);
                }
            default:
                throw new RuntimeException("Unsupported script language: " + scriptLang.name());
        }
    }

    @Override
    public void logJsEvalRequest() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeStats().incrementRequests();
        }
    }

    @Override
    public void logJsEvalResponse() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeStats().incrementResponses();
        }
    }

    @Override
    public void logJsEvalFailure() {
        if (mainCtx.isStatisticsEnabled()) {
            mainCtx.getJsInvokeStats().incrementFailures();
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
    public DeviceProfileService getDeviceProfileService() {
        return mainCtx.getDeviceProfileService();
    }

    @Override
    public AssetProfileService getAssetProfileService() {
        return mainCtx.getAssetProfileService();
    }

    @Override
    public DeviceCredentialsService getDeviceCredentialsService() {
        return mainCtx.getDeviceCredentialsService();
    }

    @Override
    public TbClusterService getClusterService() {
        return mainCtx.getClusterService();
    }

    @Override
    public DashboardService getDashboardService() {
        return mainCtx.getDashboardService();
    }

    @Override
    public RuleEngineAlarmService getAlarmService() {
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
    public ResourceService getResourceService() {
        return mainCtx.getResourceService();
    }

    @Override
    public OtaPackageService getOtaPackageService() {
        return mainCtx.getOtaPackageService();
    }

    @Override
    public RuleEngineDeviceProfileCache getDeviceProfileCache() {
        return mainCtx.getDeviceProfileCache();
    }

    @Override
    public RuleEngineAssetProfileCache getAssetProfileCache() {
        return mainCtx.getAssetProfileCache();
    }

    @Override
    public EdgeService getEdgeService() {
        return mainCtx.getEdgeService();
    }

    @Override
    public EdgeEventService getEdgeEventService() {
        return mainCtx.getEdgeEventService();
    }

    @Override
    public QueueService getQueueService() {
        return mainCtx.getQueueService();
    }

    @Override
    public CloudEventService getCloudEventService() {
        return mainCtx.getCloudEventService();
    }

    @Override
    public EventLoopGroup getSharedEventLoop() {
        return mainCtx.getSharedEventLoopGroupService().getSharedEventLoopGroup();
    }

    @Override
    public MailService getMailService(boolean isSystem) {
        if (!isSystem || mainCtx.isAllowSystemMailService()) {
            return mainCtx.getMailService();
        } else {
            throw new RuntimeException("Access to System Mail Service is forbidden!");
        }
    }

    @Override
    public SmsService getSmsService() {
        if (mainCtx.isAllowSystemSmsService()) {
            return mainCtx.getSmsService();
        } else {
            throw new RuntimeException("Access to System SMS Service is forbidden!");
        }
    }

    @Override
    public SmsSenderFactory getSmsSenderFactory() {
        return mainCtx.getSmsSenderFactory();
    }

    @Override
    public RuleEngineRpcService getRpcService() {
        return mainCtx.getTbRuleEngineDeviceRpcService();
    }

    @Override
    public CassandraCluster getCassandraCluster() {
        return mainCtx.getCassandraCluster();
    }

    @Override
    public TbResultSetFuture submitCassandraReadTask(CassandraStatementTask task) {
        return mainCtx.getCassandraBufferedRateReadExecutor().submit(task);
    }

    @Override
    public TbResultSetFuture submitCassandraWriteTask(CassandraStatementTask task) {
        return mainCtx.getCassandraBufferedRateWriteExecutor().submit(task);
    }

    @Override
    public PageData<RuleNodeState> findRuleNodeStates(PageLink pageLink) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}] Fetch Rule Node States.", getTenantId(), getSelfId());
        }
        return mainCtx.getRuleNodeStateService().findByRuleNodeId(getTenantId(), getSelfId(), pageLink);
    }

    @Override
    public RuleNodeState findRuleNodeStateForEntity(EntityId entityId) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Fetch Rule Node State for entity.", getTenantId(), getSelfId(), entityId);
        }
        return mainCtx.getRuleNodeStateService().findByRuleNodeIdAndEntityId(getTenantId(), getSelfId(), entityId);
    }

    @Override
    public RuleNodeState saveRuleNodeState(RuleNodeState state) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Persist Rule Node State for entity: {}", getTenantId(), getSelfId(), state.getEntityId(), state.getStateData());
        }
        state.setRuleNodeId(getSelfId());
        return mainCtx.getRuleNodeStateService().save(getTenantId(), state);
    }

    @Override
    public void clearRuleNodeStates() {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}] Going to clear rule node states", getTenantId(), getSelfId());
        }
        mainCtx.getRuleNodeStateService().removeByRuleNodeId(getTenantId(), getSelfId());
    }

    @Override
    public void removeRuleNodeStateForEntity(EntityId entityId) {
        if (log.isDebugEnabled()) {
            log.debug("[{}][{}][{}] Remove Rule Node State for entity.", getTenantId(), getSelfId(), entityId);
        }
        mainCtx.getRuleNodeStateService().removeByRuleNodeIdAndEntityId(getTenantId(), getSelfId(), entityId);
    }

    @Override
    public void addTenantProfileListener(Consumer<TenantProfile> listener) {
        mainCtx.getTenantProfileCache().addListener(getTenantId(), getSelfId(), listener);
    }

    @Override
    public void addDeviceProfileListeners(Consumer<DeviceProfile> profileListener, BiConsumer<DeviceId, DeviceProfile> deviceListener) {
        mainCtx.getDeviceProfileCache().addListener(getTenantId(), getSelfId(), profileListener, deviceListener);
    }

    @Override
    public void addAssetProfileListeners(Consumer<AssetProfile> profileListener, BiConsumer<AssetId, AssetProfile> assetListener) {
        mainCtx.getAssetProfileCache().addListener(getTenantId(), getSelfId(), profileListener, assetListener);
    }

    @Override
    public void removeListeners() {
        mainCtx.getDeviceProfileCache().removeListener(getTenantId(), getSelfId());
        mainCtx.getAssetProfileCache().removeListener(getTenantId(), getSelfId());
        mainCtx.getTenantProfileCache().removeListener(getTenantId(), getSelfId());
    }

    @Override
    public TenantProfile getTenantProfile() {
        return mainCtx.getTenantProfileCache().get(getTenantId());
    }

    @Override
    public WidgetsBundleService getWidgetBundleService() {
        return mainCtx.getWidgetsBundleService();
    }

    @Override
    public WidgetTypeService getWidgetTypeService() {
        return mainCtx.getWidgetTypeService();
    }

    @Override
    public RuleEngineApiUsageStateService getRuleEngineApiUsageStateService() {
        return mainCtx.getApiUsageStateService();
    }

    private TbMsgMetaData getActionMetaData(RuleNodeId ruleNodeId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ruleNodeId", ruleNodeId.toString());
        return metaData;
    }

    @Override
    public void checkTenantEntity(EntityId entityId) {
        if (!this.getTenantId().equals(TenantIdLoader.findTenantId(this, entityId))) {
            throw new RuntimeException("Entity with id: '" + entityId + "' specified in the configuration doesn't belong to the current tenant.");
        }
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
