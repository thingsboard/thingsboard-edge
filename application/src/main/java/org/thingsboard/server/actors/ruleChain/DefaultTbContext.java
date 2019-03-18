/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.FutureCallback;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.*;
import io.netty.channel.EventLoopGroup;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.ListeningExecutor;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.RuleChainTransactionService;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.cluster.ServerType;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.integration.msg.DefaultIntegrationDownlinkMsg;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.service.script.JsScriptType;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import scala.concurrent.duration.Duration;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 19.03.18.
 */
class DefaultTbContext implements TbContext, TbPeContext {

    private final ActorSystemContext mainCtx;
    private final RuleNodeCtx nodeCtx;

    public DefaultTbContext(ActorSystemContext mainCtx, RuleNodeCtx nodeCtx) {
        this.mainCtx = mainCtx;
        this.nodeCtx = nodeCtx;
    }

    @Override
    public void tellNext(TbMsg msg, String relationType) {
        tellNext(msg, Collections.singleton(relationType), null);
    }

    @Override
    public void tellNext(TbMsg msg, Set<String> relationTypes) {
        tellNext(msg, relationTypes, null);
    }

    @Override
    public void tellNext(TbMsg msg, String relationType, Throwable th) {
        tellNext(msg, Collections.singleton(relationType), th);
    }

    private void tellNext(TbMsg msg, Set<String> relationTypes, Throwable th) {
        if (nodeCtx.getSelf().isDebugMode()) {
            relationTypes.forEach(relationType -> mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, relationType, th));
        }
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), relationTypes, msg), nodeCtx.getSelfActor());
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
    public void ack(TbMsg msg) {
        if (nodeCtx.getSelf().isDebugMode()) {
            mainCtx.persistDebugOutput(nodeCtx.getTenantId(), nodeCtx.getSelf().getId(), msg, "ACK", null);
        }
//        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainAckMsg(nodeCtx.getSelf().getId(), msg), nodeCtx.getSelfActor());
    }

    @Override
    public boolean isLocalEntity(EntityId entityId) {
        Optional<ServerAddress> address = mainCtx.getRoutingService().resolveById(entityId);
        return !address.isPresent();
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
        nodeCtx.getChainActor().tell(new RuleNodeToRuleChainTellNextMsg(nodeCtx.getSelf().getId(), Collections.singleton(TbRelationTypes.FAILURE), msg), nodeCtx.getSelfActor());
    }

    @Override
    public void updateSelf(RuleNode self) {
        nodeCtx.setSelf(self);
    }

    @Override
    public TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(UUIDs.timeBased(), type, originator, metaData.copy(), data, nodeCtx.getSelf().getRuleChainId(), nodeCtx.getSelf().getId(), mainCtx.getQueuePartitionId());
    }

    @Override
    public TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(origMsg.getId(), type, originator, metaData.copy(), origMsg.getDataType(), data, origMsg.getTransactionData(), origMsg.getRuleChainId(), origMsg.getRuleNodeId(), mainCtx.getQueuePartitionId());
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
    public String getNodeId() {
        return mainCtx.getNodeIdProvider().getNodeId();
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
    public RuleChainTransactionService getRuleChainTransactionService() {
        return mainCtx.getRuleChainTransactionService();
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
        return new RuleEngineRpcService() {
            @Override
            public void sendRpcReply(DeviceId deviceId, int requestId, String body) {
                mainCtx.getDeviceRpcService().sendReplyToRpcCallFromDevice(nodeCtx.getTenantId(), deviceId, requestId, body);
            }

            @Override
            public void sendRpcRequest(RuleEngineDeviceRpcRequest src, Consumer<RuleEngineDeviceRpcResponse> consumer) {
                ToDeviceRpcRequest request = new ToDeviceRpcRequest(src.getRequestUUID(), nodeCtx.getTenantId(), src.getDeviceId(),
                        src.isOneway(), src.getExpirationTime(), new ToDeviceRpcRequestBody(src.getMethod(), src.getBody()));
                mainCtx.getDeviceRpcService().forwardServerSideRPCRequestToDeviceActor(request, response -> {
                    if (src.isRestApiCall()) {
                        ServerAddress requestOriginAddress;
                        if (!StringUtils.isEmpty(src.getOriginHost())) {
                            requestOriginAddress = new ServerAddress(src.getOriginHost(), src.getOriginPort(), ServerType.CORE);
                        } else {
                            requestOriginAddress = mainCtx.getRoutingService().getCurrentServer();
                        }
                        mainCtx.getDeviceRpcService().processResponseToServerSideRPCRequestFromRuleEngine(requestOriginAddress, response);
                    }
                    consumer.accept(RuleEngineDeviceRpcResponse.builder()
                            .deviceId(src.getDeviceId())
                            .requestId(src.getRequestId())
                            .error(response.getError())
                            .response(response.getResponse())
                            .build());
                });
            }

            @Override
            public void sendRestApiCallReply(UUID requestId, TbMsg msg) {
                mainCtx.getRuleEngineCallService().processRestAPICallResponseFromRuleEngine(requestId, msg);
            }
        };
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
    public EntityId getOwner(TenantId tenantId, EntityId entityId) {
        return null;
    }

    @Override
    public void clearOwners(EntityId entityId) {

    }

    @Override
    public Set<EntityId> getChildOwners(TenantId tenantId, EntityId parentOwnerId) {
        return null;
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
    public void pushToIntegration(IntegrationId integrationId, TbMsg msg, FutureCallback<Void> callback) {
        boolean restApiCall = msg.getType().equals(DataConstants.RPC_CALL_FROM_SERVER_TO_DEVICE);
        UUID requestUUID;
        if (restApiCall) {
            String tmp = msg.getMetaData().getValue("requestUUID");
            requestUUID = !StringUtils.isEmpty(tmp) ? UUID.fromString(tmp) : UUIDs.timeBased();
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
                    mainCtx.getDeviceRpcService().processResponseToServerSideRPCRequestFromRuleEngine(mainCtx.getRoutingService().getCurrentServer(), response);
                }
                callback.onSuccess(aVoid);
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (restApiCall) {
                    FromDeviceRpcResponse response = new FromDeviceRpcResponse(requestUUID, null, RpcError.INTERNAL);
                    mainCtx.getDeviceRpcService().processResponseToServerSideRPCRequestFromRuleEngine(mainCtx.getRoutingService().getCurrentServer(), response);
                }
                callback.onFailure(throwable);
            }
        });
    }

}
