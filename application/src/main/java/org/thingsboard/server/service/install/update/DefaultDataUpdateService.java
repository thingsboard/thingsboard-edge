/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNode;
import org.thingsboard.rule.engine.flow.TbRuleChainInputNodeConfiguration;
import org.thingsboard.rule.engine.profile.TbDeviceProfileNode;
import org.thingsboard.rule.engine.profile.TbDeviceProfileNodeConfiguration;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.edge.EdgeSettings;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.cloud.CloudEventDao;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.device.DeviceConnectivityConfiguration;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.model.sql.DeviceProfileEntity;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.sql.device.DeviceProfileRepository;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.component.RuleNodeClassInfo;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.SystemDataLoaderService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    private static final int MAX_PENDING_SAVE_RULE_NODE_FUTURES = 256;
    private static final int DEFAULT_PAGE_SIZE = 1024;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    private DeviceProfileRepository deviceProfileRepository;

    @Autowired
    private RateLimitsUpdater rateLimitsUpdater;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private CloudEventService cloudEventService;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Lazy
    @Autowired
    private QueueService queueService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private EventService eventService;

    @Autowired
    private AuditLogDao auditLogDao;

    @Autowired
    private EdgeEventDao edgeEventDao;

    @Autowired
    private CloudEventDao cloudEventDao;

    @Autowired
    JpaExecutorService jpaExecutorService;

    @Autowired
    AdminSettingsService adminSettingsService;

    @Autowired
    DeviceConnectivityConfiguration connectivityConfiguration;

    @Override
    public void updateData(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "1.4.0":
                log.info("Updating data from version 1.4.0 to 2.0.0 ...");
                tenantsDefaultRuleChainUpdater.updateEntities(null);
                break;
            case "3.0.1":
                log.info("Updating data from version 3.0.1 to 3.1.0 ...");
                tenantsEntityViewsUpdater.updateEntities(null);
                break;
            case "3.1.1":
                log.info("Updating data from version 3.1.1 to 3.2.0 ...");
                tenantsRootRuleChainUpdater.updateEntities(null);
                break;
            case "3.2.2":
                log.info("Updating data from version 3.2.2 to 3.3.0 ...");
                tenantsDefaultEdgeRuleChainUpdater.updateEntities(null);
                tenantsAlarmsCustomerUpdater.updateEntities(null);
                deviceProfileEntityDynamicConditionsUpdater.updateEntities(null);
                updateOAuth2Params();
                break;
            case "3.3.2":
                log.info("Updating data from version 3.3.2 to 3.3.3 ...");
                updateNestedRuleChains();
                break;
            case "3.3.4":
                log.info("Updating data from version 3.3.4 to 3.4.0 ...");
                tenantsProfileQueueConfigurationUpdater.updateEntities();
                rateLimitsUpdater.updateEntities();
                break;
            case "3.4.0":
                boolean skipEventsMigration = getEnv("TB_SKIP_EVENTS_MIGRATION", false);
                if (!skipEventsMigration) {
                    log.info("Updating data from version 3.4.0 to 3.4.1 ...");
                    eventService.migrateEvents();
                }
                break;
            case "3.4.1":
                log.info("Updating data from version 3.4.1 to 3.4.2 ...");
                systemDataLoaderService.saveLegacyYmlSettings();
                boolean skipAuditLogsMigration = getEnv("TB_SKIP_AUDIT_LOGS_MIGRATION", false);
                if (!skipAuditLogsMigration) {
                    log.info("Starting audit logs migration. Can be skipped with TB_SKIP_AUDIT_LOGS_MIGRATION env variable set to true");
                    auditLogDao.migrateAuditLogs();
                } else {
                    log.info("Skipping audit logs migration");
                }
                migrateEdgeEvents("Starting edge events migration. ");
                break;
            case "3.4.4":
                log.info("Updating data from version 3.4.4 to 3.5.0 ...");

                boolean skipCloudEventsMigration = getEnv("TB_SKIP_CLOUD_EVENTS_MIGRATION", false);
                if (!skipCloudEventsMigration) {
                    log.info("Starting cloud events migration. Can be skipped with TB_SKIP_CLOUD_EVENTS_MIGRATION env variable set to true");
                    cloudEventDao.migrateCloudEvents();
                } else {
                    log.info("Skipping cloud events migration");
                }
                break;
            case "3.5.1":
                log.info("Updating data from version 3.5.1 to 3.6.0 ...");
                migrateEdgeEvents("Starting edge events migration - adding seq_id column. ");
                break;
            case "3.6.0":
                log.info("Updating data from version 3.6.0 to 3.6.1 ...");
                migrateDeviceConnectivity();
                break;
            case "edge":
                // remove this line in 4+ release
                fixDuplicateSystemWidgetsBundles();

                // reset full sync required - to upload latest widgets from cloud
                tenantsFullSyncRequiredUpdater.updateEntities(null);
                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private void migrateEdgeEvents(String logPrefix) {
        boolean skipEdgeEventsMigration = getEnv("TB_SKIP_EDGE_EVENTS_MIGRATION", false);
        if (!skipEdgeEventsMigration) {
            log.info(logPrefix + "Can be skipped with TB_SKIP_EDGE_EVENTS_MIGRATION env variable set to true");
            edgeEventDao.migrateEdgeEvents();
        } else {
            log.info("Skipping edge events migration");
        }
    }

    private void migrateDeviceConnectivity() {
        if (adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "connectivity") == null) {
            AdminSettings connectivitySettings = new AdminSettings();
            connectivitySettings.setTenantId(TenantId.SYS_TENANT_ID);
            connectivitySettings.setKey("connectivity");
            connectivitySettings.setJsonValue(JacksonUtil.valueToTree(connectivityConfiguration.getConnectivity()));
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, connectivitySettings);
        }
    }

    @Override
    public void upgradeRuleNodes() {
        try {
            int totalRuleNodesUpgraded = 0;
            log.info("Starting rule nodes upgrade ...");
            var nodeClassToVersionMap = componentDiscoveryService.getVersionedNodes();
            log.debug("Found {} versioned nodes to check for upgrade!", nodeClassToVersionMap.size());
            for (var ruleNodeClassInfo : nodeClassToVersionMap) {
                var ruleNodeTypeForLogs = ruleNodeClassInfo.getSimpleName();
                var toVersion = ruleNodeClassInfo.getCurrentVersion();
                log.debug("Going to check for nodes with type: {} to upgrade to version: {}.", ruleNodeTypeForLogs, toVersion);
                var ruleNodesIdsToUpgrade = getRuleNodesIdsWithTypeAndVersionLessThan(ruleNodeClassInfo.getClassName(), toVersion);
                if (ruleNodesIdsToUpgrade.isEmpty()) {
                    log.debug("There are no active nodes with type {}, or all nodes with this type already set to latest version!", ruleNodeTypeForLogs);
                    continue;
                }
                var ruleNodeIdsPartitions = Lists.partition(ruleNodesIdsToUpgrade, MAX_PENDING_SAVE_RULE_NODE_FUTURES);
                for (var ruleNodePack : ruleNodeIdsPartitions) {
                    totalRuleNodesUpgraded += processRuleNodePack(ruleNodePack, ruleNodeClassInfo);
                    log.info("{} upgraded rule nodes so far ...", totalRuleNodesUpgraded);
                }
            }
            log.info("Finished rule nodes upgrade. Upgraded rule nodes count: {}", totalRuleNodesUpgraded);
        } catch (Exception e) {
            log.error("Unexpected error during rule nodes upgrade: ", e);
        }
    }

    private int processRuleNodePack(List<RuleNodeId> ruleNodeIdsBatch, RuleNodeClassInfo ruleNodeClassInfo) {
        var saveFutures = new ArrayList<ListenableFuture<?>>(MAX_PENDING_SAVE_RULE_NODE_FUTURES);
        String ruleNodeType = ruleNodeClassInfo.getSimpleName();
        int toVersion = ruleNodeClassInfo.getCurrentVersion();
        var ruleNodesPack = ruleChainService.findAllRuleNodesByIds(ruleNodeIdsBatch);
        for (var ruleNode : ruleNodesPack) {
            if (ruleNode == null) {
                continue;
            }
            var ruleNodeId = ruleNode.getId();
            var oldConfiguration = ruleNode.getConfiguration();
            int fromVersion = ruleNode.getConfigurationVersion();
            log.debug("Going to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                    ruleNodeId, ruleNodeType, fromVersion, toVersion);
            try {
                var tbVersionedNode = (TbNode) ruleNodeClassInfo.getClazz().getDeclaredConstructor().newInstance();
                TbPair<Boolean, JsonNode> upgradeRuleNodeConfigurationResult = tbVersionedNode.upgrade(fromVersion, oldConfiguration);
                if (upgradeRuleNodeConfigurationResult.getFirst()) {
                    ruleNode.setConfiguration(upgradeRuleNodeConfigurationResult.getSecond());
                }
                ruleNode.setConfigurationVersion(toVersion);
                saveFutures.add(jpaExecutorService.submit(() -> {
                    ruleChainService.saveRuleNode(TenantId.SYS_TENANT_ID, ruleNode);
                    log.debug("Successfully upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                            ruleNodeId, ruleNodeType, fromVersion, toVersion);
                }));
            } catch (Exception e) {
                log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {} due to: ",
                        ruleNodeId, ruleNodeType, fromVersion, toVersion, e);
            }
        }
        try {
            return Futures.allAsList(saveFutures).get().size();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to process save rule nodes requests due to: ", e);
        }
    }

    private List<RuleNodeId> getRuleNodesIdsWithTypeAndVersionLessThan(String type, int toVersion) {
        var ruleNodeIds = new ArrayList<RuleNodeId>();
        new PageDataIterable<>(pageLink ->
                ruleChainService.findAllRuleNodeIdsByTypeAndVersionLessThan(type, toVersion, pageLink), DEFAULT_PAGE_SIZE
        ).forEach(ruleNodeIds::add);
        return ruleNodeIds;
    }

    private final PaginatedUpdater<String, DeviceProfileEntity> deviceProfileEntityDynamicConditionsUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Device Profile Entity Dynamic Conditions Updater";
                }

                @Override
                protected PageData<DeviceProfileEntity> findEntities(String id, PageLink pageLink) {
                    return DaoUtil.pageToPageData(deviceProfileRepository.findAll(DaoUtil.toPageable(pageLink)));
                }

                @Override
                protected void updateEntity(DeviceProfileEntity deviceProfile) {
                    if (convertDeviceProfileForVersion330(deviceProfile.getProfileData())) {
                        deviceProfileRepository.save(deviceProfile);
                    }
                }
            };

    boolean convertDeviceProfileForVersion330(JsonNode profileData) {
        boolean isUpdated = false;
        if (profileData.has("alarms") && !profileData.get("alarms").isNull()) {
            JsonNode alarms = profileData.get("alarms");
            for (JsonNode alarm : alarms) {
                if (alarm.has("createRules")) {
                    JsonNode createRules = alarm.get("createRules");
                    for (AlarmSeverity severity : AlarmSeverity.values()) {
                        if (createRules.has(severity.name())) {
                            JsonNode spec = createRules.get(severity.name()).get("condition").get("spec");
                            if (convertDeviceProfileAlarmRulesForVersion330(spec)) {
                                isUpdated = true;
                            }
                        }
                    }
                }
                if (alarm.has("clearRule") && !alarm.get("clearRule").isNull()) {
                    JsonNode spec = alarm.get("clearRule").get("condition").get("spec");
                    if (convertDeviceProfileAlarmRulesForVersion330(spec)) {
                        isUpdated = true;
                    }
                }
            }
        }
        return isUpdated;
    }

    private final PaginatedUpdater<String, Tenant> tenantsDefaultRuleChainUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants default rule chain updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain ruleChain = ruleChainService.getRootTenantRuleChain(tenant.getId());
                        if (ruleChain == null) {
                            installScripts.createDefaultRuleChains(tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private void updateNestedRuleChains() {
        try {
            var updated = 0;
            boolean hasNext = true;
            while (hasNext) {
                List<EntityRelation> relations = relationService.findRuleNodeToRuleChainRelations(TenantId.SYS_TENANT_ID, RuleChainType.CORE, DEFAULT_PAGE_SIZE);
                hasNext = relations.size() == DEFAULT_PAGE_SIZE;
                for (EntityRelation relation : relations) {
                    try {
                        RuleNodeId sourceNodeId = new RuleNodeId(relation.getFrom().getId());
                        RuleNode sourceNode = ruleChainService.findRuleNodeById(TenantId.SYS_TENANT_ID, sourceNodeId);
                        if (sourceNode == null) {
                            log.info("Skip processing of relation for non existing source rule node: [{}]", sourceNodeId);
                            relationService.deleteRelation(TenantId.SYS_TENANT_ID, relation);
                            continue;
                        }
                        RuleChainId sourceRuleChainId = sourceNode.getRuleChainId();
                        RuleChainId targetRuleChainId = new RuleChainId(relation.getTo().getId());
                        RuleChain targetRuleChain = ruleChainService.findRuleChainById(TenantId.SYS_TENANT_ID, targetRuleChainId);
                        if (targetRuleChain == null) {
                            log.info("Skip processing of relation for non existing target rule chain: [{}]", targetRuleChainId);
                            relationService.deleteRelation(TenantId.SYS_TENANT_ID, relation);
                            continue;
                        }
                        TenantId tenantId = targetRuleChain.getTenantId();
                        RuleNode targetNode = new RuleNode();
                        targetNode.setName(targetRuleChain.getName());
                        targetNode.setRuleChainId(sourceRuleChainId);
                        targetNode.setType(TbRuleChainInputNode.class.getName());
                        TbRuleChainInputNodeConfiguration configuration = new TbRuleChainInputNodeConfiguration();
                        configuration.setRuleChainId(targetRuleChain.getId().toString());
                        targetNode.setConfiguration(JacksonUtil.valueToTree(configuration));
                        targetNode.setAdditionalInfo(relation.getAdditionalInfo());
                        targetNode.setDebugMode(false);
                        targetNode = ruleChainService.saveRuleNode(tenantId, targetNode);

                        EntityRelation sourceRuleChainToRuleNode = new EntityRelation();
                        sourceRuleChainToRuleNode.setFrom(sourceRuleChainId);
                        sourceRuleChainToRuleNode.setTo(targetNode.getId());
                        sourceRuleChainToRuleNode.setType(EntityRelation.CONTAINS_TYPE);
                        sourceRuleChainToRuleNode.setTypeGroup(RelationTypeGroup.RULE_CHAIN);
                        relationService.saveRelation(tenantId, sourceRuleChainToRuleNode);

                        EntityRelation sourceRuleNodeToTargetRuleNode = new EntityRelation();
                        sourceRuleNodeToTargetRuleNode.setFrom(sourceNode.getId());
                        sourceRuleNodeToTargetRuleNode.setTo(targetNode.getId());
                        sourceRuleNodeToTargetRuleNode.setType(relation.getType());
                        sourceRuleNodeToTargetRuleNode.setTypeGroup(RelationTypeGroup.RULE_NODE);
                        sourceRuleNodeToTargetRuleNode.setAdditionalInfo(relation.getAdditionalInfo());
                        relationService.saveRelation(tenantId, sourceRuleNodeToTargetRuleNode);

                        //Delete old relation
                        relationService.deleteRelation(tenantId, relation);
                        updated++;
                    } catch (Exception e) {
                        log.info("Failed to update RuleNodeToRuleChainRelation: {}", relation, e);
                    }
                }
                if (updated > 0) {
                    log.info("RuleNodeToRuleChainRelations: {} entities updated so far...", updated);
                }
            }
        } catch (Exception e) {
            log.error("Unable to update Tenant", e);
        }
    }

    private final PaginatedUpdater<String, Tenant> tenantsDefaultEdgeRuleChainUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants default edge rule chain updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain defaultEdgeRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(tenant.getId());
                        if (defaultEdgeRuleChain == null) {
                            installScripts.createDefaultEdgeRuleChains(tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private final PaginatedUpdater<String, Tenant> tenantsRootRuleChainUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants root rule chain updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain ruleChain = ruleChainService.getRootTenantRuleChain(tenant.getId());
                        if (ruleChain == null) {
                            installScripts.createDefaultRuleChains(tenant.getId());
                        } else {
                            RuleChainMetaData md = ruleChainService.loadRuleChainMetaData(tenant.getId(), ruleChain.getId());
                            int oldIdx = md.getFirstNodeIndex();
                            int newIdx = md.getNodes().size();

                            if (md.getNodes().size() < oldIdx) {
                                // Skip invalid rule chains
                                return;
                            }

                            RuleNode oldFirstNode = md.getNodes().get(oldIdx);
                            if (oldFirstNode.getType().equals(TbDeviceProfileNode.class.getName())) {
                                // No need to update the rule node twice.
                                return;
                            }

                            RuleNode ruleNode = new RuleNode();
                            ruleNode.setRuleChainId(ruleChain.getId());
                            ruleNode.setName("Device Profile Node");
                            ruleNode.setType(TbDeviceProfileNode.class.getName());
                            ruleNode.setDebugMode(false);
                            TbDeviceProfileNodeConfiguration ruleNodeConfiguration = new TbDeviceProfileNodeConfiguration().defaultConfiguration();
                            ruleNode.setConfiguration(JacksonUtil.valueToTree(ruleNodeConfiguration));
                            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                            additionalInfo.put("description", "Process incoming messages from devices with the alarm rules defined in the device profile. Dispatch all incoming messages with \"Success\" relation type.");
                            additionalInfo.put("layoutX", 204);
                            additionalInfo.put("layoutY", 240);
                            ruleNode.setAdditionalInfo(additionalInfo);

                            md.getNodes().add(ruleNode);
                            md.setFirstNodeIndex(newIdx);
                            md.addConnectionInfo(newIdx, oldIdx, TbNodeConnectionType.SUCCESS);
                            ruleChainService.saveRuleChainMetaData(tenant.getId(), md, Function.identity());
                        }
                    } catch (Exception e) {
                        log.error("[{}] Unable to update Tenant: {}", tenant.getId(), tenant.getName(), e);
                    }
                }
            };

    private final PaginatedUpdater<String, Tenant> tenantsEntityViewsUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants entity views updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    updateTenantEntityViews(tenant.getId());
                }
            };

    private void updateTenantEntityViews(TenantId tenantId) {
        PageLink pageLink = new PageLink(100);
        PageData<EntityView> pageData = entityViewService.findEntityViewByTenantId(tenantId, pageLink);
        boolean hasNext = true;
        while (hasNext) {
            List<ListenableFuture<List<Void>>> updateFutures = new ArrayList<>();
            for (EntityView entityView : pageData.getData()) {
                updateFutures.add(updateEntityViewLatestTelemetry(entityView));
            }

            try {
                Futures.allAsList(updateFutures).get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to copy latest telemetry to entity view", e);
            }

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
                pageData = entityViewService.findEntityViewByTenantId(tenantId, pageLink);
            } else {
                hasNext = false;
            }
        }
    }

    private ListenableFuture<List<Void>> updateEntityViewLatestTelemetry(EntityView entityView) {
        EntityViewId entityId = entityView.getId();
        List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                entityView.getKeys().getTimeseries() : Collections.emptyList();
        long startTs = entityView.getStartTimeMs();
        long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
        ListenableFuture<List<String>> keysFuture;
        if (keys.isEmpty()) {
            keysFuture = Futures.transform(tsService.findAllLatest(TenantId.SYS_TENANT_ID,
                    entityView.getEntityId()), latest -> latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList()), MoreExecutors.directExecutor());
        } else {
            keysFuture = Futures.immediateFuture(keys);
        }
        ListenableFuture<List<TsKvEntry>> latestFuture = Futures.transformAsync(keysFuture, fetchKeys -> {
            List<ReadTsKvQuery> queries = fetchKeys.stream().filter(key -> !isBlank(key)).map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 1, "DESC")).collect(Collectors.toList());
            if (!queries.isEmpty()) {
                return tsService.findAll(TenantId.SYS_TENANT_ID, entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
        return Futures.transformAsync(latestFuture, latestValues -> {
            if (latestValues != null && !latestValues.isEmpty()) {
                ListenableFuture<List<Void>> saveFuture = tsService.saveLatest(TenantId.SYS_TENANT_ID, entityId, latestValues);
                return saveFuture;
            }
            return Futures.immediateFuture(null);
        }, MoreExecutors.directExecutor());
    }

    private final PaginatedUpdater<String, Tenant> tenantsAlarmsCustomerUpdater =
            new PaginatedUpdater<>() {

                final AtomicLong processed = new AtomicLong();

                @Override
                protected String getName() {
                    return "Tenants alarms customer updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    updateTenantAlarmsCustomer(tenant.getId(), getName(), processed);
                }
            };

    private void updateTenantAlarmsCustomer(TenantId tenantId, String name, AtomicLong processed) {
        AlarmQuery alarmQuery = new AlarmQuery(null, new TimePageLink(1000), null, null, null, false);
        PageData<AlarmInfo> alarms = alarmDao.findAlarms(tenantId, alarmQuery);
        boolean hasNext = true;
        while (hasNext) {
            for (Alarm alarm : alarms.getData()) {
                if (alarm.getCustomerId() == null && alarm.getOriginator() != null) {
                    alarm.setCustomerId(entityService.fetchEntityCustomerId(tenantId, alarm.getOriginator()).get());
                    alarmDao.save(tenantId, alarm);
                }
                if (processed.incrementAndGet() % 1000 == 0) {
                    log.info("{}: {} alarms processed so far...", name, processed);
                }
            }
            if (alarms.hasNext()) {
                alarmQuery.setPageLink(alarmQuery.getPageLink().nextPageLink());
                alarms = alarmDao.findAlarms(tenantId, alarmQuery);
            } else {
                hasNext = false;
            }
        }
    }

    boolean convertDeviceProfileAlarmRulesForVersion330(JsonNode spec) {
        if (spec != null) {
            if (spec.has("type") && spec.get("type").asText().equals("DURATION")) {
                if (spec.has("value")) {
                    long value = spec.get("value").asLong();
                    var predicate = new FilterPredicateValue<>(
                            value, null, new DynamicValue<>(null, null, false)
                    );
                    ((ObjectNode) spec).remove("value");
                    ((ObjectNode) spec).putPOJO("predicate", predicate);
                    return true;
                }
            } else if (spec.has("type") && spec.get("type").asText().equals("REPEATING")) {
                if (spec.has("count")) {
                    int count = spec.get("count").asInt();
                    var predicate = new FilterPredicateValue<>(
                            count, null, new DynamicValue<>(null, null, false)
                    );
                    ((ObjectNode) spec).remove("count");
                    ((ObjectNode) spec).putPOJO("predicate", predicate);
                    return true;
                }
            }
        }
        return false;
    }

    private void updateOAuth2Params() {
        log.warn("CAUTION: Update of Oauth2 parameters from 3.2.2 to 3.3.0 available only in ThingsBoard versions 3.3.0/3.3.1");
    }

    private final PaginatedUpdater<String, TenantProfile> tenantsProfileQueueConfigurationUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenant profiles queue configuration updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<TenantProfile> findEntities(String id, PageLink pageLink) {
                    return tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
                }

                @Override
                protected void updateEntity(TenantProfile tenantProfile) {
                    updateTenantProfileQueueConfiguration(tenantProfile);
                }
            };

    private void updateTenantProfileQueueConfiguration(TenantProfile profile) {
        try {
            List<TenantProfileQueueConfiguration> queueConfiguration = profile.getProfileData().getQueueConfiguration();
            if (profile.isIsolatedTbRuleEngine() && (queueConfiguration == null || queueConfiguration.isEmpty())) {
                TenantProfileQueueConfiguration mainQueueConfig = getMainQueueConfiguration();
                profile.getProfileData().setQueueConfiguration(Collections.singletonList((mainQueueConfig)));
                tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, profile);
                List<TenantId> isolatedTenants = tenantService.findTenantIdsByTenantProfileId(profile.getId());
                isolatedTenants.forEach(tenantId -> {
                    queueService.saveQueue(new Queue(tenantId, mainQueueConfig));
                });
            }
        } catch (Exception e) {
            log.error("Failed to update tenant profile queue configuration name=[" + profile.getName() + "], id=[" + profile.getId().getId() + "]", e);
        }
    }

    private TenantProfileQueueConfiguration getMainQueueConfiguration() {
        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        return mainQueueConfiguration;
    }

    public static boolean getEnv(String name, boolean defaultValue) {
        String env = System.getenv(name);
        if (env == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(env);
        }
    }

    private void fixDuplicateSystemWidgetsBundles() {
        try {
            List<WidgetsBundle> systemWidgetsBundles = widgetsBundleService.findSystemWidgetsBundles(TenantId.SYS_TENANT_ID);
            for (WidgetsBundle widgetsBundle : systemWidgetsBundles) {
                try {
                    widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, widgetsBundle.getAlias());
                } catch (IncorrectResultSizeDataAccessException e) {
                    // fix for duplicate entries of system widgets
                    for (WidgetsBundle systemWidgetsBundle : systemWidgetsBundles) {
                        if (systemWidgetsBundle.getAlias().equals(widgetsBundle.getAlias())) {
                            widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, systemWidgetsBundle.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to fix duplicate system widgets bundles", e);
        }
    }

    private final PaginatedUpdater<String, Tenant> tenantsFullSyncRequiredUpdater =
            new PaginatedUpdater<>() {

                @Override
                protected String getName() {
                    return "Tenants edge full sync required updater";
                }

                @Override
                protected boolean forceReportTotal() {
                    return true;
                }

                @Override
                protected PageData<Tenant> findEntities(String region, PageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EdgeSettings edgeSettings = cloudEventService.findEdgeSettings(tenant.getId());
                        if (edgeSettings != null) {
                            edgeSettings.setFullSyncRequired(true);
                            cloudEventService.saveEdgeSettings(tenant.getId(), edgeSettings);
                        } else {
                            log.warn("Edge settings not found for tenant: {}", tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };
}
