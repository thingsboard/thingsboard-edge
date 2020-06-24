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
package org.thingsboard.server.service.edge.rpc.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.CustomTranslationProto;
import org.thingsboard.server.gen.edge.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.DownlinkMsg;
import org.thingsboard.server.gen.edge.EntityDataProto;
import org.thingsboard.server.gen.edge.EntityGroupEntitiesRequestMsg;
import org.thingsboard.server.gen.edge.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.EntityUpdateMsg;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.ResponseMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.UserUpdateMsg;
import org.thingsboard.server.gen.edge.WhiteLabelingParamsProto;
import org.thingsboard.server.service.edge.rpc.constructor.AssetUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomTranslationProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityGroupUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.SchedulerEventUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WhiteLabelingParamsProtoConstructor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultSyncEdgeService implements SyncEdgeService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private RuleChainUpdateMsgConstructor ruleChainUpdateMsgConstructor;

    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Autowired
    private AssetUpdateMsgConstructor assetUpdateMsgConstructor;

    @Autowired
    private EntityViewUpdateMsgConstructor entityViewUpdateMsgConstructor;

    @Autowired
    private DashboardUpdateMsgConstructor dashboardUpdateMsgConstructor;

    @Autowired
    private UserUpdateMsgConstructor userUpdateMsgConstructor;

    @Autowired
    private RelationUpdateMsgConstructor relationUpdateMsgConstructor;

    @Autowired
    private EntityDataMsgConstructor entityDataMsgConstructor;

    @Autowired
    private SchedulerEventUpdateMsgConstructor schedulerEventUpdateMsgConstructor;

    @Autowired
    private EntityGroupUpdateMsgConstructor entityGroupUpdateMsgConstructor;

    @Autowired
    private WhiteLabelingParamsProtoConstructor whiteLabelingParamsProtoConstructor;

    @Autowired
    private CustomTranslationProtoConstructor customTranslationProtoConstructor;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public void sync(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        syncLoginWhiteLabeling(edge, outputStream);
        syncWhiteLabeling(edge, outputStream);
        syncCustomTranslation(edge, outputStream);
        syncRuleChains(edge, outputStream);
        syncEntityGroups(edge, outputStream);
        syncSchedulerEvents(edge, outputStream);
    }

    private void syncRuleChains(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<TimePageData<RuleChain>> future =
                    ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<RuleChain>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<RuleChain> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (RuleChain ruleChain : pageData.getData()) {
                            RuleChainUpdateMsg ruleChainUpdateMsg =
                                    ruleChainUpdateMsgConstructor.constructRuleChainUpdatedMsg(
                                            edge.getRootRuleChainId(),
                                            UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                            ruleChain);
                            EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                    .setRuleChainUpdateMsg(ruleChainUpdateMsg)
                                    .build();
                            outputStream.onNext(ResponseMsg.newBuilder()
                                    .setEntityUpdateMsg(entityUpdateMsg)
                                    .build());
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge rule chain(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on sync!", e);
        }
    }

    private void syncEntityGroups(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            List<ListenableFuture<List<EntityGroup>>> futures = new ArrayList<>();
            futures.add(entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.DEVICE));
            futures.add(entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.ASSET));
            futures.add(entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.ENTITY_VIEW));
            futures.add(entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.DASHBOARD));
            futures.add(entityGroupService.findEdgeEntityGroupsByType(edge.getTenantId(), edge.getId(), EntityType.USER));

            ListenableFuture<List<List<EntityGroup>>> listFuture = Futures.allAsList(futures);

            Futures.addCallback(listFuture, new FutureCallback<List<List<EntityGroup>>>() {
                @Override
                public void onSuccess(@Nullable List<List<EntityGroup>> result) {
                    if (result != null && !result.isEmpty()) {
                        for (List<EntityGroup> entityGroups : result) {
                            if (entityGroups != null && !entityGroups.isEmpty()) {
                                for (EntityGroup entityGroup : entityGroups) {
                                    EntityGroupUpdateMsg entityGroupUpdateMsg =
                                            entityGroupUpdateMsgConstructor.constructEntityGroupUpdatedMsg(
                                                    UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                                    entityGroup);
                                    EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                            .setEntityGroupUpdateMsg(entityGroupUpdateMsg)
                                            .build();
                                    outputStream.onNext(ResponseMsg.newBuilder()
                                            .setEntityUpdateMsg(entityUpdateMsg)
                                            .build());
                                }
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge entity groups(s) on sync!", t);
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge entity groups(s) on sync!", e);
        }
    }

    private void syncSchedulerEvents(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            ListenableFuture<List<SchedulerEvent>> schedulerEventsFuture =
                    schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId());
            Futures.transform(schedulerEventsFuture, schedulerEvents -> {
                if (schedulerEvents != null && !schedulerEvents.isEmpty()) {
                    log.trace("[{}] [{}] scheduler events(s) are going to be pushed to edge.", edge.getId(), schedulerEvents.size());
                    for (SchedulerEvent schedulerEvent : schedulerEvents) {
                        SchedulerEventUpdateMsg schedulerEventUpdateMsg =
                                schedulerEventUpdateMsgConstructor.constructSchedulerEventUpdatedMsg(
                                        UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                                        schedulerEvent);
                        EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                .setSchedulerEventUpdateMsg(schedulerEventUpdateMsg)
                                .build();
                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setEntityUpdateMsg(entityUpdateMsg)
                                .build());
                    }
                }
                return null;
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge scheduler event(s) on sync!");
        }
    }

    private void syncLoginWhiteLabeling(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            EntityId ownerId = edge.getOwnerId();
            String domainName = "localhost";
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                domainName = whiteLabelingService.getTenantLoginWhiteLabelingParams(new TenantId(ownerId.getId())).getDomainName();
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                domainName = whiteLabelingService.getCustomerLoginWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId())).getDomainName();
            }

            LoginWhiteLabelingParams loginWhiteLabelingParams = whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, domainName == null ? "localhost" : domainName, null, null);
            if (loginWhiteLabelingParams != null) {
                LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                        whiteLabelingParamsProtoConstructor.constructLoginWhiteLabelingParamsProto(loginWhiteLabelingParams);
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setLoginWhiteLabelingParams(loginWhiteLabelingParamsProto)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't load login white labeling params", e);
        }
    }

    private void syncWhiteLabeling(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            EntityId ownerId = edge.getOwnerId();
            WhiteLabelingParams whiteLabelingParams = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(new TenantId(ownerId.getId()), null, null);
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId()), null, null);
            }

            if (whiteLabelingParams != null) {
                WhiteLabelingParamsProto whiteLabelingParamsProto =
                        whiteLabelingParamsProtoConstructor.constructWhiteLabelingParamsProto(whiteLabelingParams);
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setWhiteLabelingParams(whiteLabelingParamsProto)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
        }
    }

    private void syncCustomTranslation(Edge edge, StreamObserver<ResponseMsg> outputStream) {
        try {
            EntityId ownerId = edge.getOwnerId();
            CustomTranslation customTranslation = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedTenantCustomTranslation(new TenantId(ownerId.getId()));
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedCustomerCustomTranslation(edge.getTenantId(), new CustomerId(ownerId.getId()));
            }


            if (customTranslation != null) {
                CustomTranslationProto customTranslationProto =
                        customTranslationProtoConstructor.constructCustomTranslationProto(customTranslation);
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setCustomTranslation(customTranslationProto)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        } catch (Exception e) {
            log.error("Can't load custom translation", e);
        }
    }

    @Override
    public void processRuleChainMetadata(Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edge.getTenantId(), ruleChainId);
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ruleChainUpdateMsgConstructor.constructRuleChainMetadataUpdatedMsg(
                            UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE,
                            ruleChainMetaData);
            if (ruleChainMetadataUpdateMsg != null) {
                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                        .setRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
                outputStream.onNext(ResponseMsg.newBuilder()
                        .setEntityUpdateMsg(entityUpdateMsg)
                        .build());
            }
        }
    }

    @Override
    public void processAttributesRequestMsg(Edge edge, AttributesRequestMsg attributesRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        ListenableFuture<List<AttributeKvEntry>> ssAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.SERVER_SCOPE);
        Futures.addCallback(ssAttrFuture, new FutureCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(@Nullable List<AttributeKvEntry> ssAttributes) {
                if (ssAttributes != null && !ssAttributes.isEmpty()) {
                    try {
                        ObjectNode entityNode = mapper.createObjectNode();
                        for (AttributeKvEntry attr : ssAttributes) {
                            if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                                entityNode.put(attr.getKey(), attr.getBooleanValue().get());
                            } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                                entityNode.put(attr.getKey(), attr.getDoubleValue().get());
                            } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                                entityNode.put(attr.getKey(), attr.getLongValue().get());
                            } else {
                                entityNode.put(attr.getKey(), attr.getValueAsString());
                            }
                        }
                        log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, entityNode);

                        EntityDataProto entityDataProto =
                                entityDataMsgConstructor.constructEntityDataMsg(
                                        entityId,
                                        ActionType.ATTRIBUTES_UPDATED,
                                        JsonUtils.parse(mapper.writeValueAsString(entityNode)));
                        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                                .addAllEntityData(Collections.singletonList(entityDataProto));
                        DownlinkMsg value = builder.build();

                        outputStream.onNext(ResponseMsg.newBuilder()
                                .setDownlinkMsg(value).build());
                    } catch (Exception e) {
                        log.error("[{}] Failed to send attribute updates to the edge", edge.getName(), e);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, dbCallbackExecutorService);

        // TODO: voba - push shared attributes to edge?
        ListenableFuture<List<AttributeKvEntry>> shAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.SHARED_SCOPE);
        ListenableFuture<List<AttributeKvEntry>> clAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.CLIENT_SCOPE);
    }

    @Override
    public void processRelationRequestMsg(Edge edge, RelationRequestMsg relationRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        Futures.addCallback(relationsListFuture, new FutureCallback<List<List<EntityRelation>>>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (!relationsList.isEmpty()) {
                        for (List<EntityRelation> entityRelations : relationsList) {
                            log.trace("[{}] [{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    RelationUpdateMsg relationUpdateMsg =
                                            relationUpdateMsgConstructor.constructRelationUpdatedMsg(
                                                    UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                    relation);
                                    EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                            .setRelationUpdateMsg(relationUpdateMsg)
                                            .build();
                                    outputStream.onNext(ResponseMsg.newBuilder()
                                            .setEntityUpdateMsg(entityUpdateMsg)
                                            .build());
                                } catch (Exception e) {
                                    log.error("Exception during loading relation [{}] to edge on sync!", relation, e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Exception during loading relation(s) to edge on sync!", t);
            }
        }, dbCallbackExecutorService);
    }

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(edge.getTenantId(), query);
    }

    @Override
    public void processDeviceCredentialsRequestMsg(Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(edge.getTenantId(), deviceId);
        if (deviceCredentials != null) {
            EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                    .setDeviceCredentialsUpdateMsg(deviceUpdateMsgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials))
                    .build();
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    @Override
    public void processUserCredentialsRequestMsg(Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
        UserCredentials userCredentialsByUserId = userService.findUserCredentialsByUserId(edge.getTenantId(), userId);
        if (userCredentialsByUserId != null) {
            EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                    .setUserCredentialsUpdateMsg(userUpdateMsgConstructor.constructUserCredentialsUpdatedMsg(userCredentialsByUserId))
                    .build();
            outputStream.onNext(ResponseMsg.newBuilder()
                    .setEntityUpdateMsg(entityUpdateMsg)
                    .build());
        }
    }

    @Override
    public void processEntityGroupEntitiesRequest(Edge edge, EntityGroupEntitiesRequestMsg entityGroupEntitiesRequestMsg, StreamObserver<ResponseMsg> outputStream) {
        if (entityGroupEntitiesRequestMsg.getEntityGroupIdMSB() != 0 && entityGroupEntitiesRequestMsg.getEntityGroupIdLSB() != 0) {
            EntityGroupId entityGroupId = new EntityGroupId(new UUID(entityGroupEntitiesRequestMsg.getEntityGroupIdMSB(), entityGroupEntitiesRequestMsg.getEntityGroupIdLSB()));
            ListenableFuture<List<EntityId>> entityIdsFuture = entityGroupService.findAllEntityIds(edge.getTenantId(), entityGroupId, new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(entityIdsFuture, new FutureCallback<List<EntityId>>() {
                @Override
                public void onSuccess(@Nullable List<EntityId> entityIds) {
                    EntityType groupType = EntityType.valueOf(entityGroupEntitiesRequestMsg.getType());
                    switch (groupType) {
                        case DEVICE:
                            syncDevices(edge, entityIds, entityGroupId, outputStream);
                            break;
                        case ASSET:
                            syncAssets(edge, entityIds, entityGroupId, outputStream);
                            break;
                        case ENTITY_VIEW:
                            syncEntityViews(edge, entityIds, entityGroupId, outputStream);
                            break;
                        case DASHBOARD:
                            syncDashboards(edge, entityIds, entityGroupId, outputStream);
                            break;
                        case USER:
                            syncUsers(edge, entityIds, entityGroupId, outputStream);
                            break;
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failed to sync entity group [{}]", entityGroupId, t);
                }
            }, dbCallbackExecutorService);
        }
    }

    private void syncDevices(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId, StreamObserver<ResponseMsg> outputStream) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<DeviceId> deviceIds = entityIds.stream().map(e -> new DeviceId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<Device>> devicesFuture = deviceService.findDevicesByTenantIdAndIdsAsync(edge.getTenantId(), deviceIds);
                Futures.addCallback(devicesFuture, new FutureCallback<List<Device>>() {
                    @Override
                    public void onSuccess(@Nullable List<Device> devices) {
                        if (devices != null && !devices.isEmpty()) {
                            log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), devices.size());
                            for (Device device : devices) {
                                DeviceUpdateMsg deviceUpdateMsg =
                                        deviceUpdateMsgConstructor.constructDeviceUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                device,
                                                entityGroupId);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setDeviceUpdateMsg(deviceUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Exception during loading edge device(s) on sync!", t);
                    }
                }, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
        }
    }

    private void syncAssets(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId, StreamObserver<ResponseMsg> outputStream) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<AssetId> assetIds = entityIds.stream().map(e -> new AssetId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<Asset>> assetsFuture = assetService.findAssetsByTenantIdAndIdsAsync(edge.getTenantId(), assetIds);
                Futures.addCallback(assetsFuture, new FutureCallback<List<Asset>>() {
                    @Override
                    public void onSuccess(@Nullable List<Asset> assets) {
                        if (assets != null && !assets.isEmpty()) {
                            log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), assets.size());
                            for (Asset asset : assets) {
                                AssetUpdateMsg assetUpdateMsg =
                                        assetUpdateMsgConstructor.constructAssetUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                asset,
                                                entityGroupId);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setAssetUpdateMsg(assetUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Exception during loading edge asset(s) on sync!", t);
                    }
                }, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on sync!", e);
        }
    }

    private void syncEntityViews(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId, StreamObserver<ResponseMsg> outputStream) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<EntityViewId> entityViewIds = entityIds.stream().map(e -> new EntityViewId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<EntityView>> entityViewsFuture = entityViewService.findEntityViewsByTenantIdAndIdsAsync(edge.getTenantId(), entityViewIds);
                Futures.addCallback(entityViewsFuture, new FutureCallback<List<EntityView>>() {
                    @Override
                    public void onSuccess(@Nullable List<EntityView> entityViews) {
                        if (entityViews != null && !entityViews.isEmpty()) {
                            log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), entityViews.size());
                            for (EntityView entityView : entityViews) {
                                EntityViewUpdateMsg entityViewUpdateMsg =
                                        entityViewUpdateMsgConstructor.constructEntityViewUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                entityView,
                                                entityGroupId);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setEntityViewUpdateMsg(entityViewUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Exception during loading edge entity view(s) on sync!", t);
                    }
                }, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge  entity view(s) on sync!", e);
        }
    }

    private void syncDashboards(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId, StreamObserver<ResponseMsg> outputStream) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<DashboardId> dashboardIds = entityIds.stream().map(e -> new DashboardId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<DashboardInfo>> dashboardInfosFuture = dashboardService.findDashboardInfoByIdsAsync(edge.getTenantId(), dashboardIds);

                ListenableFuture<List<Dashboard>> dashboardsFuture = Futures.transformAsync(dashboardInfosFuture, dashboardInfos -> {
                    List<ListenableFuture<Dashboard>> futures = new ArrayList<>();
                    if (dashboardInfos != null && !dashboardInfos.isEmpty()) {
                        for (DashboardInfo dashboardInfo : dashboardInfos) {
                            futures.add(dashboardService.findDashboardByIdAsync(edge.getTenantId(), dashboardInfo.getId()));
                        }
                    }
                    return Futures.successfulAsList(futures);
                }, dbCallbackExecutorService);

                Futures.addCallback(dashboardsFuture, new FutureCallback<List<Dashboard>>() {
                    @Override
                    public void onSuccess(@Nullable List<Dashboard> dashboards) {
                        if (dashboards != null && !dashboards.isEmpty()) {
                            log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), dashboards.size());
                            for (Dashboard dashboard : dashboards) {
                                DashboardUpdateMsg dashboardUpdateMsg =
                                        dashboardUpdateMsgConstructor.constructDashboardUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                dashboard,
                                                entityGroupId);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setDashboardUpdateMsg(dashboardUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Exception during loading edge dashboard(s) on sync!", t);
                    }
                }, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on sync!", e);
        }
    }

    private void syncUsers(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId, StreamObserver<ResponseMsg> outputStream) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<UserId> userIds = entityIds.stream().map(e -> new UserId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<User>> usersFuture = userService.findUsersByTenantIdAndIdsAsync(edge.getTenantId(), userIds);

                Futures.addCallback(usersFuture, new FutureCallback<List<User>>() {
                    @Override
                    public void onSuccess(@Nullable List<User> users) {
                        if (users != null && !users.isEmpty()) {
                            log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), users.size());
                            for (User user : users) {
                                UserUpdateMsg userUpdateMsg =
                                        userUpdateMsgConstructor.constructUserUpdatedMsg(
                                                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                                                user,
                                                entityGroupId);
                                EntityUpdateMsg entityUpdateMsg = EntityUpdateMsg.newBuilder()
                                        .setUserUpdateMsg(userUpdateMsg)
                                        .build();
                                outputStream.onNext(ResponseMsg.newBuilder()
                                        .setEntityUpdateMsg(entityUpdateMsg)
                                        .build());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Exception during loading edge user(s) on sync!", t);
                    }
                }, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on sync!", e);
        }
    }
}
