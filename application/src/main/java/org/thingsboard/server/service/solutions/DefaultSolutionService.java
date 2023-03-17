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
package org.thingsboard.server.service.solutions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.entitiy.asset.TbAssetService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.entitiy.entity.group.TbEntityGroupService;
import org.thingsboard.server.service.entitiy.entity.relation.TbEntityRelationService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.solutions.data.CreatedEntityInfo;
import org.thingsboard.server.service.solutions.data.DashboardLinkInfo;
import org.thingsboard.server.service.solutions.data.DeviceCredentialsInfo;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.data.UserCredentialsInfo;
import org.thingsboard.server.service.solutions.data.definition.AssetDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerEntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardUserDetailsDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceDefinition;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.definition.GroupRoleDefinition;
import org.thingsboard.server.service.solutions.data.definition.ReferenceableEntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.definition.RoleDefinition;
import org.thingsboard.server.service.solutions.data.definition.SchedulerEventDefinition;
import org.thingsboard.server.service.solutions.data.definition.TenantDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserGroupDefinition;
import org.thingsboard.server.service.solutions.data.emulator.AssetEmulatorLauncher;
import org.thingsboard.server.service.solutions.data.emulator.DeviceEmulatorLauncher;
import org.thingsboard.server.service.solutions.data.names.RandomNameData;
import org.thingsboard.server.service.solutions.data.names.RandomNameUtil;
import org.thingsboard.server.service.solutions.data.solution.SolutionDescriptor;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplate;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.SolutionTemplateLevel;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateDetails;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInfo;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultSolutionService implements SolutionService {

    public static final String SOLUTIONS_DIR = "solutions";

    private static final Map<SolutionTemplateLevel, Set<String>> allowedSolutionTemplateLevelsMap = new HashMap<>();

    static {
        allowedSolutionTemplateLevelsMap.put(SolutionTemplateLevel.MAKER, Set.of("Maker", "Prototype", "Startup", "Business"));
        allowedSolutionTemplateLevelsMap.put(SolutionTemplateLevel.PROTOTYPE, Set.of("Prototype", "Startup", "Business"));
        allowedSolutionTemplateLevelsMap.put(SolutionTemplateLevel.STARTUP, Set.of("Startup", "Business"));
    }

    private List<SolutionTemplateInfo> solutions = new ArrayList<>();
    private Map<String, SolutionTemplateDetails> solutionsMap = new HashMap<>();

    private final InstallScripts installScripts;
    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;
    private final RuleChainService ruleChainService;
    private final AttributesService attributesService;
    private final TimeseriesService tsService;
    private final DashboardService dashboardService;
    private final TbEntityRelationService relationService;
    private final DeviceService deviceService;
    private final TbDeviceService tbDeviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AssetService assetService;
    private final TbAssetService tbAssetService;
    private final CustomerService customerService;
    private final UserService userService;
    private final EntityGroupService entityGroupService;
    private final TbEntityGroupService tbEntityGroupService;
    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;
    private final SystemSecurityService systemSecurityService;
    private final TbClusterService tbClusterService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TbQueueProducerProvider tbQueueProducerProvider;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    private final TelemetrySubscriptionService tsSubService;
    private final EntityActionService entityActionService;
    private final AlarmService alarmService;
    private final SchedulerEventService schedulerEventService;
    private final SchedulerService schedulerService;
    private final ExecutorService emulatorExecutor = ThingsBoardExecutors.newWorkStealingPool(10, getClass());

    @PostConstruct
    public void init() {
        Path solutionsDescriptorsFile = resolve("solutions.json");
        List<SolutionDescriptor> descriptors = JacksonUtil.readValue(solutionsDescriptorsFile.toFile(), new TypeReference<>() {
        });
        for (SolutionDescriptor descriptor : descriptors) {
            SolutionTemplateInfo templateInfo = new SolutionTemplateInfo();
            templateInfo.setId(descriptor.getId());
            templateInfo.setTitle(descriptor.getTitle());
            templateInfo.setLevel(descriptor.getLevel());
            templateInfo.setInstallTimeoutMs(descriptor.getInstallTimeoutMs());
            templateInfo.setShortDescription(readFile(resolve(descriptor.getId(), "short.md")));
            templateInfo.setPreviewImageUrl(descriptor.getPreviewImageUrl());
            templateInfo.setTenantTelemetryKeys(descriptor.getTenantTelemetryKeys());
            templateInfo.setTenantAttributeKeys(descriptor.getTenantAttributeKeys());
            solutions.add(templateInfo);

            SolutionTemplateDetails templateDetails = new SolutionTemplateDetails();
            templateDetails.setId(descriptor.getId());
            templateDetails.setTitle(descriptor.getTitle());
            templateDetails.setLevel(descriptor.getLevel());
            templateDetails.setInstallTimeoutMs(descriptor.getInstallTimeoutMs());
            templateDetails.setHighlights(readFile(resolve(descriptor.getId(), "highlights.md")));
            templateDetails.setDescription(readFile(resolve(descriptor.getId(), "description.md")));
            templateDetails.setImageUrls(descriptor.getImageUrls());
            templateDetails.setTenantTelemetryKeys(descriptor.getTenantTelemetryKeys());
            templateDetails.setTenantAttributeKeys(descriptor.getTenantAttributeKeys());
            solutionsMap.put(descriptor.getId(), templateDetails);
        }
    }

    @PreDestroy
    private void destroy() {
        emulatorExecutor.shutdownNow();
    }

    private Path resolve(String subdir, String... subdirs) {
        return getSolutionsDir().resolve(Paths.get(subdir, subdirs));
    }

    private Path getSolutionsDir() {
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, SOLUTIONS_DIR);
    }

    private String readFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<TenantSolutionTemplateInfo> getSolutionInfos(TenantId tenantId) throws ThingsboardException {
        try {
            List<String> solutionIds = solutions.stream().map(SolutionTemplate::getId).collect(Collectors.toList());
            List<AttributeKvEntry> stateList = attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, solutionIds.stream().map(this::toStatusKey).collect(Collectors.toList())).get();

            List<TenantSolutionTemplateInfo> result = new ArrayList<>();

            solutions.forEach(solution -> {
                boolean installed = stateList.stream()
                        .filter(attr -> attr.getKey().equals(toStatusKey(solution.getId())) && attr.getBooleanValue().isPresent())
                        .map(attr -> attr.getBooleanValue().get()).findFirst().orElse(Boolean.FALSE);
                result.add(new TenantSolutionTemplateInfo(solution, installed));
            });
            return result;
        } catch (Exception e) {
            log.error("[{}] Failed to fetch solution list", tenantId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public TenantSolutionTemplateDetails getSolutionDetails(TenantId tenantId, String id) throws ThingsboardException {
        try {
            SolutionTemplateDetails details = solutionsMap.get(id);
            Optional<AttributeKvEntry> state = attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, toStatusKey(id)).get();
            return new TenantSolutionTemplateDetails(details, state.isPresent() && state.get().getBooleanValue().orElse(Boolean.FALSE));
        } catch (Exception e) {
            log.error("[{}] Failed to fetch solution list", tenantId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public TenantSolutionTemplateInstructions getSolutionInstructions(TenantId tenantId, String id) throws ThingsboardException {
        try {
            Optional<AttributeKvEntry> state = attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, toInstructionsKey(id)).get();
            String body = state.orElseThrow(() -> new ThingsboardRuntimeException(ThingsboardErrorCode.ITEM_NOT_FOUND))
                    .getStrValue().orElseThrow(() -> new ThingsboardRuntimeException(ThingsboardErrorCode.ITEM_NOT_FOUND));
            return JacksonUtil.fromString(body, TenantSolutionTemplateInstructions.class);
        } catch (ThingsboardRuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] Failed to fetch solution list", tenantId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public SolutionInstallResponse installSolution(User user, TenantId tenantId, String solutionId, HttpServletRequest request) throws ThingsboardException {
        if (!solutionsMap.containsKey(solutionId)) {
            throw new ThingsboardException("Solution does not exist", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }

        SolutionInstallResponse validateResult = validateSolution(tenantId, solutionId);
        if (validateResult != null && !validateResult.isSuccess()) {
            return validateResult;
        } else {
            return doInstallSolution(user, tenantId, solutionId, request);
        }
    }

    @Override
    public void deleteSolution(TenantId tenantId, String solutionId, User user) throws ThingsboardException {
        try {
            Optional<AttributeKvEntry> entitiesOpt = attributesService.find(tenantId, tenantId, DataConstants.SERVER_SCOPE, toCreatedEntitiesKey(solutionId)).get();
            if (entitiesOpt.isPresent()) {
                String entitiesListSrc = entitiesOpt.get().getValueAsString();
                List<EntityId> entityIds = new ArrayList<>(Objects.requireNonNull(JacksonUtil.fromString(entitiesListSrc, new TypeReference<List<EntityId>>() {
                })));
                // Delete elements in the descending order of their creation to make sure we don't have dependencies.
                Collections.reverse(entityIds);
                for (EntityId entityId : entityIds) {
                    try {
                        deleteEntity(tenantId, entityId, user);
                    } catch (RuntimeException e) {
                        log.error("[{}][{}] Failed to delete the entity: {}", tenantId, solutionId, entityId, e);
                    }
                }

                attributesService.removeAll(tenantId, tenantId, DataConstants.SERVER_SCOPE, Arrays.asList(toCreatedEntitiesKey(solutionId), toStatusKey(solutionId), toInstructionsKey(solutionId))).get();

                SolutionTemplateDetails solutionTemplate = solutionsMap.get(solutionId);
                List<String> tsKeys = solutionTemplate.getTenantTelemetryKeys();
                if (tsKeys != null && !tsKeys.isEmpty()) {
                    List<DeleteTsKvQuery> queries = new ArrayList<>(tsKeys.size());
                    for (String tsKey : tsKeys) {
                        queries.add(new BaseDeleteTsKvQuery(tsKey, 0, System.currentTimeMillis(), false));
                    }
                    tsService.remove(tenantId, tenantId, queries).get();
                }
                List<String> attrKeys = solutionTemplate.getTenantAttributeKeys();
                if (tsKeys != null && !tsKeys.isEmpty()) {
                    attributesService.removeAll(tenantId, tenantId, DataConstants.SERVER_SCOPE, attrKeys).get();
                }
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to delete the solution", tenantId, solutionId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    private SolutionInstallResponse validateSolution(TenantId tenantId, String solutionId) {
        Map<EntityType, List<HasName>> alreadyExistingEntities = new HashMap<>();

        //TODO: check that enough entities in subscription plan.
        //TODO: check other entities.

        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(solutionId, "rule_chains.json", new TypeReference<>() {
        });
        if (!ruleChains.isEmpty()) {
            for (ReferenceableEntityDefinition ruleChain : ruleChains) {
                List<RuleChain> savedRuleChains = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, new PageLink(1, 0, ruleChain.getName())).getData();
                if (savedRuleChains != null && !savedRuleChains.isEmpty()) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.RULE_CHAIN, key -> new ArrayList<>()).add(savedRuleChains.get(0));
                }
            }
        }

        List<DeviceProfile> deviceProfiles = loadListOfEntitiesIfFileExists(solutionId, "device_profiles.json", new TypeReference<>() {
        });
        deviceProfiles.addAll(loadListOfEntitiesFromDirectory(solutionId, "device_profiles", DeviceProfile.class));
        // Validate that entities with such name does not exist entities
        if (!deviceProfiles.isEmpty()) {
            for (DeviceProfile deviceProfile : deviceProfiles) {
                DeviceProfile savedProfile = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfile.getName());
                if (savedProfile != null) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.DEVICE_PROFILE, key -> new ArrayList<>()).add(savedProfile);
                }
            }
        }

        List<AssetProfile> assetProfiles = loadListOfEntitiesIfFileExists(solutionId, "asset_profiles.json", new TypeReference<>() {
        });
        assetProfiles.addAll(loadListOfEntitiesFromDirectory(solutionId, "asset_profiles", AssetProfile.class));
        // Validate that entities with such name does not exist entities
        if (!assetProfiles.isEmpty()) {
            for (AssetProfile assetProfile : assetProfiles) {
                AssetProfile savedProfile = assetProfileService.findAssetProfileByName(tenantId, assetProfile.getName());
                if (savedProfile != null) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.ASSET_PROFILE, key -> new ArrayList<>()).add(savedProfile);
                }
            }
        }

        List<DashboardDefinition> dashboards = loadListOfEntitiesIfFileExists(solutionId, "dashboards.json", new TypeReference<>() {
        });
        if (!dashboards.isEmpty()) {
            for (DashboardDefinition dashboard : dashboards) {
                List<DashboardInfo> savedDashboards = dashboardService.findDashboardsByTenantId(tenantId, new PageLink(1, 0, dashboard.getName())).getData();
                if (savedDashboards != null && !savedDashboards.isEmpty()) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.DASHBOARD, key -> new ArrayList<>()).add(savedDashboards.get(0));
                }
            }
        }
        if (!alreadyExistingEntities.isEmpty()) {
            SolutionInstallResponse solutionInstallResponse = new SolutionInstallResponse();
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append("## Validation failed").append(System.lineSeparator()).append(System.lineSeparator());
            alreadyExistingEntities.forEach((type, list) -> detailsBuilder.append("The following **").append(getTypeLabel(type)).append("** entities already exist: ")
                    .append(list.stream().map(HasName::getName).map(name -> "'" + name + "'").collect(Collectors.joining(","))).append(";")
                    .append(System.lineSeparator()).append(System.lineSeparator()));
            solutionInstallResponse.setSuccess(false);
            solutionInstallResponse.setDetails(detailsBuilder.toString());
            return solutionInstallResponse;
        } else {
            return null;
        }
    }

    private SolutionInstallResponse doInstallSolution(User user, TenantId tenantId, String solutionId, HttpServletRequest request) {
        SolutionInstallContext ctx = new SolutionInstallContext(tenantId, solutionId, user, new TenantSolutionTemplateInstructions());
        try {
            provisionRoles(ctx);

            provisionTenantDetails(ctx);

            provisionRuleChains(ctx);

            provisionDeviceProfiles(ctx);

            provisionAssetProfiles(ctx);

            List<CustomerDefinition> customers = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "customers.json", new TypeReference<>() {
            });

            provisionCustomers(ctx, customers);

            var assets = provisionAssets(ctx);

            var devices = provisionDevices(user, ctx);

            provisionDashboards(ctx);

            provisionCustomerUsers(ctx, customers);

            provisionRelations(ctx);

            provisionSchedulerEvents(ctx);

            updateRuleChains(ctx);

            launchEmulators(ctx, devices, assets);

            ctx.getSolutionInstructions().setDetails(prepareInstructions(ctx, request));

            long ts = System.currentTimeMillis();
            AttributeKvEntry createdEntitiesAttribute = new BaseAttributeKvEntry(new StringDataEntry(toCreatedEntitiesKey(solutionId), JacksonUtil.toString(ctx.getCreatedEntitiesList())), ts);
            AttributeKvEntry statusAttribute = new BaseAttributeKvEntry(new BooleanDataEntry(toStatusKey(solutionId), true), ts);

            TenantSolutionTemplateInstructions instructions = new TenantSolutionTemplateInstructions(ctx.getSolutionInstructions());
            AttributeKvEntry instructionAttribute = new BaseAttributeKvEntry(new StringDataEntry(toInstructionsKey(solutionId), JacksonUtil.toString(instructions)), ts);
            attributesService.save(tenantId, tenantId, DataConstants.SERVER_SCOPE, Arrays.asList(createdEntitiesAttribute, statusAttribute, instructionAttribute));

            List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "rule_chains.json", new TypeReference<>() {
            });
            if (ruleChains.stream().anyMatch(r -> StringUtils.isNotEmpty(r.getUpdate()))) {
                long timeout = solutions.stream().filter(s -> s.getId().equals(solutionId))
                        .map(SolutionTemplate::getInstallTimeoutMs).findFirst().orElseThrow(RuntimeException::new);
                Thread.sleep(timeout);
                finalUpdateRuleChains(ctx);
            }

            return new SolutionInstallResponse(ctx.getSolutionInstructions(), true);
        } catch (Throwable e) {
            log.error("[{}][{}] Failed to provision", tenantId, solutionId, e);
            rollback(tenantId, solutionId, ctx, e);
            return new SolutionInstallResponse(ctx.getSolutionInstructions(), false);
        }
    }

    private void rollback(TenantId tenantId, String solutionId, SolutionInstallContext ctx, Throwable e) {
        List<EntityId> createdEntities = ctx.getCreatedEntitiesList();
        Collections.reverse(createdEntities);
        for (EntityId entityId : createdEntities) {
            try {
                deleteEntity(tenantId, entityId, ctx.getUser());
            } catch (RuntimeException re) {
                log.error("[{}][{}] Failed to delete the entity: {}", tenantId, solutionId, entityId, re);
            }
        }
        attributesService.removeAll(tenantId, tenantId, DataConstants.SERVER_SCOPE, Arrays.asList(toCreatedEntitiesKey(solutionId), toStatusKey(solutionId), toInstructionsKey(solutionId)));
        ctx.getSolutionInstructions().setDetails(e.getMessage());
    }

    private String prepareInstructions(SolutionInstallContext ctx, HttpServletRequest request) {
        String template = readFile(resolve(ctx.getSolutionId(), "instructions.md"));
        template = template.replace("${BASE_URL}", systemSecurityService.getBaseUrl(ctx.getTenantId(), null, request));
        TenantSolutionTemplateInstructions solutionInstructions = ctx.getSolutionInstructions();
        template = template.replace("${MAIN_DASHBOARD_URL}",
                getDashboardLink(solutionInstructions, solutionInstructions.getDashboardGroupId(), solutionInstructions.getDashboardId(), false));
        if (solutionInstructions.isMainDashboardPublic()) {
            template = template.replace("${MAIN_DASHBOARD_PUBLIC_URL}",
                    getDashboardLink(solutionInstructions, solutionInstructions.getDashboardGroupId(), solutionInstructions.getDashboardId(), true));
        }

        for (DashboardLinkInfo dashboardLinkInfo : ctx.getDashboardLinks()) {
            template = template.replace("${" + dashboardLinkInfo.getName() + "DASHBOARD_URL}",
                    getDashboardLink(solutionInstructions, dashboardLinkInfo.getEntityGroupId(), dashboardLinkInfo.getDashboardId(), false));
            if (dashboardLinkInfo.isPublic()) {
                template = template.replace("${" + dashboardLinkInfo.getName() + "DASHBOARD_PUBLIC_URL}",
                        getDashboardLink(solutionInstructions, dashboardLinkInfo.getEntityGroupId(), dashboardLinkInfo.getDashboardId(), true));
            }
        }

        StringBuilder devList = new StringBuilder();

        devList.append("| Device name | Access token | Customer name |");
        devList.append(System.lineSeparator());
        devList.append("| :---   | :---  | :---  |");
        devList.append(System.lineSeparator());

        for (DeviceCredentialsInfo credentialsInfo : ctx.getCreatedDevices().values()) {
            devList.append("|").append(credentialsInfo.getName())
                    .append("|").append(credentialsInfo.getCredentials().getCredentialsId()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getCustomerName() != null ? credentialsInfo.getCustomerName() : "");
            devList.append(System.lineSeparator());

            template = template.replace("${" + credentialsInfo.getName() + "ACCESS_TOKEN}", credentialsInfo.getCredentials().getCredentialsId());
        }

        template = template.replace("${device_list_and_credentials}", devList.toString());

        StringBuilder userList = new StringBuilder();

        userList.append("| Name | Login | Password | Customer name | User Group |");
        userList.append(System.lineSeparator());
        userList.append("| :---  | :---  | :---  | :---  | :---  |");
        userList.append(System.lineSeparator());

        for (UserCredentialsInfo credentialsInfo : ctx.getCreatedUsers().values()) {
            userList.append("|").append(credentialsInfo.getName())
                    .append("|").append(credentialsInfo.getLogin()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getPassword()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getCustomerName() != null ? credentialsInfo.getCustomerName() : "")
                    .append("|").append(credentialsInfo.getCustomerGroup() != null ? credentialsInfo.getCustomerGroup() : "");
            userList.append(System.lineSeparator());
        }

        template = template.replace("${user_list}", userList.toString());

        StringBuilder entityList = new StringBuilder();

        entityList.append("| Name | Type | Owner |");
        entityList.append(System.lineSeparator());
        entityList.append("| :---  | :---  | :---  |");
        entityList.append(System.lineSeparator());

        for (CreatedEntityInfo entityInfo : ctx.getCreatedEntities().values()) {
            entityList.append("|").append(entityInfo.getName())
                    .append("|").append(entityInfo.getType()).append("|")
                    .append(entityInfo.getOwner());
            entityList.append(System.lineSeparator());
        }

        template = template.replace("${all_entities}", entityList.toString());

        return template;
    }

    private String getDashboardLink(TenantSolutionTemplateInstructions solutionInstructions, EntityGroupId dashboardGroupId, DashboardId dashboardId, boolean isPublic) {
        String dashboardLink;
        if (isPublic) {
            dashboardLink = "/dashboard/" + dashboardId.getId() + "?publicId=" + solutionInstructions.getPublicId();
        } else {
            dashboardLink = "/dashboardGroups/" + dashboardGroupId.getId() + "/" + dashboardId.getId();
        }
        return dashboardLink;
    }

    private void provisionRoles(SolutionInstallContext ctx) {
        List<RoleDefinition> roleDefinitions = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "roles.json", new TypeReference<>() {
        });
        for (RoleDefinition roleDef : roleDefinitions) {
            Role role = new Role();
            role.setTenantId(ctx.getTenantId());
            role.setName(roleDef.getName());
            role.setType(roleDef.getType());
            role.setPermissions(roleDef.getOperations());
            role = roleService.saveRole(ctx.getTenantId(), role);
            ctx.register(role);
            ctx.putIdToMap(roleDef, role.getId());
        }
    }

    private void provisionRuleChains(SolutionInstallContext ctx) {
        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "rule_chains.json", new TypeReference<>() {
        });
        for (ReferenceableEntityDefinition entityDefinition : ruleChains) {
            // Rule chains should be ordered correctly to exclude dependencies.
            Path ruleChainPath = resolve(ctx.getSolutionId(), "rule_chains", entityDefinition.getFile());
            JsonNode ruleChainJson = replaceIds(ctx, JacksonUtil.toJsonNode(ruleChainPath));
            RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            ruleChain.setTenantId(ctx.getTenantId());
            String metadataStr = JacksonUtil.toString(ruleChainJson.get("metadata"));
            RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(JacksonUtil.toJsonNode(metadataStr), RuleChainMetaData.class);
            RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
            ruleChainMetaData.setRuleChainId(savedRuleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), ruleChainMetaData);
            if (ruleChain.isRoot()) {
                ruleChainService.setRootRuleChain(ctx.getTenantId(), savedRuleChain.getId());
            }
            ctx.register(entityDefinition.getJsonId(), savedRuleChain);
            tbClusterService.broadcastEntityStateChangeEvent(ruleChain.getTenantId(), savedRuleChain.getId(), ComponentLifecycleEvent.CREATED);
        }
    }

    private void updateRuleChains(SolutionInstallContext ctx) {
        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "rule_chains.json", new TypeReference<>() {
        });
        for (ReferenceableEntityDefinition entityDefinition : ruleChains) {
            // Rule chains should be ordered correctly to exclude dependencies.
            Path ruleChainPath = resolve(ctx.getSolutionId(), "rule_chains", entityDefinition.getFile());
            JsonNode ruleChainJson = JacksonUtil.toJsonNode(ruleChainPath);
            RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            ruleChain.setTenantId(ctx.getTenantId());
            String metadataStr = JacksonUtil.toString(ruleChainJson.get("metadata"));
            String oldMetadataStr = metadataStr;
            for (var entry : ctx.getRealIds().entrySet()) {
                metadataStr = metadataStr.replace(entry.getKey(), entry.getValue());
            }
            if (metadataStr.equals(oldMetadataStr)) {
                continue;
            }
            RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(JacksonUtil.toJsonNode(metadataStr), RuleChainMetaData.class);

            RuleChainId ruleChainId = (RuleChainId) EntityIdFactory.getByTypeAndUuid(EntityType.RULE_CHAIN, ctx.getRealIds().get(entityDefinition.getJsonId()));
            RuleChain savedRuleChain = ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChainId);
            ruleChainMetaData.setRuleChainId(savedRuleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), ruleChainMetaData);
            tbClusterService.broadcastEntityStateChangeEvent(ruleChain.getTenantId(), savedRuleChain.getId(), ComponentLifecycleEvent.UPDATED);
        }
    }

    private void finalUpdateRuleChains(SolutionInstallContext ctx) {
        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "rule_chains.json", new TypeReference<>() {
        });
        for (ReferenceableEntityDefinition entityDefinition : ruleChains) {
            if (StringUtils.isEmpty(entityDefinition.getUpdate())) {
                continue;
            }
            // Rule chains should be ordered correctly to exclude dependencies.
            Path ruleChainPath = resolve(ctx.getSolutionId(), "rule_chains", entityDefinition.getUpdate());
            JsonNode ruleChainJson = JacksonUtil.toJsonNode(ruleChainPath);
            RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            ruleChain.setTenantId(ctx.getTenantId());
            String metadataStr = JacksonUtil.toString(ruleChainJson.get("metadata"));
            String oldMetadataStr = metadataStr;
            for (var entry : ctx.getRealIds().entrySet()) {
                metadataStr = metadataStr.replace(entry.getKey(), entry.getValue());
            }
            RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(JacksonUtil.toJsonNode(metadataStr), RuleChainMetaData.class);

            RuleChainId ruleChainId = (RuleChainId) EntityIdFactory.getByTypeAndUuid(EntityType.RULE_CHAIN, ctx.getRealIds().get(entityDefinition.getJsonId()));
            RuleChain savedRuleChain = ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChainId);
            ruleChainMetaData.setRuleChainId(savedRuleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), ruleChainMetaData);
            tbClusterService.broadcastEntityStateChangeEvent(ruleChain.getTenantId(), savedRuleChain.getId(), ComponentLifecycleEvent.UPDATED);
        }
    }

    private void provisionDeviceProfiles(SolutionInstallContext ctx) {
        List<DeviceProfile> deviceProfiles = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "device_profiles.json", new TypeReference<>() {
        });
        deviceProfiles.addAll(loadListOfEntitiesFromDirectory(ctx.getSolutionId(), "device_profiles", DeviceProfile.class));
        deviceProfiles.forEach(deviceProfile -> {
            deviceProfile.setId(null);
            deviceProfile.setCreatedTime(0L);
            deviceProfile.setTenantId(ctx.getTenantId());
            if (deviceProfile.getDefaultRuleChainId() != null) {
                String newId = ctx.getRealIds().get(deviceProfile.getDefaultRuleChainId().getId().toString());
                if (newId != null) {
                    deviceProfile.setDefaultRuleChainId(new RuleChainId(UUID.fromString(newId)));
                } else {
                    log.error("[{}][{}] Device profile: {} references non existing rule chain.", ctx.getTenantId(), ctx.getSolutionId(), deviceProfile.getName());
                    throw new ThingsboardRuntimeException();
                }
            }
        });

        deviceProfiles = deviceProfiles.stream().map(deviceProfileService::saveDeviceProfile).collect(Collectors.toList());
        deviceProfiles.forEach(ctx::register);
    }

    private void provisionAssetProfiles(SolutionInstallContext ctx) {
        List<AssetProfile> assetProfiles = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "asset_profiles.json", new TypeReference<>() {
        });
        assetProfiles.addAll(loadListOfEntitiesFromDirectory(ctx.getSolutionId(), "asset_profiles", AssetProfile.class));
        assetProfiles.forEach(assetProfile -> {
            assetProfile.setId(null);
            assetProfile.setCreatedTime(0L);
            assetProfile.setTenantId(ctx.getTenantId());
            if (assetProfile.getDefaultRuleChainId() != null) {
                String newId = ctx.getRealIds().get(assetProfile.getDefaultRuleChainId().getId().toString());
                if (newId != null) {
                    assetProfile.setDefaultRuleChainId(new RuleChainId(UUID.fromString(newId)));
                } else {
                    log.error("[{}][{}] Asset profile: {} references non existing rule chain.", ctx.getTenantId(), ctx.getSolutionId(), assetProfile.getName());
                    throw new ThingsboardRuntimeException();
                }
            }
        });

        assetProfiles = assetProfiles.stream().map(assetProfileService::saveAssetProfile).collect(Collectors.toList());
        assetProfiles.forEach(ctx::register);
    }

    private void provisionSchedulerEvents(SolutionInstallContext ctx) {
        List<SchedulerEventDefinition> schedulerEvents = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "scheduler_events.json", new TypeReference<>() {
        });
        schedulerEvents.addAll(loadListOfEntitiesFromDirectory(ctx.getSolutionId(), "scheduler_events", SchedulerEventDefinition.class));
        schedulerEvents.forEach(entityDef -> {
            SchedulerEvent schedulerEvent = new SchedulerEvent();
            schedulerEvent.setTenantId(ctx.getTenantId());
            schedulerEvent.setName(entityDef.getName());
            schedulerEvent.setType(entityDef.getType());
            schedulerEvent.setConfiguration(entityDef.getConfiguration());
            schedulerEvent.setSchedule(entityDef.getSchedule());
            schedulerEvent.setCustomerId(ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer()));
            if (entityDef.getOriginatorId() != null) {
                String newIdStr = ctx.getRealIds().get(entityDef.getOriginatorId().getId().toString());
                if (newIdStr != null) {
                    EntityId newId = EntityIdFactory.getByTypeAndUuid(entityDef.getOriginatorId().getEntityType(), UUID.fromString(newIdStr));
                    schedulerEvent.setOriginatorId(newId);
                } else {
                    log.error("[{}][{}] Scheduler event: {} references non existing entity.", ctx.getTenantId(), ctx.getSolutionId(), entityDef.getName());
                    throw new ThingsboardRuntimeException();
                }
            }
            //TODO: use tbSchedulerService here when it becomes available.
            SchedulerEvent savedSchedulerEvent = schedulerEventService.saveSchedulerEvent(schedulerEvent);

            if (schedulerEvent.getId() == null) {
                schedulerService.onSchedulerEventAdded(savedSchedulerEvent);
            } else {
                schedulerService.onSchedulerEventUpdated(savedSchedulerEvent);
            }
            log.info("[{}] Saved scheduler event: {}", schedulerEvent.getId(), schedulerEvent);
            ctx.register(savedSchedulerEvent.getId());
        });
    }

    private void provisionDashboards(SolutionInstallContext ctx) throws ThingsboardException {
        List<DashboardDefinition> dashboards = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "dashboards.json", new TypeReference<>() {
        });
        for (DashboardDefinition entityDef : dashboards) {
            CustomerId customerId = ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Path dashboardsPath = resolve(ctx.getSolutionId(), "dashboards", entityDef.getFile());
            JsonNode dashboardJson = replaceIds(ctx, JacksonUtil.toJsonNode(dashboardsPath));
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(ctx.getTenantId());
            dashboard.setTitle(entityDef.getName());
            dashboard.setConfiguration(dashboardJson.get("configuration"));
            dashboard.setCustomerId(customerId);
            dashboard = dashboardService.saveDashboard(dashboard);
            ctx.register(entityDef, dashboard);
            ctx.putIdToMap(EntityType.DASHBOARD, entityDef.getName(), dashboard.getId());
            EntityGroupId entityGroupId = addEntityToGroup(ctx, entityDef, dashboard.getId());
            if (entityGroupId == null) {
                entityGroupId = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), dashboard.getOwnerId(), EntityType.DASHBOARD, EntityGroup.GROUP_ALL_NAME).get().getId();
            }
            if (entityDef.isMain()) {
                ctx.getSolutionInstructions().setDashboardGroupId(entityGroupId);
                ctx.getSolutionInstructions().setDashboardId(dashboard.getId());
                ctx.getSolutionInstructions().setMainDashboardPublic(entityDef.isMakePublic());
            }
            ctx.getDashboardLinks().add(new DashboardLinkInfo(dashboard.getName(), entityGroupId, dashboard.getId(), entityDef.isMakePublic()));
        }
    }

    protected void provisionRelations(SolutionInstallContext ctx) {
        ctx.getRelationDefinitions().forEach((id, relations) -> {
            for (RelationDefinition relationDef : relations) {
                log.info("[{}] Saving relation: {}", id, relationDef);
                EntityRelation entityRelation = new EntityRelation();
                EntityId otherId = ctx.getIdFromMap(relationDef.getEntityType(), relationDef.getEntityName());
                if (EntitySearchDirection.FROM.equals(relationDef.getDirection())) {
                    entityRelation.setFrom(otherId);
                    entityRelation.setTo(id);
                } else {
                    entityRelation.setFrom(id);
                    entityRelation.setTo(otherId);
                }
                entityRelation.setTypeGroup(RelationTypeGroup.COMMON);
                entityRelation.setType(relationDef.getType());
                try {
                    relationService.save(ctx.getTenantId(), null, entityRelation, null);
                } catch (Exception e) {
                    log.info("[{}] Failed to save relation: {}, cause: {}", id, relationDef, e.getMessage());
                }
            }
        });
    }

    protected Map<Device, DeviceDefinition> provisionDevices(User user, SolutionInstallContext ctx) throws Exception {
        Map<Device, DeviceDefinition> result = new HashMap<>();
        Set<String> deviceTypeSet = new HashSet<>();
        List<DeviceDefinition> devices = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "devices.json", new TypeReference<>() {
        });

        for (DeviceDefinition entityDef : devices) {
            CustomerId customerId = ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Device entity = new Device();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            ensureDeviceProfileExists(ctx, deviceTypeSet, entityDef);
            entity.setType(entityDef.getType());
            entity.setCustomerId(customerId);
            entity = deviceService.saveDevice(entity);

            entityActionService.logEntityAction(user, entity.getId(), entity, customerId, ActionType.ADDED, null);

            ctx.register(entityDef, entity);
            log.info("[{}] Saved device: {}", entity.getId(), entity);
            DeviceId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx.getTenantId(), entityId, entityDef.getAttributes());
            ctx.put(entityId, entityDef.getRelations());
            addEntityToGroup(ctx, entityDef, entityId);

            DeviceCredentialsInfo deviceCredentialsInfo = new DeviceCredentialsInfo();
            deviceCredentialsInfo.setName(entity.getName());
            deviceCredentialsInfo.setType(entity.getType());
            deviceCredentialsInfo.setCustomerName(entityDef.getCustomer());
            deviceCredentialsInfo.setCredentials(deviceCredentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), entityId));

            ctx.addDeviceCredentials(deviceCredentialsInfo);

            result.put(entity, entityDef);
            tbClusterService.onDeviceUpdated(entity, null);
        }
        return result;
    }

    private void ensureDeviceProfileExists(SolutionInstallContext ctx, Set<String> deviceTypeSet, DeviceDefinition entityDef) {
        if (!deviceTypeSet.contains(entityDef.getType())){
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileByName(ctx.getTenantId(), entityDef.getType());
            if (deviceProfile == null) {
                DeviceProfile created = deviceProfileService.findOrCreateDeviceProfile(ctx.getTenantId(), entityDef.getType());
                ctx.register(created.getId());
                log.info("Saved device profile: {}", created.getId());
                deviceTypeSet.add(entityDef.getType());
            }
        }
    }

    private void launchEmulators(SolutionInstallContext ctx, Map<Device, DeviceDefinition> devicesMap, Map<Asset, AssetDefinition> assets) throws Exception {
        List<EmulatorDefinition> emulatorDefinitions = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "device_emulators.json", new TypeReference<>() {
        });
        Map<String, EmulatorDefinition> deviceEmulators = emulatorDefinitions.stream().collect(Collectors.toMap(EmulatorDefinition::getName, Function.identity()));
        emulatorDefinitions.stream().filter(ed -> StringUtils.isNotEmpty(ed.getExtendz()))
                .forEach(ed -> {
                    EmulatorDefinition parent = deviceEmulators.get(ed.getExtendz());
                    if (parent != null) {
                        ed.enrich(parent);
                    }
                });


        for (var entry : devicesMap.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue().getEmulator())).collect(Collectors.toSet())) {
            DeviceEmulatorLauncher.builder()
                    .entity(entry.getKey())
                    .emulatorDefinition(deviceEmulators.get(entry.getValue().getEmulator()))
                    .oldTelemetryExecutor(emulatorExecutor)
                    .tbClusterService(tbClusterService)
                    .partitionService(partitionService)
                    .tbQueueProducerProvider(tbQueueProducerProvider)
                    .serviceInfoProvider(serviceInfoProvider)
                    .tsSubService(tsSubService)
                    .build().launch();
        }

        Map<String, EmulatorDefinition> assetEmulators = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "asset_emulators.json", new TypeReference<List<EmulatorDefinition>>() {
        }).stream().collect(Collectors.toMap(EmulatorDefinition::getName, Function.identity()));

        for (var entry : assets.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue().getEmulator())).collect(Collectors.toSet())) {
            AssetEmulatorLauncher.builder()
                    .entity(entry.getKey())
                    .emulatorDefinition(assetEmulators.get(entry.getValue().getEmulator()))
                    .oldTelemetryExecutor(emulatorExecutor)
                    .tbClusterService(tbClusterService)
                    .partitionService(partitionService)
                    .tbQueueProducerProvider(tbQueueProducerProvider)
                    .serviceInfoProvider(serviceInfoProvider)
                    .tsSubService(tsSubService)
                    .build().launch();
        }
    }

    protected void provisionTenantDetails(SolutionInstallContext ctx) throws Exception {
        TenantDefinition tenant = loadEntityIfFileExists(ctx.getSolutionId(), "tenant.json", TenantDefinition.class);
        if (tenant != null) {
            saveServerSideAttributes(ctx.getTenantId(), ctx.getTenantId(), tenant.getAttributes());
            ctx.put(ctx.getTenantId(), tenant.getRelations());

            for (UserGroupDefinition ugDef : tenant.getUserGroups()) {
                EntityGroup ugEntity = getUserGroupInfo(ctx, ctx.getTenantId(), ugDef.getName());
                ctx.registerReferenceOnly(ugDef.getJsonId(), ugEntity.getId());

                for (String genericRoleName : ugDef.getGenericRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, genericRoleName);
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    log.info("[{}] Saving group permission: {}", ctx.getTenantId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);

                }
                for (GroupRoleDefinition grDef : ugDef.getGroupRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, grDef.getRoleName());
                    EntityGroupId entityGroupId = ctx.getGroupIdFromMap(grDef.getGroupType(), grDef.getGroupName());
                    if (entityGroupId == null) {
                        throw new RuntimeException("Invalid solution configuration. EntityGroup does not exist:" + grDef.getGroupType() + grDef.getGroupName());
                    }
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    gp.setEntityGroupId(entityGroupId);
                    gp.setEntityGroupType(grDef.getGroupType());
                    log.info("[{}] Saving group permission: {}", ctx.getTenantId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);
                }
            }
        }
    }

    protected Map<Asset, AssetDefinition> provisionAssets(SolutionInstallContext ctx) throws ThingsboardException {
        Map<Asset, AssetDefinition> result = new HashMap<>();
        Set<String> assetTypeSet = new HashSet<>();
        List<AssetDefinition> assets = loadListOfEntitiesIfFileExists(ctx.getSolutionId(), "assets.json", new TypeReference<>() {
        });
        for (AssetDefinition entityDef : assets) {
            Asset entity = new Asset();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            entity.setType(entityDef.getType());
            entity.setCustomerId(ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer()));
            ensureAssetProfileExists(ctx, assetTypeSet, entityDef);
            entity = assetService.saveAsset(entity);
            ctx.register(entityDef, entity);
            log.info("[{}] Saved asset: {}", entity.getId(), entity);
            AssetId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx.getTenantId(), entityId, entityDef.getAttributes());
            ctx.put(entityId, entityDef.getRelations());
            addEntityToGroup(ctx, entityDef, entityId);
            result.put(entity, entityDef);
        }
        return result;
    }

    private void ensureAssetProfileExists(SolutionInstallContext ctx, Set<String> assetTypeSet, AssetDefinition entityDef) {
        if (!assetTypeSet.contains(entityDef.getType())){
            AssetProfile assetProfile = assetProfileService.findAssetProfileByName(ctx.getTenantId(), entityDef.getType());
            if (assetProfile == null) {
                AssetProfile created = assetProfileService.findOrCreateAssetProfile(ctx.getTenantId(), entityDef.getType());
                ctx.register(created.getId());
                log.info("Saved asset profile: {}", created.getId());
                assetTypeSet.add(entityDef.getType());
            }
        }
    }

    private void provisionCustomers(SolutionInstallContext ctx, List<CustomerDefinition> customers) throws ExecutionException, InterruptedException {
        for (CustomerDefinition entityDef : customers) {
            EntityGroup groupEntity = null;
            if (!StringUtils.isEmpty(entityDef.getGroup())) {
                groupEntity = getCustomerGroupInfo(ctx, ctx.getTenantId(), entityDef.getGroup());
            }
            entityDef.setRandomNameData(generateRandomName(ctx));
            Customer entity = new Customer();
            entity.setTenantId(ctx.getTenantId());
            entity.setTitle(randomize(entityDef.getName(), entityDef.getRandomNameData()));
            entity.setEmail(randomize(entityDef.getEmail(), entityDef.getRandomNameData()));
            entity.setCountry(entityDef.getCountry());
            entity.setCity(entityDef.getCity());
            entity.setState(entityDef.getState());
            entity.setZip(entityDef.getZip());
            entity.setAddress(entityDef.getAddress());
            entity = customerService.saveCustomer(entity);
            log.info("[{}] Saved customer: {}", entity.getId(), entity);
            ctx.register(entityDef, entity);
            CustomerId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx.getTenantId(), entityId, entityDef.getAttributes(), entityDef.getRandomNameData());
            ctx.put(entityId, entityDef.getRelations());

            entityDef.getAssetGroups().forEach(name -> createEntityGroup(ctx, entityId, name, EntityType.ASSET));
            entityDef.getDeviceGroups().forEach(name -> createEntityGroup(ctx, entityId, name, EntityType.DEVICE));
            entityDef.setName(entity.getName());
            if (groupEntity != null) {
                entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), groupEntity.getId(), Collections.singletonList(entity.getId()));
            }
        }
    }

    private void provisionCustomerUsers(SolutionInstallContext ctx, List<CustomerDefinition> customers) throws ExecutionException, InterruptedException {
        for (CustomerDefinition entityDef : customers) {
            Customer entity = customerService.findCustomerByTenantIdAndTitle(ctx.getTenantId(), entityDef.getName()).get();
            for (UserGroupDefinition ugDef : entityDef.getUserGroups()) {
                EntityGroup ugEntity = getUserGroupInfo(ctx, entity.getId(), ugDef.getName());
                ctx.registerReferenceOnly(ugDef.getJsonId(), ugEntity.getId());
                for (String genericRoleName : ugDef.getGenericRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, genericRoleName);
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    log.info("[{}] Saving group permission: {}", entity.getId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);

                }
                for (GroupRoleDefinition grDef : ugDef.getGroupRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, grDef.getRoleName());
                    EntityGroupId entityGroupId = ctx.getGroupIdFromMap(grDef.getGroupType(), grDef.getGroupName());
                    if (entityGroupId == null) {
                        throw new RuntimeException("Invalid solution configuration. EntityGroup does not exist:" + grDef.getGroupType() + grDef.getGroupName());
                    }
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    gp.setEntityGroupId(entityGroupId);
                    gp.setEntityGroupType(grDef.getGroupType());
                    log.info("[{}] Saving group permission: {}", entity.getId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);
                }
            }

            for (UserDefinition uDef : entityDef.getUsers()) {
                String originalName = uDef.getName(); // May not be unique;
                EntityGroup ugEntity = getUserGroupInfo(ctx, entity.getId(), uDef.getGroup());
                User user = createUser(ctx, entity, uDef, entityDef);
                // TODO: get activation token, etc..
                UserCredentials credentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                credentials.setEnabled(true);
                credentials.setActivateToken(null);
                credentials.setPassword(passwordEncoder.encode(uDef.getPassword()));
                userService.saveUserCredentials(ctx.getTenantId(), credentials);
                entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), ugEntity.getId(), Collections.singletonList(user.getId()));
                DashboardUserDetailsDefinition dd = uDef.getDashboard();
                if (dd != null) {
                    DashboardId dashboardId = ctx.getIdFromMap(EntityType.DASHBOARD, dd.getName());
                    ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                    additionalInfo.put("defaultDashboardId", dashboardId.getId().toString());
                    additionalInfo.put("defaultDashboardFullscreen", dd.isFullScreen());
                    user.setAdditionalInfo(additionalInfo);
                    userService.saveUser(user);
                    log.info("[{}] Added default dashboard for user {}", entity.getId(), user.getEmail());
                }
                UserCredentialsInfo credentialsInfo = new UserCredentialsInfo();
                credentialsInfo.setName(user.getFirstName() + " " + user.getLastName());
                credentialsInfo.setLogin(uDef.getName());
                credentialsInfo.setPassword(uDef.getPassword());
                credentialsInfo.setCustomerName(entityDef.getName());
                credentialsInfo.setCustomerGroup(uDef.getGroup());
                ctx.addUserCredentials(credentialsInfo);
                ctx.register(entityDef, uDef, user);
                ctx.put(user.getId(), uDef.getRelations());
                ctx.putIdToMap(EntityType.USER, originalName, user.getId());
                ctx.putIdToMap(EntityType.USER, uDef.getName(), user.getId());
                saveServerSideAttributes(ctx.getTenantId(), user.getId(), uDef.getAttributes());
            }
        }
    }

    private User createUser(SolutionInstallContext ctx, Customer entity, UserDefinition uDef, CustomerDefinition cDef) {
        int maxAttempts = 10;
        int attempts = 0;
        Exception finalE = null;
        while (attempts < maxAttempts) {
            try {
                boolean lastAttempt = maxAttempts == (attempts + 1);
                var randomName = lastAttempt ? RandomNameUtil.nextSuperRandom() : RandomNameUtil.next();
                User user = new User();
                if (!StringUtils.isEmpty(uDef.getFirstname())) {
                    user.setFirstName(randomize(uDef.getFirstname(), randomName, cDef.getRandomNameData()));
                } else {
                    user.setFirstName(randomName.getFirstName());
                }
                if (!StringUtils.isEmpty(uDef.getLastname())) {
                    user.setLastName(randomize(uDef.getLastname(), randomName, cDef.getRandomNameData()));
                } else {
                    user.setLastName(randomName.getLastName());
                }
                user.setAuthority(Authority.CUSTOMER_USER);
                user.setEmail(randomize(uDef.getName(), randomName, cDef.getRandomNameData()));
                user.setCustomerId(entity.getId());
                user.setTenantId(ctx.getTenantId());
                log.info("[{}] Saving user: {}", entity.getId(), user);
                user = userService.saveUser(user);
                uDef.setName(user.getEmail());
                return user;
            } catch (Exception e) {
                finalE = e;
                attempts++;
            }
        }
        throw new RuntimeException(finalE);
    }

    private RandomNameData generateRandomName(SolutionInstallContext ctx) {
        int i = 0;
        while (i < 10) {
            var randomName = RandomNameUtil.next();
            var user = userService.findUserByEmail(ctx.getTenantId(), randomName.getEmail());
            if (user == null) {
                return randomName;
            } else {
                i++;
            }
        }
        String firstName = StringUtils.randomAlphanumeric(5);
        String lastName = StringUtils.randomAlphanumeric(5);
        return new RandomNameData(firstName, lastName, firstName + "." + lastName + "@thingsboard.io");
    }

    private String randomize(String src, RandomNameData name) {
        return randomize(src, name, null);
    }

    private String randomize(String src, RandomNameData name, RandomNameData customer) {
        if (src == null) {
            return null;
        } else {
            String result = src
                    .replace("$randomFirstName", name.getFirstName())
                    .replace("$randomLastName", name.getLastName())
                    .replace("$randomEmail", name.getEmail());
            if (customer != null) {
                result = result
                        .replace("$customerFirstName", customer.getFirstName())
                        .replace("$customerLastName", customer.getLastName())
                        .replace("$customerEmail", customer.getEmail());
            }
            return result.replace("$random", StringUtils.randomAlphanumeric(10).toLowerCase());
        }
    }

    private EntityGroup getCustomerGroupInfo(SolutionInstallContext ctx, EntityId entityId, String ugName) throws ExecutionException, InterruptedException {
        return getGroupInfo(ctx, entityId, EntityType.CUSTOMER, ugName);
    }

    private EntityGroup getUserGroupInfo(SolutionInstallContext ctx, EntityId entityId, String ugName) throws ExecutionException, InterruptedException {
        return getGroupInfo(ctx, entityId, EntityType.USER, ugName);
    }

    private EntityGroup getGroupInfo(SolutionInstallContext ctx, EntityId entityId, EntityType entityType, String ugName) throws ExecutionException, InterruptedException {
        Optional<EntityGroup> ugEntityOpt = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), entityId, entityType, ugName);
        EntityGroup ugEntity;
        if (ugEntityOpt.isPresent()) {
            ugEntity = ugEntityOpt.get();
        } else {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setName(ugName);
            entityGroup.setType(entityType);
            ugEntity = entityGroupService.saveEntityGroup(ctx.getTenantId(), entityId, entityGroup);
            ctx.register(ugEntity.getId());
        }
        return ugEntity;
    }

    private void saveServerSideAttributes(TenantId tenantId, EntityId entityId, JsonNode attributes) {
        saveServerSideAttributes(tenantId, entityId, attributes, null);
    }

    private void saveServerSideAttributes(TenantId tenantId, EntityId entityId, JsonNode attributes, RandomNameData randomNameData) {
        if (attributes != null && !attributes.isNull() && attributes.size() > 0) {
            log.info("[{}] Saving attributes: {}", entityId, attributes);
            if (randomNameData != null) {
                attributes = JacksonUtil.toJsonNode(randomize(JacksonUtil.toString(attributes), randomNameData, null));
            }
            attributesService.save(tenantId, entityId, DataConstants.SERVER_SCOPE,
                    new ArrayList<>(JsonConverter.convertToAttributes(new JsonParser().parse(JacksonUtil.toString(attributes)))));
        }
    }

    protected EntityGroup createEntityGroup(SolutionInstallContext ctx, EntityId ownerId, String name, EntityType type) {
        EntityGroup eg = new EntityGroup();
        eg.setName(name);
        eg.setType(type);
        eg.setOwnerId(ownerId);
        eg = entityGroupService.saveEntityGroup(ctx.getTenantId(), ownerId, eg);
        ctx.register(eg.getId());
        ctx.putIdToMap(eg.getOwnerId(), type, name, eg.getId());
        log.info("[{}] Created entityGroup {}", ownerId, eg);
        return eg;
    }

    private EntityGroupId addEntityToGroup(SolutionInstallContext ctx, CustomerEntityDefinition entityDef, EntityId entityId) throws ThingsboardException {
        CustomerId customerId = ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
        if (!StringUtils.isEmpty(entityDef.getGroup())) {
            EntityId ownerId = customerId == null ? ctx.getTenantId() : customerId;
            EntityGroupId egId = ctx.getGroupIdFromMap(ownerId, entityId.getEntityType(), entityDef.getGroup());
            if (egId == null) {
                if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                    log.info("Creating tenant {} group: {}", entityId.getEntityType(), entityDef.getGroup());
                    egId = createEntityGroup(ctx, ctx.getTenantId(), entityDef.getGroup(), entityId.getEntityType()).getId();
                } else {
                    log.info("[{}] Creating customer {} group: {}", entityDef.getCustomer(), entityId.getEntityType(), entityDef.getGroup());
                    egId = createEntityGroup(ctx, customerId, entityDef.getGroup(), entityId.getEntityType()).getId();
                }
            }
            entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), egId, Collections.singletonList(entityId));

            if (entityDef.isMakePublic()) {
                EntityGroup eg = entityGroupService.findEntityGroupById(ctx.getTenantId(), egId);
                TenantSolutionTemplateInstructions solutionInstructions = ctx.getSolutionInstructions();
                if (!eg.isPublic()) {
                    EntityId publicId = tbEntityGroupService.makePublic(ctx.getTenantId(), eg, ctx.getUser());
                    solutionInstructions.setPublicId(new CustomerId(publicId.getId()));
                } else {
                    if (solutionInstructions.getPublicId() == null) {
                        solutionInstructions.setPublicId(new CustomerId(
                                customerService.findOrCreatePublicUserGroup(ctx.getTenantId(), ctx.getUser().getOwnerId()).getOwnerId().getId()));
                    }
                }
            }

            return egId;
        } else {
            if (entityDef.isMakePublic()) {
                throw new IllegalArgumentException("Entity is assigned to group 'All' only. Can't make entity public!");
            } else {
                return null;
            }
        }
    }

    private String getTypeLabel(EntityType type) {
        return type.name().toLowerCase().replace('_', ' ');
    }

    private <T> T loadEntityIfFileExists(String solutionId, String fileName, Class<T> clazz) {
        Path filePath = resolve(solutionId, "entities", fileName);
        if (Files.exists(filePath)) {
            return JacksonUtil.readValue(filePath.toFile(), clazz);
        } else {
            return null;
        }
    }

    private <T> List<T> loadListOfEntitiesIfFileExists(String solutionId, String fileName, TypeReference<List<T>> typeReference) {
        Path filePath = resolve(solutionId, "entities", fileName);
        if (Files.exists(filePath)) {
            return JacksonUtil.readValue(filePath.toFile(), typeReference);
        } else {
            return new ArrayList<>();
        }
    }

    private <T> List<T> loadListOfEntitiesFromDirectory(String solutionId, String dirName, Class<T> clazz) {
        Path dirPath = resolve(solutionId, dirName);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            List<T> result = new ArrayList<>();
            try {
                for (Path filePath : Files.list(dirPath).collect(Collectors.toList())) {
                    result.add(JacksonUtil.readValue(filePath.toFile(), clazz));
                }
            } catch (IOException e) {
                log.warn("[{}] Failed to read directory: {}", solutionId, dirName, e);
                throw new RuntimeException(e);
            }
            return result;
        } else {
            return new ArrayList<>();
        }
    }

    private void deleteEntity(TenantId tenantId, EntityId entityId, User user) {
        try {
            List<AlarmId> alarmIds = alarmService.findAlarms(tenantId, new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE), null, null, null, false))
                    .get().getData().stream().map(AlarmInfo::getId).collect(Collectors.toList());
            alarmIds.forEach(alarmId -> {
                alarmService.deleteAlarm(tenantId, alarmId);
            });
        } catch (Exception e) {
            log.error("[{}] Failed to delete alarms for entity", entityId.getId(), e);
        }
        switch (entityId.getEntityType()) {
            case RULE_CHAIN:
                var ruleChainid = new RuleChainId(entityId.getId());
                ruleChainService.deleteRuleChainById(tenantId, ruleChainid);
                tbClusterService.broadcastEntityStateChangeEvent(tenantId, ruleChainid, ComponentLifecycleEvent.DELETED);
                break;
            case DEVICE_PROFILE:
                deviceProfileService.deleteDeviceProfile(tenantId, new DeviceProfileId(entityId.getId()));
                break;
            case ASSET_PROFILE:
                assetProfileService.deleteAssetProfile(tenantId, new AssetProfileId(entityId.getId()));
                break;
            case DASHBOARD:
                dashboardService.deleteDashboard(tenantId, new DashboardId(entityId.getId()));
                break;
            case ROLE:
                roleService.deleteRole(tenantId, new RoleId(entityId.getId()));
                break;
            case USER:
                userService.deleteUser(tenantId, new UserId(entityId.getId()));
                break;
            case ASSET:
                tbAssetService.delete(new AssetId(entityId.getId()), user);
                break;
            case DEVICE:
                tbDeviceService.delete(new DeviceId(entityId.getId()), user);
                break;
            case CUSTOMER:
                customerService.deleteCustomer(tenantId, new CustomerId(entityId.getId()));
                break;
            case ENTITY_GROUP:
                entityGroupService.deleteEntityGroup(tenantId, new EntityGroupId(entityId.getId()));
                break;
            case SCHEDULER_EVENT:
                schedulerEventService.deleteSchedulerEvent(tenantId, new SchedulerEventId(entityId.getId()));
                break;
        }
    }

    private JsonNode replaceIds(SolutionInstallContext ctx, JsonNode dashboardJson) {
        String jsonStr = JacksonUtil.toString(dashboardJson);
        for (var e : ctx.getRealIds().entrySet()) {
            jsonStr = jsonStr.replace(e.getKey(), e.getValue());
        }
        return JacksonUtil.toJsonNode(jsonStr);
    }

    private String toCreatedEntitiesKey(String solutionId) {
        return solutionId + "_" + "entities";
    }

    private String toStatusKey(String solutionId) {
        return solutionId + "_" + "status";
    }

    private String toInstructionsKey(String solutionId) {
        return solutionId + "_" + "instructions";
    }

}
