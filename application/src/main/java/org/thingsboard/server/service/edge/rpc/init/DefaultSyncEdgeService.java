/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Edge;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
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
import org.thingsboard.server.gen.edge.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.RelationRequestMsg;
import org.thingsboard.server.gen.edge.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.UserCredentialsRequestMsg;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;

import java.util.ArrayList;
import java.util.Arrays;
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

    private static final int DEFAULT_LIMIT = 100;

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
    private DeviceProfileService deviceProfileService;

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
    private GroupPermissionService groupPermissionService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private CustomTranslationService customTranslationService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    @Autowired
    private TbClusterService tbClusterService;

    @Override
    public void sync(TenantId tenantId, Edge edge) {
        log.trace("[{}][{}] Staring edge sync process", tenantId, edge.getId());
        try {

            syncEdgeOwner(tenantId, edge);
            syncRoles(tenantId, edge);
            syncWidgetsBundles(tenantId, edge);
            syncDeviceProfiles(tenantId, edge);
            syncLoginWhiteLabeling(tenantId, edge);
            syncWhiteLabeling(tenantId, edge);
            syncCustomTranslation(tenantId, edge);
            // TODO: voba - implement this
            // syncAdminSettings(tenantId, edge);
            syncRuleChains(tenantId, edge);
            syncEntityGroups(tenantId, edge);
            syncSchedulerEvents(tenantId, edge);
            syncWidgetsTypes(tenantId, edge);
        } catch (Exception e) {
            log.error("[{}][{}] Exception during sync process", tenantId, edge.getId(), e);
        }
    }

    private void syncRoles(TenantId tenantId, Edge edge) {
        processRolesData(TenantId.SYS_TENANT_ID, edge);
        processRolesData(tenantId, edge);
    }

    private void processRolesData(TenantId tenantId, Edge edge) {
        PageData<Role> rolesData =
                roleService.findRolesByTenantId(tenantId, new PageLink(Integer.MAX_VALUE));
        if (!rolesData.getData().isEmpty()) {
            for (Role role : rolesData.getData()) {
                saveEdgeEvent(edge.getTenantId(), edge.getId(),
                        EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null);
            }
        }
    }

    private void syncEntityGroups(TenantId tenantId, Edge edge) {
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
                                    if (!entityGroup.isEdgeGroupAll()) {
                                        saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_GROUP, EdgeEventActionType.ADDED, entityGroup.getId(), null, null);
                                    }
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

    private void syncSchedulerEvents(TenantId tenantId, Edge edge) {
        try {
            ListenableFuture<List<SchedulerEvent>> schedulerEventsFuture =
                    schedulerEventService.findSchedulerEventsByTenantIdAndEdgeId(edge.getTenantId(), edge.getId());
            Futures.addCallback(schedulerEventsFuture, new FutureCallback<List<SchedulerEvent>>() {
                @Override
                public void onSuccess(@Nullable List<SchedulerEvent> schedulerEvents) {
                    if (schedulerEvents != null && !schedulerEvents.isEmpty()) {
                        log.trace("[{}] [{}] scheduler events(s) are going to be pushed to edge.", edge.getId(), schedulerEvents.size());
                        for (SchedulerEvent schedulerEvent : schedulerEvents) {
                            saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.SCHEDULER_EVENT, EdgeEventActionType.ADDED, schedulerEvent.getId(), null, null);
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

    private void syncLoginWhiteLabeling(TenantId tenantId, Edge edge) {
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
                        EdgeEventActionType.UPDATED,
                        null,
                        mapper.valueToTree(loginWhiteLabelingParams),
                        null);
            }
        } catch (Exception e) {
            log.error("Can't load login white labeling params", e);
        }
    }

    private void syncWhiteLabeling(TenantId tenantId, Edge edge) {
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
                        EdgeEventActionType.UPDATED,
                        null,
                        mapper.valueToTree(whiteLabelingParams),
                        null);
            }
        } catch (Exception e) {
            log.error("Can't load white labeling params", e);
        }
    }


    @Override
    public void syncEdgeOwner(TenantId tenantId, Edge edge) {
        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType())) {
            saveEdgeEvent(edge.getTenantId(), edge.getId(),
                    EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getOwnerId(), null, null);

            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<Role> rolesData;
            do {
                rolesData = roleService.findRolesByTenantIdAndCustomerId(tenantId,
                        new CustomerId(edge.getOwnerId().getId()), pageLink);
                if (rolesData != null && rolesData.getData() != null && !rolesData.getData().isEmpty()) {
                    for (Role role : rolesData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(),
                                EdgeEventType.ROLE, EdgeEventActionType.ADDED, role.getId(), null, null);
                    }
                    if (rolesData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (rolesData != null && rolesData.hasNext());
        }
    }


    private void syncRuleChains(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncRuleChains [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<RuleChain> pageData;
            do {
                pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] rule chains(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (RuleChain ruleChain : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN, EdgeEventActionType.ADDED, ruleChain.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading edge rule chain(s) on sync!", e);
        }
    }

    private void syncDeviceProfiles(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncDeviceProfiles [{}]", tenantId, edge.getName());
        try {
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<DeviceProfile> pageData;
            do {
                pageData = deviceProfileService.findDeviceProfiles(tenantId, pageLink);
                if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                    log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
                    for (DeviceProfile deviceProfile : pageData.getData()) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_PROFILE, EdgeEventActionType.ADDED, deviceProfile.getId(), null);
                    }
                    if (pageData.hasNext()) {
                        pageLink = pageLink.nextPageLink();
                    }
                }
            } while (pageData != null && pageData.hasNext());
        } catch (Exception e) {
            log.error("Exception during loading device profile(s) on sync!", e);
        }
    }

    private void syncCustomTranslation(TenantId tenantId, Edge edge) {
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
                        EdgeEventActionType.UPDATED,
                        null,
                        mapper.valueToTree(customTranslation),
                        null);
            }
        } catch (Exception e) {
            log.error("Can't load custom translation", e);
        }
    }

    private void syncAdminSettings(TenantId tenantId, Edge edge) {
        try {
            List<String> adminSettingsKeys = Arrays.asList("mail", "mailTemplates");
            for (String key : adminSettingsKeys) {
                AdminSettings sysAdminMainSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key);
                saveAdminSettingsEdgeEvent(edge, sysAdminMainSettings);
                Optional<AttributeKvEntry> tenantMailSettingsAttr = attributesService.find(edge.getTenantId(), edge.getTenantId(), DataConstants.SERVER_SCOPE, key).get();
                if (tenantMailSettingsAttr.isPresent()) {
                    AdminSettings tenantMailSettings = new AdminSettings();
                    tenantMailSettings.setKey(key);
                    String value = tenantMailSettingsAttr.get().getValueAsString();
                    tenantMailSettings.setJsonValue(mapper.readTree(value));
                    saveAdminSettingsEdgeEvent(edge, tenantMailSettings);
                }
            }
        } catch (Exception e) {
            log.error("Can't load admin settings", e);
        }
    }

    private void saveAdminSettingsEdgeEvent(Edge edge, AdminSettings adminSettings) {
        log.info(String.valueOf(adminSettings));
        saveEdgeEvent(edge.getTenantId(),
                edge.getId(),
                EdgeEventType.ADMIN_SETTINGS,
                EdgeEventActionType.UPDATED,
                null,
                mapper.valueToTree(adminSettings),
                null);
    }

    private void syncCustomerUsers(TenantId tenantId, Edge edge) {
        if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.CUSTOMER, EdgeEventActionType.ADDED, edge.getCustomerId(), null);
            PageLink pageLink = new PageLink(DEFAULT_LIMIT);
            PageData<User> pageData;
            do {
                pageData = userService.findCustomerUsers(tenantId, edge.getCustomerId(), pageLink);
                pushUsersToEdge(tenantId, pageData, edge);
                if (pageData != null && pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            } while (pageData != null && pageData.hasNext());
        }
    }

    private void pushUsersToEdge(TenantId tenantId, PageData<User> pageData, Edge edge) {
        if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
            log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), pageData.getData().size());
            for (User user : pageData.getData()) {
                saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null);
            }
        }
    }

    private void syncWidgetsBundles(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncWidgetsBundles [{}]", tenantId, edge.getName());
        List<WidgetsBundle> widgetsBundlesToPush = new ArrayList<>();
        widgetsBundlesToPush.addAll(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId));
        widgetsBundlesToPush.addAll(widgetsBundleService.findSystemWidgetsBundles(tenantId));
        try {
            for (WidgetsBundle widgetsBundle : widgetsBundlesToPush) {
                saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGETS_BUNDLE, EdgeEventActionType.ADDED, widgetsBundle.getId(), null);
            }
        } catch (Exception e) {
            log.error("Exception during loading widgets bundle(s) on sync!", e);
        }
    }

    private void syncWidgetsTypes(TenantId tenantId, Edge edge) {
        log.trace("[{}] syncWidgetsTypes [{}]", tenantId, edge.getName());
        List<WidgetsBundle> widgetsBundlesToPush = new ArrayList<>();
        widgetsBundlesToPush.addAll(widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenantId));
        widgetsBundlesToPush.addAll(widgetsBundleService.findSystemWidgetsBundles(tenantId));
        try {
            for (WidgetsBundle widgetsBundle : widgetsBundlesToPush) {
                List<WidgetType> widgetTypesToPush =
                        widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(widgetsBundle.getTenantId(), widgetsBundle.getAlias());
                for (WidgetType widgetType : widgetTypesToPush) {
                    saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGET_TYPE, EdgeEventActionType.ADDED, widgetType.getId(), null);
                }
            }
        } catch (Exception e) {
            log.error("Exception during loading widgets type(s) on sync!", e);
        }
    }

    private AdminSettings convertToTenantAdminSettings(String key, ObjectNode jsonValue) {
        AdminSettings tenantMailSettings = new AdminSettings();
        jsonValue.put("useSystemMailSettings", true);
        tenantMailSettings.setJsonValue(jsonValue);
        tenantMailSettings.setKey(key);
        return tenantMailSettings;
    }

    @Override
    public ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg) {
        log.trace("[{}] processRuleChainMetadataRequestMsg [{}][{}]", tenantId, edge.getName(), ruleChainMetadataRequestMsg);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        if (ruleChainMetadataRequestMsg.getRuleChainIdMSB() != 0 && ruleChainMetadataRequestMsg.getRuleChainIdLSB() != 0) {
            RuleChainId ruleChainId =
                    new RuleChainId(new UUID(ruleChainMetadataRequestMsg.getRuleChainIdMSB(), ruleChainMetadataRequestMsg.getRuleChainIdLSB()));
            ListenableFuture<EdgeEvent> future = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.RULE_CHAIN_METADATA, EdgeEventActionType.ADDED, ruleChainId, null);
            Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
                @Override
                public void onSuccess(@Nullable EdgeEvent result) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save edge event [{}]", ruleChainMetadataRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
        }
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg) {
        log.trace("[{}] processAttributesRequestMsg [{}][{}]", tenantId, edge.getName(), attributesRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(attributesRequestMsg.getEntityType()),
                new UUID(attributesRequestMsg.getEntityIdMSB(), attributesRequestMsg.getEntityIdLSB()));
        final EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
        if (type != null) {
            SettableFuture<Void> futureToSet = SettableFuture.create();
            String scope = attributesRequestMsg.getScope();
            ListenableFuture<List<AttributeKvEntry>> ssAttrFuture = attributesService.findAll(tenantId, entityId, scope);
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
                            entityData.put("scope", scope);
                            JsonNode body = mapper.valueToTree(entityData);
                            log.debug("Sending attributes data msg, entityId [{}], attributes [{}]", entityId, body);
                            saveEdgeEvent(tenantId,
                                    edge.getId(),
                                    type,
                                    EdgeEventActionType.ATTRIBUTES_UPDATED,
                                    entityId,
                                    body,
                                    null);
                        } catch (Exception e) {
                            log.error("[{}] Failed to send attribute updates to the edge", edge.getName(), e);
                            throw new RuntimeException("[" + edge.getName() + "] Failed to send attribute updates to the edge", e);
                        }
                    } else {
                        log.trace("[{}][{}] No attributes found for entity {} [{}]", tenantId,
                                edge.getName(),
                                entityId.getEntityType(),
                                entityId.getId());
                    }
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save attributes [{}]", attributesRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
            return futureToSet;
        } else {
            log.warn("[{}] Type doesn't supported {}", tenantId, entityId.getEntityType());
            return Futures.immediateFuture(null);
        }
    }

    @Override
    public ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg) {
        log.trace("[{}] processRelationRequestMsg [{}][{}]", tenantId, edge.getName(), relationRequestMsg);
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(
                EntityType.valueOf(relationRequestMsg.getEntityType()),
                new UUID(relationRequestMsg.getEntityIdMSB(), relationRequestMsg.getEntityIdLSB()));

        List<ListenableFuture<List<EntityRelation>>> futures = new ArrayList<>();
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.FROM));
        futures.add(findRelationByQuery(tenantId, edge, entityId, EntitySearchDirection.TO));
        ListenableFuture<List<List<EntityRelation>>> relationsListFuture = Futures.allAsList(futures);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        Futures.addCallback(relationsListFuture, new FutureCallback<List<List<EntityRelation>>>() {
            @Override
            public void onSuccess(@Nullable List<List<EntityRelation>> relationsList) {
                try {
                    if (relationsList != null && !relationsList.isEmpty()) {
                        for (List<EntityRelation> entityRelations : relationsList) {
                            log.trace("[{}] [{}] [{}] relation(s) are going to be pushed to edge.", edge.getId(), entityId, entityRelations.size());
                            for (EntityRelation relation : entityRelations) {
                                try {
                                    if (!relation.getFrom().getEntityType().equals(EntityType.EDGE) &&
                                            !relation.getTo().getEntityType().equals(EntityType.EDGE)) {
                                        saveEdgeEvent(tenantId,
                                                edge.getId(),
                                                EdgeEventType.RELATION,
                                                EdgeEventActionType.ADDED,
                                                null,
                                                mapper.valueToTree(relation));
                                    }
                                } catch (Exception e) {
                                    log.error("Exception during loading relation [{}] to edge on sync!", relation, e);
                                    futureToSet.setException(e);
                                    return;
                                }
                            }
                        }
                    }
                    futureToSet.set(null);
                } catch (Exception e) {
                    log.error("Exception during loading relation(s) to edge on sync!", e);
                    futureToSet.setException(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't find relation by query. Entity id [{}]", tenantId, entityId, t);
                futureToSet.setException(t);
            }
        }, dbCallbackExecutorService);
        return futureToSet;
    }

    private ListenableFuture<List<EntityRelation>> findRelationByQuery(TenantId tenantId, Edge edge, EntityId entityId, EntitySearchDirection direction) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(entityId, direction, -1, false));
        return relationService.findByQuery(tenantId, query);
    }

    @Override
    public ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg) {
        log.trace("[{}] processDeviceCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), deviceCredentialsRequestMsg);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        if (deviceCredentialsRequestMsg.getDeviceIdMSB() != 0 && deviceCredentialsRequestMsg.getDeviceIdLSB() != 0) {
            DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsRequestMsg.getDeviceIdMSB(), deviceCredentialsRequestMsg.getDeviceIdLSB()));
            ListenableFuture<EdgeEvent> future = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_UPDATED, deviceId, null);
            Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
                @Override
                public void onSuccess(@Nullable EdgeEvent result) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save edge event [{}]", deviceCredentialsRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
        }
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg) {
        log.trace("[{}] processUserCredentialsRequestMsg [{}][{}]", tenantId, edge.getName(), userCredentialsRequestMsg);
        SettableFuture<Void> futureToSet = SettableFuture.create();
        if (userCredentialsRequestMsg.getUserIdMSB() != 0 && userCredentialsRequestMsg.getUserIdLSB() != 0) {
            UserId userId = new UserId(new UUID(userCredentialsRequestMsg.getUserIdMSB(), userCredentialsRequestMsg.getUserIdLSB()));
            ListenableFuture<EdgeEvent> future = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER, EdgeEventActionType.CREDENTIALS_UPDATED, userId, null);
            Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
                @Override
                public void onSuccess(@Nullable EdgeEvent result) {
                    futureToSet.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Can't save edge event [{}]", userCredentialsRequestMsg, t);
                    futureToSet.setException(t);
                }
            }, dbCallbackExecutorService);
        }
        return futureToSet;
    }

    @Override
    public ListenableFuture<Void> processEntityGroupEntitiesRequest(TenantId tenantId, Edge edge, EntityGroupRequestMsg entityGroupEntitiesRequestMsg) {
        if (entityGroupEntitiesRequestMsg.getEntityGroupIdMSB() != 0 && entityGroupEntitiesRequestMsg.getEntityGroupIdLSB() != 0) {
            EntityGroupId entityGroupId = new EntityGroupId(new UUID(entityGroupEntitiesRequestMsg.getEntityGroupIdMSB(), entityGroupEntitiesRequestMsg.getEntityGroupIdLSB()));
            // TODO: voba - refactor this to pagination
            ListenableFuture<List<EntityId>> entityIdsFuture = entityGroupService.findAllEntityIds(edge.getTenantId(), entityGroupId, new PageLink(Integer.MAX_VALUE));
            return Futures.transformAsync(entityIdsFuture, entityIds -> {
                EntityType groupType = EntityType.valueOf(entityGroupEntitiesRequestMsg.getType());
                switch (groupType) {
                    case DEVICE:
                        return syncDevices(edge, entityIds, entityGroupId);
                    case ASSET:
                        return syncAssets(edge, entityIds, entityGroupId);
                    case ENTITY_VIEW:
                        return syncEntityViews(edge, entityIds, entityGroupId);
                    case DASHBOARD:
                        return syncDashboards(edge, entityIds, entityGroupId);
                    case USER:
                        return syncUsers(edge, entityIds, entityGroupId);
                    default:
                        return Futures.immediateFuture(null);
                }
            }, dbCallbackExecutorService);
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> processEntityGroupPermissionsRequest(TenantId tenantId, Edge edge, EntityGroupRequestMsg entityGroupEntitiesRequestMsg) {
        try {
            if (entityGroupEntitiesRequestMsg.getEntityGroupIdMSB() != 0 && entityGroupEntitiesRequestMsg.getEntityGroupIdLSB() != 0) {
                EntityGroupId userGroupId = new EntityGroupId(new UUID(entityGroupEntitiesRequestMsg.getEntityGroupIdMSB(), entityGroupEntitiesRequestMsg.getEntityGroupIdLSB()));
                EntityType entityGroupType = EntityType.valueOf(entityGroupEntitiesRequestMsg.getType());
                if (EntityType.USER.equals(entityGroupType)) {
                    return processUserGroupPermissionsRequest(edge, userGroupId);
                } else {
                    return processEntityGroupPermissionsRequest(edge, userGroupId, entityGroupType);
                }
            } else {
                log.warn("Received empty entity group ID MSG and LSB [{}]", entityGroupEntitiesRequestMsg);
                return Futures.immediateFuture(null);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process entity group permission request [{}]", edge.getRoutingKey(), entityGroupEntitiesRequestMsg, e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<Void> processUserGroupPermissionsRequest(Edge edge, EntityGroupId userGroupId) {
        PageData<GroupPermission> groupPermissionsData =
                groupPermissionService.findGroupPermissionByTenantIdAndUserGroupId(edge.getTenantId(), userGroupId, new PageLink(Integer.MAX_VALUE));
        if (!groupPermissionsData.getData().isEmpty()) {
            List<ListenableFuture<Void>> result = new ArrayList<>();
            for (GroupPermission groupPermission : groupPermissionsData.getData()) {
                ListenableFuture<Role> roleFuture = roleService.findRoleByIdAsync(edge.getTenantId(), groupPermission.getRoleId());
                result.add(Futures.transformAsync(roleFuture, role -> {
                    if (role != null) {
                        if (RoleType.GENERIC.equals(role.getType())) {
                            return Futures.transform(saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                    EdgeEventType.GROUP_PERMISSION, EdgeEventActionType.ADDED,
                                    groupPermission.getId(), null, null), edgeEvent -> null, dbCallbackExecutorService);
                        } else {
                            ListenableFuture<Boolean> checkFuture =
                                    entityGroupService.checkEdgeEntityGroupById(edge.getTenantId(), edge.getId(), groupPermission.getEntityGroupId(), groupPermission.getEntityGroupType());
                            return Futures.transformAsync(checkFuture, exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Futures.transform(
                                            saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                                    EdgeEventType.GROUP_PERMISSION, EdgeEventActionType.ADDED,
                                                    groupPermission.getId(), null, null),
                                            edgeEvent -> null, dbCallbackExecutorService);
                                } else {
                                    return Futures.immediateFuture(null);
                                }
                            }, dbCallbackExecutorService);
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, dbCallbackExecutorService));
            }
            return Futures.transform(Futures.allAsList(result), voids -> null, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> processEntityGroupPermissionsRequest(Edge edge, EntityGroupId entityGroupId, EntityType entityGroupType) {
        PageData<GroupPermission> groupPermissionsData =
                groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupId(edge.getTenantId(), entityGroupId, new PageLink(Integer.MAX_VALUE));
        if (!groupPermissionsData.getData().isEmpty()) {
            List<ListenableFuture<Void>> result = new ArrayList<>();
            for (GroupPermission groupPermission : groupPermissionsData.getData()) {
                ListenableFuture<Boolean> checkFuture =
                        entityGroupService.checkEdgeEntityGroupById(edge.getTenantId(), edge.getId(), groupPermission.getUserGroupId(), EntityType.USER);
                result.add(Futures.transformAsync(checkFuture, exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Futures.transform(
                                saveEdgeEvent(edge.getTenantId(), edge.getId(),
                                        EdgeEventType.GROUP_PERMISSION, EdgeEventActionType.ADDED,
                                        groupPermission.getId(), null, null),
                                edgeEvent -> null, dbCallbackExecutorService);
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, dbCallbackExecutorService));
            }
            return Futures.transform(Futures.allAsList(result), voids -> null, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private ListenableFuture<Void> syncDevices(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<DeviceId> deviceIds = entityIds.stream().map(e -> new DeviceId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<Device>> devicesFuture = deviceService.findDevicesByTenantIdAndIdsAsync(edge.getTenantId(), deviceIds);
                ListenableFuture<List<EdgeEvent>> f = Futures.transformAsync(devicesFuture, devices -> {
                    List<ListenableFuture<EdgeEvent>> result = new ArrayList<>();
                    if (devices != null && !devices.isEmpty()) {
                        log.trace("[{}] [{}] device(s) are going to be pushed to edge.", edge.getId(), devices.size());
                        for (Device device : devices) {
                            result.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.ADDED, device.getId(), null, entityGroupId));
                        }
                    }
                    return Futures.allAsList(result);
                }, dbCallbackExecutorService);
                return Futures.transform(f, l -> null, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge device(s) on sync!", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during loading edge device(s) on sync!", e));
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> syncAssets(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<AssetId> assetIds = entityIds.stream().map(e -> new AssetId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<Asset>> assetsFuture = assetService.findAssetsByTenantIdAndIdsAsync(edge.getTenantId(), assetIds);
                ListenableFuture<List<EdgeEvent>> f = Futures.transformAsync(assetsFuture, assets -> {
                    List<ListenableFuture<EdgeEvent>> result = new ArrayList<>();
                    if (assets != null && !assets.isEmpty()) {
                        log.trace("[{}] [{}] asset(s) are going to be pushed to edge.", edge.getId(), assets.size());
                        for (Asset asset : assets) {
                            result.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ASSET, EdgeEventActionType.ADDED, asset.getId(), null, entityGroupId));
                        }
                    }
                    return Futures.allAsList(result);
                }, dbCallbackExecutorService);
                return Futures.transform(f, l -> null, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge asset(s) on sync!", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during loading edge asset(s) on sync!", e));
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> syncEntityViews(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<EntityViewId> entityViewIds = entityIds.stream().map(e -> new EntityViewId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<EntityView>> entityViewsFuture = entityViewService.findEntityViewsByTenantIdAndIdsAsync(edge.getTenantId(), entityViewIds);
                ListenableFuture<List<EdgeEvent>> f = Futures.transformAsync(entityViewsFuture, entityViews -> {
                    List<ListenableFuture<EdgeEvent>> result = new ArrayList<>();
                    if (entityViews != null && !entityViews.isEmpty()) {
                        log.trace("[{}] [{}] entity view(s) are going to be pushed to edge.", edge.getId(), entityViews.size());
                        for (EntityView entityView : entityViews) {
                            result.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.ENTITY_VIEW, EdgeEventActionType.ADDED, entityView.getId(), null, entityGroupId));
                        }
                    }
                    return Futures.allAsList(result);
                }, dbCallbackExecutorService);
                return Futures.transform(f, l -> null, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge  entity view(s) on sync!", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during loading edge  entity view(s) on sync!", e));
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> syncDashboards(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<DashboardId> dashboardIds = entityIds.stream().map(e -> new DashboardId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<DashboardInfo>> dashboardInfosFuture = dashboardService.findDashboardInfoByIdsAsync(edge.getTenantId(), dashboardIds);
                ListenableFuture<List<EdgeEvent>> f = Futures.transformAsync(dashboardInfosFuture, dashboardInfos -> {
                    List<ListenableFuture<EdgeEvent>> result = new ArrayList<>();
                    if (dashboardInfos != null && !dashboardInfos.isEmpty()) {
                        log.trace("[{}] [{}] dashboard(s) are going to be pushed to edge.", edge.getId(), dashboardInfos.size());
                        for (DashboardInfo dashboardInfo : dashboardInfos) {
                            result.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.DASHBOARD, EdgeEventActionType.ADDED, dashboardInfo.getId(), null, entityGroupId));
                        }
                    }
                    return Futures.allAsList(result);
                }, dbCallbackExecutorService);
                return Futures.transform(f, l -> null, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge dashboard(s) on sync!", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during loading edge dashboard(s) on sync!", e));
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> syncUsers(Edge edge, List<EntityId> entityIds, EntityGroupId entityGroupId) {
        try {
            if (entityIds != null && !entityIds.isEmpty()) {
                List<UserId> userIds = entityIds.stream().map(e -> new UserId(e.getId())).collect(Collectors.toList());
                ListenableFuture<List<User>> usersFuture = userService.findUsersByTenantIdAndIdsAsync(edge.getTenantId(), userIds);
                ListenableFuture<List<EdgeEvent>> f = Futures.transformAsync(usersFuture, users -> {
                    List<ListenableFuture<EdgeEvent>> result = new ArrayList<>();
                    if (users != null && !users.isEmpty()) {
                        log.trace("[{}] [{}] user(s) are going to be pushed to edge.", edge.getId(), users.size());
                        for (User user : users) {
                            result.add(saveEdgeEvent(edge.getTenantId(), edge.getId(), EdgeEventType.USER, EdgeEventActionType.ADDED, user.getId(), null, entityGroupId));
                        }
                    }
                    return Futures.allAsList(result);
                }, dbCallbackExecutorService);
                return Futures.transform(f, l -> null, dbCallbackExecutorService);
            }
        } catch (Exception e) {
            log.error("Exception during loading edge user(s) on sync!", e);
            return Futures.immediateFailedFuture(new RuntimeException("Exception during loading edge user(s) on sync!", e));
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<EdgeEvent> saveEdgeEvent(TenantId tenantId,
                                                      EdgeId edgeId,
                                                      EdgeEventType type,
                                                      EdgeEventActionType action,
                                                      EntityId entityId,
                                                      JsonNode body) {
        return saveEdgeEvent(tenantId, edgeId, type, action, entityId, body, null);
    }

    private ListenableFuture<EdgeEvent> saveEdgeEvent(TenantId tenantId,
                                                      EdgeId edgeId,
                                                      EdgeEventType type,
                                                      EdgeEventActionType action,
                                                      EntityId entityId,
                                                      JsonNode body,
                                                      EntityId entityGroupId) {
        log.trace("Pushing edge event to edge queue. tenantId [{}], edgeId [{}], type [{}], action[{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId.getId());
        }
        edgeEvent.setBody(body);
        ListenableFuture<EdgeEvent> future = edgeEventService.saveAsync(edgeEvent);
        Futures.addCallback(future, new FutureCallback<EdgeEvent>() {
            @Override
            public void onSuccess(@Nullable EdgeEvent result) {
                tbClusterService.onEdgeEventUpdate(tenantId, edgeId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Can't save edge event [{}] for edge [{}]", tenantId.getId(), edgeEvent, edgeId.getId(), t);
            }
        }, dbCallbackExecutorService);
        return future;
    }
}
