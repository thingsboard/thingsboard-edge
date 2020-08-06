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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
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
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.AttributesRequestMsg;
import org.thingsboard.server.gen.edge.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.EntityGroupEntitiesRequestMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultSyncEdgeService implements SyncEdgeService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String MAIL_TEMPLATES = "mailTemplates";

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Override
    public void sync(Edge edge) {
        try {
            syncWidgetsBundleAndWidgetTypes(edge);
            syncLoginWhiteLabeling(edge);
            syncWhiteLabeling(edge);
            syncCustomTranslation(edge);
            syncRuleChains(edge);
            syncEntityGroups(edge);
            syncSchedulerEvents(edge);
            syncMailTemplateSettings(edge);
        } catch (Exception e) {
            log.error("Exception during sync process", e);
        }
    }

    private void syncRuleChains(Edge edge) {
        try {
            ListenableFuture<TimePageData<RuleChain>> future =
                    ruleChainService.findRuleChainsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId(), new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(future, new FutureCallback<TimePageData<RuleChain>>() {
                @Override
                public void onSuccess(@Nullable TimePageData<RuleChain> pageData) {
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                        for (RuleChain ruleChain : pageData.getData()) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.RULE_CHAIN, ActionType.ADDED, ruleChain.getId(), null, null);
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

    private void syncEntityGroups(Edge edge) {
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
                                    saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_GROUP, ActionType.ADDED, entityGroup.getId(), null, null);
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

    private void syncSchedulerEvents(Edge edge) {
        try {
            ListenableFuture<List<SchedulerEvent>> schedulerEventsFuture =
                    schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId());
            Futures.addCallback(schedulerEventsFuture, new FutureCallback<List<SchedulerEvent>>() {
                @Override
                public void onSuccess(@Nullable List<SchedulerEvent> schedulerEvents) {
                    if (schedulerEvents != null && !schedulerEvents.isEmpty()) {
                        log.trace("[{}] [{}] scheduler events(s) are going to be pushed to edge.", edge.getId(), schedulerEvents.size());
                        for (SchedulerEvent schedulerEvent : schedulerEvents) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.SCHEDULER_EVENT, ActionType.ADDED, schedulerEvent.getId(), null, null);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Exception during loading edge scheduler event(s) on sync!");
                }
            }, dbCallbackExecutorService);
        } catch (Exception e) {
            log.error("Exception during loading edge scheduler event(s) on sync!");
        }
    }

    private void syncLoginWhiteLabeling(Edge edge) {
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
                saveEdgeEvent(edge.getTenantId(),
                        edge.getId(),
                        EdgeEventType.LOGIN_WHITE_LABELING,
                        ActionType.UPDATED,
                        null,
                        mapper.valueToTree(loginWhiteLabelingParams),
                        null);
            }
        } catch (Exception e) {
            log.error("Can't load login white labeling params", e);
        }
    }

    private void syncWhiteLabeling(Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            WhiteLabelingParams whiteLabelingParams = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(new TenantId(ownerId.getId()), null, null);
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(edge.getTenantId(), new CustomerId(ownerId.getId()), null, null);
            }

            if (whiteLabelingParams != null) {
                saveEdgeEvent(edge.getTenantId(),
                        edge.getId(),
                        EdgeEventType.WHITE_LABELING,
                        ActionType.UPDATED,
                        null,
                        mapper.valueToTree(whiteLabelingParams),
                        null);
            }
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
        }
    }

    private void syncCustomTranslation(Edge edge) {
        try {
            EntityId ownerId = edge.getOwnerId();
            CustomTranslation customTranslation = null;

            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedTenantCustomTranslation(new TenantId(ownerId.getId()));
            } else if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                customTranslation = customTranslationService.getMergedCustomerCustomTranslation(edge.getTenantId(), new CustomerId(ownerId.getId()));
            }

            if (customTranslation != null) {
                saveEdgeEvent(edge.getTenantId(),
                        edge.getId(),
                        EdgeEventType.CUSTOM_TRANSLATION,
                        ActionType.UPDATED,
                        null,
                        mapper.valueToTree(customTranslation),
                        null);
            }
        } catch (Exception e) {
            log.error("Can't load custom translation", e);
        }
    }

    private void syncMailTemplateSettings(Edge edge) {
        try {
            AdminSettings sysAdminMailTemplates = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, MAIL_TEMPLATES);
            saveMailTemplateSettingsEdgeEvent(edge, sysAdminMailTemplates);
            Optional<AttributeKvEntry> tenantMailTemplateAttr = attributesService.find(edge.getTenantId(), edge.getTenantId(), DataConstants.SERVER_SCOPE, MAIL_TEMPLATES).get();
            if (tenantMailTemplateAttr.isPresent()) {
                AdminSettings tenantMailTemplates = new AdminSettings();
                tenantMailTemplates.setKey(MAIL_TEMPLATES);
                String value = tenantMailTemplateAttr.get().getValueAsString();
                tenantMailTemplates.setJsonValue(mapper.readTree(value));
                saveMailTemplateSettingsEdgeEvent(edge, tenantMailTemplates);
            }
        } catch (Exception e) {
            log.error("Can't load mail template settings", e);
        }
    }

    private void saveMailTemplateSettingsEdgeEvent(Edge edge, AdminSettings adminSettings) {
        saveEdgeEvent(edge.getTenantId(),
                edge.getId(),
                EdgeEventType.MAIL_TEMPLATE_SETTINGS,
                ActionType.UPDATED,
                null,
                mapper.valueToTree(adminSettings),
                null);
    }

    private void syncWidgetsBundleAndWidgetTypes(Edge edge) {
        List<WidgetsBundle> widgetsBundlesToPush = new ArrayList<>();
        List<WidgetType> widgetTypesToPush = new ArrayList<>();
        widgetsBundlesToPush.addAll(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(edge.getTenantId()));
        widgetsBundlesToPush.addAll(widgetsBundleService.findSystemWidgetsBundles(edge.getTenantId()));
        try {
            for (WidgetsBundle widgetsBundle: widgetsBundlesToPush) {
                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.WIDGETS_BUNDLE, ActionType.ADDED, widgetsBundle.getId(), null, null);
                widgetTypesToPush.addAll(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(widgetsBundle.getTenantId(), widgetsBundle.getAlias()));
            }
            for (WidgetType widgetType: widgetTypesToPush) {
                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.WIDGET_TYPE, ActionType.ADDED, widgetType.getId(), null, null);
            }
        } catch (Exception e) {
            log.error("Exception during loading widgets bundle(s) and widget type(s) on sync!", e);
        }
    }

    @Override
    public void processRuleChainMetadataRequestMsg(Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId = new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.RULE_CHAIN_METADATA, ActionType.ADDED, ruleChainId, null, null);
        }
    }

    @Override
    public void processAttributesRequestMsg(Edge edge, AttributesRequestMsg attributesRequestMsg) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType edgeEventType = getEdgeQueueTypeByEntityType(entityId.getEntityType());
        if (edgeEventType != null) {
            ListenableFuture<List<AttributeKvEntry>> ssAttrFuture = attributesService.findAll(edge.getTenantId(), entityId, DataConstants.SERVER_SCOPE);
            Futures.addCallback(ssAttrFuture, new FutureCallback<List<AttributeKvEntry>>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> ssAttributes) {
                    if (ssAttributes != null && !ssAttributes.isEmpty()) {
                        try {
                            Map<String, Object> entityData = new HashMap<>();
                            ObjectNode attributes = mapper.createObjectNode();
                            for (AttributeKvEntry attr : ssAttributes) {
                                if (attr.getDataType() == DataType.BOOLEAN && attr.getBooleanValue().isPresent()) {
                                    attributes.put(attr.getKey(), attr.getBooleanValue().get());
                                } else if (attr.getDataType() == DataType.DOUBLE && attr.getDoubleValue().isPresent()) {
                                    attributes.put(attr.getKey(), attr.getDoubleValue().get());
                                } else if (attr.getDataType() == DataType.LONG && attr.getLongValue().isPresent()) {
                                    attributes.put(attr.getKey(), attr.getLongValue().get());
                                } else {
                                    attributes.put(attr.getKey(), attr.getValueAsString());
                                }
                            }
                            entityData.put("kv", attributes);
                            entityData.put("scope", DataConstants.SERVER_SCOPE);
                            JsonNode entityBody = mapper.valueToTree(entityData);
                            log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, entityBody);
                            saveEdgeEvent(edge.getTenantId(),
                                    edge.getId(),
                                    edgeEventType,
                                    ActionType.ATTRIBUTES_UPDATED,
                                    entityId,
                                    entityBody,
                                    null);
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
    }

    private EdgeEventType getEdgeQueueTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeEventType.DEVICE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            default:
                return null;
        }
    }

    @Override
    public void processRelationRequestMsg(Edge edge, RelationRequestMsg relationRequestMsg) {
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
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        saveEdgeEvent(edge.getTenantId(),
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                ActionType.ADDED,
                                                null,
                                                mapper.valueToTree(relation),
                                                null);
                                    }
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
    public void processDeviceCredentialsRequestMsg(Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DEVICE, ActionType.CREDENTIALS_UPDATED, deviceId, null, null);
        }
    }

    @Override
    public void processUserCredentialsRequestMsg(Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        if (userCredentialsRequestMsg.getUserIdMSB() != 0 && userCredentialsRequestMsg.getUserIdLSB() != 0) {
            UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, ActionType.CREDENTIALS_UPDATED, userId, null, null);
        }
    }

    @Override
    public void processEntityGroupEntitiesRequest(Edge edge, EntityGroupEntitiesRequestMsg entityGroupEntitiesRequestMsg) {
        if (entityGroupEntitiesRequestMsg.getEntityGroupIdMSB() != 0 && entityGroupEntitiesRequestMsg.getEntityGroupIdLSB() != 0) {
            EntityGroupId entityGroupId = new EntityGroupId(new UUID(entityGroupEntitiesRequestMsg.getEntityGroupIdMSB(), entityGroupEntitiesRequestMsg.getEntityGroupIdLSB()));
            ListenableFuture<List<EntityId>> entityIdsFuture = entityGroupService.findAllEntityIds(edge.getTenantId(), entityGroupId, new TimePageLink(Integer.MAX_VALUE));
            Futures.addCallback(entityIdsFuture, new FutureCallback<List<EntityId>>() {
                @Override
                public void onSuccess(@Nullable List<EntityId> entityIds) {
                    EntityType groupType = EntityType.valueOf(entityGroupEntitiesRequestMsg.getType());
                    switch (groupType) {
                        case DEVICE:
                            syncDevices(edge, entityIds, entityGroupId);
                            break;
                        case ASSET:
                            syncAssets(edge, entityIds, entityGroupId);
                            break;
                        case ENTITY_VIEW:
                            syncEntityViews(edge, entityIds, entityGroupId);
                            break;
                        case DASHBOARD:
                            syncDashboards(edge, entityIds, entityGroupId);
                            break;
                        case USER:
                            syncUsers(edge, entityIds, entityGroupId);
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

    private void syncDevices(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
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
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DEVICE, ActionType.ADDED, device.getId(), null, entityGroupId);
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

    private void syncAssets(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
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
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ASSET, ActionType.ADDED, asset.getId(), null, entityGroupId);
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

    private void syncEntityViews(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
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
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_VIEW, ActionType.ADDED, entityView.getId(), null, entityGroupId);
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

    private void syncDashboards(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<DashboardId> dashboardIds = entityIds.stream().map(e -> new DashboardId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<DashboardInfo>> dashboardInfosFuture = dashboardService.findDashboardInfoByIdsAsync(edge.getTenantId(), dashboardIds);

                Futures.addCallback(dashboardInfosFuture, new FutureCallback<List<DashboardInfo>>() {
                    @Override
                    public void onSuccess(@Nullable List<DashboardInfo> dashboardInfos) {
                        if (dashboardInfos != null && !dashboardInfos.isEmpty()) {
                            log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), dashboardInfos.size());
                            for (DashboardInfo dashboardInfo : dashboardInfos) {
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DASHBOARD, ActionType.ADDED, dashboardInfo.getId(), null, entityGroupId);
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

    private void syncUsers(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
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
                                saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, ActionType.ADDED, user.getId(), null, entityGroupId);
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

    private void saveEdgeEvent(TenantId tenantId,
                               EdgeId edgeId,
                               EdgeEventType edgeEventType,
                               ActionType edgeEventAction,
                               EntityId entityId,
                               JsonNode entityBody,
                               EntityId entityGroupId) {
        log.debug("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], edgeEventType [{}], edgeEventAction[{}], entityId [{}], entityBody [{}]",
                tenantId, edgeId, edgeEventType, edgeEventAction, entityId, entityBody);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setEdgeEventType(edgeEventType);
        edgeEvent.setEdgeEventAction(edgeEventAction.name());
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId.getId());
        }
        edgeEvent.setEntityBody(entityBody);
        edgeEventService.saveAsync(edgeEvent);
    }
}
