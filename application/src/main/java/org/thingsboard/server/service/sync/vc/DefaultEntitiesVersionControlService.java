/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.sync.vc;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.*;
import org.thingsboard.server.common.data.sync.vc.request.create.*;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.SingleEntityVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.EntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.importing.MissingEntityException;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.sync.vc.data.*;
import org.thingsboard.server.service.sync.vc.repository.TbRepositorySettingsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Futures.transform;
import static com.google.common.util.concurrent.Futures.transformAsync;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {

    private final TbRepositorySettingsService repositorySettingsService;
    private final TbAutoCommitSettingsService autoCommitSettingsService;
    private final GitVersionControlQueueService gitServiceQueue;
    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final TbNotificationEntityService entityNotificationService;
    private final TransactionTemplate transactionTemplate;
    private final CustomerService customerService;
    private final OwnerService ownersService;
    private final EntityGroupService groupService;

    private ListeningExecutorService executor;

    private static final Set<EntityType> GROUP_ENTITIES = new HashSet<>(Arrays.asList(EntityType.CUSTOMER, EntityType.DEVICE, EntityType.ASSET, EntityType.DASHBOARD));

    @Value("${vc.thread_pool_size:4}")
    private int threadPoolSize;

    @PostConstruct
    public void init() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(threadPoolSize, DefaultEntitiesVersionControlService.class));
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ListenableFuture<VersionCreationResult> saveEntitiesVersion(SecurityUser user, VersionCreateRequest request) throws Exception {
        var pendingCommit = gitServiceQueue.prepareCommit(user, request);

        return transformAsync(pendingCommit, commit -> {
            List<ListenableFuture<Void>> gitFutures = new ArrayList<>();
            switch (request.getType()) {
                case SINGLE_ENTITY: {
                    handleSingleEntityRequest(user, commit, gitFutures, (SingleEntityVersionCreateRequest) request);
                    break;
                }
                case COMPLEX: {
                    handleComplexRequest(user, commit, gitFutures, (ComplexVersionCreateRequest) request);
                    break;
                }
            }
            return transformAsync(Futures.allAsList(gitFutures), success -> gitServiceQueue.push(commit), executor);
        }, executor);
    }

    private void handleSingleEntityRequest(SecurityUser user, CommitGitRequest commit, List<ListenableFuture<Void>> gitFutures, SingleEntityVersionCreateRequest vcr) throws Exception {
        EntityId entityId = vcr.getEntityId();
        var config = vcr.getConfig();
        EntityExportSettings exportSettings = EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .exportGroupEntities(config.isSaveGroupEntities())
                .exportPermissions(config.isSavePermissions())
                .build();
        GroupExportCtx ctx = new GroupExportCtx(user, commit, gitFutures, exportSettings, true, true);
        if (EntityType.ENTITY_GROUP.equals(entityId.getEntityType())) {
            exportGroup(ctx, new EntityGroupId(entityId.getId()));
        } else {
            EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(user, entityId, exportSettings);
            ExportableEntity<EntityId> entity = entityData.getEntity();
            if (entity instanceof HasOwnerId) {
                List<CustomerId> hierarchy = addCustomerHierarchyToCommit(ctx, entityId, (HasOwnerId) entity);
                gitFutures.add(gitServiceQueue.addToCommit(commit, hierarchy, entityData));
            } else {
                gitFutures.add(gitServiceQueue.addToCommit(commit, entityData));
            }
        }
    }

    private void handleComplexRequest(SecurityUser user, CommitGitRequest commit, List<ListenableFuture<Void>> gitFutures, ComplexVersionCreateRequest versionCreateRequest) throws Exception {
        versionCreateRequest.getEntityTypes().forEach((entityType, config) -> {
            EntityTypeExportCtx ctx = new EntityTypeExportCtx(user, commit, gitFutures, versionCreateRequest.getEntityTypes().get(entityType), versionCreateRequest.getSyncStrategy(), entityType);
            if (ctx.isOverwrite()) {
                ctx.add(gitServiceQueue.deleteAll(commit, entityType));
            }

            if (GROUP_ENTITIES.contains(entityType)) {
                if (config.isAllEntities()) {
                    ctx.addTask(new EntityTypeExportTask(Collections.emptyList(), ctx.getUser().getTenantId()));
                    while (true) {
                        EntityTypeExportTask task = ctx.pollTask();
                        if (task == null) {
                            break;
                        }
                        exportGroupEntities(ctx, task);
                    }
                } else {
                    GroupExportCtx groupCtx = new GroupExportCtx(user, commit, ctx.getFutures(), ctx.getSettings(), false, false);
                    for (UUID groupId : config.getEntityIds()) {
                        exportGroup(groupCtx, new EntityGroupId(groupId));
                    }
                }
            } else {
                if (config.isAllEntities()) {
                    //For Entity Types that belong to Tenant Level Only.
                    DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntitiesByTenantId(user.getTenantId(), entityType, pageLink)
                            , 100, entity -> {
                                ctx.add(saveEntityData(user, commit, entity.getId(), config));
                            });
                } else {
                    for (UUID entityId : config.getEntityIds()) {
                        ctx.add(saveEntityData(user, commit, EntityIdFactory.getByTypeAndUuid(entityType, entityId), config));
                    }
                }
            }
        });
    }

    @SneakyThrows
    private void exportGroup(GroupExportCtx ctx, EntityGroupId entityId) {
        EntityExportData<ExportableEntity<EntityGroupId>> entityData = exportImportService.exportEntity(ctx.getUser(), entityId, ctx.getSettings());
        EntityGroup group = (EntityGroup) (entityData.getEntity());
        List<CustomerId> hierarchy = addCustomerHierarchyToCommit(ctx, entityId, group);
        ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, entityData));
        if (ctx.getSettings().isExportGroupEntities()) {
            PageDataIterable<EntityId> entityIdsIterator = new PageDataIterable<>(
                    link -> groupService.findEntityIds(ctx.getTenantId(), group.getType(), group.getId(), link), 1024);
            List<EntityId> groupEntityIds = new ArrayList<>();
            for (EntityId groupEntityId : entityIdsIterator) {
                if (!group.isGroupAll()) {
                    groupEntityIds.add(groupEntityId);
                }
                if (ctx.isExportRelatedEntities()) {
                    EntityExportData<ExportableEntity<EntityId>> groupEntityData = exportImportService.exportEntity(ctx.getUser(), groupEntityId, ctx.getSettings());
                    ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, groupEntityData));
                }
            }
            if (!group.isGroupAll()) {
                ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, group.getType(), entityData.getExternalId(), groupEntityIds));
            }
        }
    }

    private List<CustomerId> addCustomerHierarchyToCommit(GroupExportCtx ctx, EntityId entityId, HasOwnerId entity) throws ThingsboardException {
        List<CustomerId> customerIds = getCustomerExternalIds(ctx.getTenantId(), entityId, entity);
        List<CustomerId> hierarchy = new ArrayList<>(customerIds.size());
        for (CustomerId ownerId : customerIds) {
            if (ctx.isExportRelatedCustomers()) {
                EntityExportData<ExportableEntity<EntityId>> ownerData = exportImportService.exportEntity(ctx.getUser(), ownerId, ctx.getSettings());
                ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), new ArrayList<>(hierarchy), ownerData));
            }
            hierarchy.add(ownerId);
        }
        return hierarchy;
    }

    @SneakyThrows
    private void exportGroupEntities(EntityTypeExportCtx ctx, EntityTypeExportTask task) {
        List<CustomerId> parents = new ArrayList<>(task.getParents());
        CustomerId customerId = EntityType.CUSTOMER.equals(task.getOwnerId().getEntityType()) ? new CustomerId(task.getOwnerId().getId()) : null;
        if (customerId != null) {
            parents.add(customerId);
        }
        for (EntityGroup group : groupService.findEntityGroupsByType(ctx.getTenantId(), task.getOwnerId(), ctx.getEntityType()).get()) {
            EntityExportData<ExportableEntity<EntityGroupId>> entityData = exportImportService.exportEntity(ctx.getUser(), group.getId(), ctx.getSettings());
            ctx.getFutures().add(gitServiceQueue.addToCommit(ctx.getCommit(), parents, entityData));
            if (!group.isGroupAll()) {
                PageDataIterable<EntityId> entityIdsIterator = new PageDataIterable<>(
                        link -> groupService.findEntityIds(ctx.getTenantId(), group.getType(), group.getId(), link), 1024);
                List<EntityId> groupEntityIds = new ArrayList<>();
                for (EntityId groupEntityId : entityIdsIterator) {
                    if (!group.isGroupAll()) {
                        groupEntityIds.add(groupEntityId);
                    }
                }
                ctx.getFutures().add(gitServiceQueue.addToCommit(ctx.getCommit(), parents, group.getType(), entityData.getExternalId(), groupEntityIds));
            }
        }
        DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntityIdsByTenantIdAndCustomerId(ctx.getTenantId(), customerId, ctx.getEntityType(), pageLink)
                , 1024, entityId -> {
                    try {
                        EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(ctx.getUser(), entityId, ctx.getSettings());
                        ctx.getFutures().add(gitServiceQueue.addToCommit(ctx.getCommit(), parents, entityData));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntityIdsByTenantIdAndCustomerId(ctx.getTenantId(), customerId, EntityType.CUSTOMER, pageLink)
                , 1024, cId -> ctx.addTask(new EntityTypeExportTask(parents, cId)));
    }

    @SneakyThrows
    private ListenableFuture<Void> saveEntityData(SecurityUser user, CommitGitRequest commit, EntityId entityId, VersionCreateConfig config) {
        EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(user, entityId, EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .build());
        return gitServiceQueue.addToCommit(commit, entityData);
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, EntityId internalId, PageLink pageLink) throws Exception {
        if (internalId == null) {
            internalId = externalId;
        }
        List<CustomerId> customerExternalIds = getCustomerExternalIds(tenantId, internalId);
        if (EntityType.ENTITY_GROUP.equals(internalId.getEntityType())) {
            var entity = findExportableEntityInDb(tenantId, internalId);
            return gitServiceQueue.listVersions(tenantId, branch,
                    customerExternalIds, ((EntityGroup) entity).getType(), externalId, pageLink);
        } else {
            return gitServiceQueue.listVersions(tenantId, branch,
                    customerExternalIds, externalId, pageLink);
        }
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, entityType, pageLink);
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, pageLink);
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, EntityType entityType) throws Exception {
        return gitServiceQueue.listEntitiesAtVersion(tenantId, branch, versionId, entityType);
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listAllEntitiesAtVersion(TenantId tenantId, String branch, String versionId) throws Exception {
        return gitServiceQueue.listEntitiesAtVersion(tenantId, branch, versionId);
    }

    @SuppressWarnings({"UnstableApiUsage", "rawtypes"})
    @Override
    public ListenableFuture<VersionLoadResult> loadEntitiesVersion(SecurityUser user, VersionLoadRequest request) throws Exception {
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionLoadRequest versionLoadRequest = (SingleEntityVersionLoadRequest) request;
                return executor.submit(() -> doInTemplate(status -> loadSingleEntity(user, versionLoadRequest)));
            }
            case ENTITY_TYPE: {
                EntityTypeVersionLoadRequest versionLoadRequest = (EntityTypeVersionLoadRequest) request;
                return executor.submit(() -> doInTemplate(status -> loadMultipleEntities(user, versionLoadRequest)));
            }
            default:
                throw new IllegalArgumentException("Unsupported version load request");
        }
    }

    @SneakyThrows
    @SuppressWarnings("rawtypes")
    private VersionLoadResult loadSingleEntity(SecurityUser user, SingleEntityVersionLoadRequest request) {
        EntityId internalId = request.getInternalEntityId();
        List<CustomerId> ownerIds = internalId != null ? getCustomerExternalIds(user.getTenantId(), internalId) : Collections.emptyList();

        VersionLoadConfig config = request.getConfig();
        var settings = EntityImportSettings.builder()
                .updateRelations(config.isLoadRelations())
                .saveAttributes(config.isLoadAttributes())
                .saveCredentials(config.isLoadCredentials())
                .saveUserGroupPermissions(config.isLoadPermissions())
                .findExistingByName(false)
                .build();

        if (EntityType.ENTITY_GROUP.equals(request.getExternalEntityId().getEntityType())) {
            var entity = findExportableEntityInDb(user.getTenantId(), internalId != null ? internalId : request.getExternalEntityId());
            EntityExportData groupData = gitServiceQueue.getEntityGroup(user.getTenantId(), request.getVersionId(), ownerIds, ((EntityGroup) entity).getType(), request.getExternalEntityId()).get();
            EntityImportResult<?> importResult = exportImportService.importEntity(user, groupData, settings, true, true);
            if (config.isLoadGroupEntities()) {
                EntityGroup savedGroup = (EntityGroup) importResult.getSavedEntity();
                EntitiesImportCtx ctx = new EntitiesImportCtx(request.getVersionId());

                if (savedGroup.isGroupAll()) {
                    importEntities(user, ownerIds, savedGroup.getType(), settings, ctx, false);
                } else {
                    importGroupEntities(user, ownerIds, savedGroup.getType(), groupData.getExternalId(), settings, ctx);
                }
                reimport(user, ownerIds, ctx);
                ctx.executeCallbacks();
            }

            return VersionLoadResult.success(EntityTypeLoadResult.builder()
                    .entityType(importResult.getEntityType())
                    .created(importResult.getOldEntity() == null ? 1 : 0)
                    .updated(importResult.getOldEntity() != null ? 1 : 0)
                    .deleted(0)
                    .build());
        } else {
            EntityExportData entityData = gitServiceQueue.getEntity(user.getTenantId(), request.getVersionId(), ownerIds, request.getExternalEntityId()).get();
            return loadSingleEntity(user, settings, entityData);
        }
    }

    private List<CustomerId> getCustomerExternalIds(TenantId tenantId, EntityId entityId) {
        return getCustomerExternalIds(tenantId, entityId, null);
    }

    private List<CustomerId> getCustomerExternalIds(TenantId tenantId, EntityId entityId, HasOwnerId entity) {
        Set<EntityId> ownersSet;
        if (entity != null) {
            ownersSet = ownersService.getOwners(tenantId, entityId, entity);
        } else {
            ownersSet = ownersService.getOwners(tenantId, entityId);
        }
        List<EntityId> owners = new ArrayList<>(ownersSet);
        if (owners.size() == 1) {
            return Collections.emptyList();
        } else {
            Collections.reverse(owners);
            List<CustomerId> result = new ArrayList<>(Math.max(1, owners.size() - 1));
            for (EntityId ownerId : owners) {
                if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                    continue;
                }
                CustomerId internalId = new CustomerId(ownerId.getId());
                Customer customer = customerService.findCustomerById(tenantId, internalId);
                if (customer == null) {
                    throw new RuntimeException("Failed to fetch customer with id: " + internalId);
                }
                result.add(customer.getExternalId() != null ? customer.getExternalId() : internalId);
            }
            return result;
        }
    }

    private VersionLoadResult doInTemplate(TransactionCallback<VersionLoadResult> result) {
        try {
            return transactionTemplate.execute(result);
        } catch (LoadEntityException e) {
            return onError(e.getExternalId(), e.getCause());
        }
    }

    private VersionLoadResult loadSingleEntity(SecurityUser user, EntityImportSettings settings, EntityExportData entityData) {
        try {
            EntityImportResult<?> importResult = exportImportService.importEntity(user, entityData, settings, true, true);
            return VersionLoadResult.success(EntityTypeLoadResult.builder()
                    .entityType(importResult.getEntityType())
                    .created(importResult.getOldEntity() == null ? 1 : 0)
                    .updated(importResult.getOldEntity() != null ? 1 : 0)
                    .deleted(0)
                    .build());
        } catch (Exception e) {
            throw new LoadEntityException(entityData.getExternalId(), e);
        }
    }

    @SneakyThrows
    private VersionLoadResult loadMultipleEntities(SecurityUser user, EntityTypeVersionLoadRequest request) {
        EntitiesImportCtx ctx = new EntitiesImportCtx(request.getVersionId());

        List<EntityType> entityTypes = request.getEntityTypes().keySet().stream()
                .sorted(exportImportService.getEntityTypeComparatorForImport()).collect(Collectors.toList());
        for (EntityType entityType : entityTypes) {
            var config = request.getEntityTypes().get(entityType);
            var settings = EntityImportSettings.builder()
                    .updateRelations(config.isLoadRelations())
                    .saveAttributes(config.isLoadAttributes())
                    .findExistingByName(config.isFindExistingEntityByName())
                    .build();
            importEntityGroups(user, entityType, settings, ctx, true);
            importEntities(user, Collections.emptyList(), entityType, settings, ctx, true);
        }

        reimport(user, Collections.emptyList(), ctx);

        ctx.getExternalEntityIdGroups().forEach((id, groupIds) -> {
            EntityGroupId groupId = groupIds.stream().findFirst().orElseThrow();
            throw new LoadEntityException(groupId, new MissingEntityException(id));
        });
        request.getEntityTypes().keySet().stream()
                .filter(entityType -> request.getEntityTypes().get(entityType).isRemoveOtherEntities())
                .sorted(exportImportService.getEntityTypeComparatorForImport().reversed())
                .forEach(entityType -> removeOtherEntities(user, entityType, ctx));

        ctx.executeCallbacks();
        return VersionLoadResult.success(new ArrayList<>(ctx.getResults().values()));
    }

    private void importGroupEntities(SecurityUser user, List<CustomerId> ownerIds, EntityType entityType, EntityId externalGroupId,
                                     EntityImportSettings importSettings, EntitiesImportCtx ctx) throws InterruptedException, ExecutionException {

        List<EntityId> allGroupEntityIds = gitServiceQueue.getGroupEntityIds(user.getTenantId(), ctx.getVersionId(), ownerIds, entityType, externalGroupId).get();

        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();

        for (List<UUID> entityIds : Lists.partition(allGroupEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()), 100)) {
            List<EntityExportData> entityDataList = gitServiceQueue.getEntities(user.getTenantId(), ctx.getVersionId(), ownerIds, entityType, entityIds).get();
            importEntityDataList(user, entityType, importSettings, ctx, created, updated, entityDataList);
        }
        ctx.put(entityType, EntityTypeLoadResult.builder().entityType(entityType).created(created.get()).updated(updated.get()).build());
    }

    private void importEntities(SecurityUser user, List<CustomerId> ownerIds, EntityType entityType,
                                EntityImportSettings importSettings, EntitiesImportCtx ctx, boolean recursive) throws InterruptedException, ExecutionException {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();

        int limit = 100;
        int offset = 0;
        List<EntityExportData> entityDataList;
        do {
            entityDataList = gitServiceQueue.getEntities(user.getTenantId(), ctx.getVersionId(), ownerIds, entityType, false, recursive, offset, limit).get();
            importEntityDataList(user, entityType, importSettings, ctx, created, updated, entityDataList);
            offset += limit;
        } while (entityDataList.size() == limit);
        ctx.put(entityType, EntityTypeLoadResult.builder().entityType(entityType).created(created.get()).updated(updated.get()).build());
    }

    private void importEntityGroups(SecurityUser user, EntityType entityType,
                                    EntityImportSettings importSettings, EntitiesImportCtx ctx, boolean recursive) throws InterruptedException, ExecutionException {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();

        int limit = 100;
        int offset = 0;
        List<EntityExportData> entityDataList;
        Map<EntityId, List<CustomerId>> ownersCache = new HashMap<>();
        do {
            entityDataList = gitServiceQueue.getEntities(user.getTenantId(), ctx.getVersionId(), Collections.emptyList(), entityType, true, recursive, offset, limit).get();
            List<EntityImportResult<?>> entityGroupResults = importEntityDataList(user, entityType, importSettings, ctx, created, updated, entityDataList);
            for (EntityImportResult<?> entityImportResult : entityGroupResults) {
                EntityGroup savedGroup = (EntityGroup) entityImportResult.getSavedEntity();
                if (!savedGroup.isGroupAll()) {
                    var ownerIds = ownersCache.get(savedGroup.getOwnerId());
                    if (ownerIds == null) {
                        ownerIds = getCustomerExternalIds(user.getTenantId(), savedGroup.getId(), savedGroup);
                        ownersCache.put(savedGroup.getOwnerId(), ownerIds);
                    }
                    List<EntityId> allGroupEntityIds = gitServiceQueue.getGroupEntityIds(user.getTenantId(), ctx.getVersionId(), ownerIds, entityType, savedGroup.getExternalId()).get();
                    ctx.registerGroupEntities(savedGroup.getId(), allGroupEntityIds);
                }
            }
            offset += limit;
        } while (entityDataList.size() == limit);
        ctx.put(entityType, EntityTypeLoadResult.builder().entityType(entityType).created(created.get()).updated(updated.get()).build());
    }

    private List<EntityImportResult<?>> importEntityDataList(SecurityUser user, EntityType entityType,
                                                             EntityImportSettings importSettings, EntitiesImportCtx ctx,
                                                             AtomicInteger created, AtomicInteger updated, List<EntityExportData> entityDataList) {
        List<EntityImportResult<?>> importResults = new ArrayList<>();
        for (EntityExportData entityData : entityDataList) {
            EntityImportResult<?> importResult;
            try {
                importResult = exportImportService.importEntity(user, entityData,
                        importSettings, false, false);
                importResults.add(importResult);
            } catch (Exception e) {
                throw new LoadEntityException(entityData.getExternalId(), e);
            }
            if (importResult.getUpdatedAllExternalIds() != null && !importResult.getUpdatedAllExternalIds()) {
                ctx.getToReimport().put(entityData.getEntity().getExternalId(), importSettings);
                continue;
            }

            if (importResult.getOldEntity() == null) created.incrementAndGet();
            else updated.incrementAndGet();
            ctx.getSaveReferencesCallbacks().add(importResult.getSaveReferencesCallback());
            ctx.getSendEventsCallbacks().add(importResult.getSendEventsCallback());
            EntityId savedEntityId = importResult.getSavedEntity().getId();
            ctx.getImportedEntities().computeIfAbsent(entityType, t -> new HashSet<>())
                    .add(savedEntityId);
            createGroupRelations(user, ctx, entityData.getExternalId(), savedEntityId);
        }
        return importResults;
    }

    private void createGroupRelations(SecurityUser user, EntitiesImportCtx ctx, EntityId externalId, EntityId savedEntityId) {
        Set<EntityGroupId> groupIds = ctx.unregisterEntityGroups(externalId);
        groupIds.forEach(groupId -> groupService.addEntityToEntityGroup(user.getTenantId(), groupId, savedEntityId));
    }

    private void reimport(SecurityUser user, List<CustomerId> parents, EntitiesImportCtx ctx) {
        ctx.getToReimport().forEach((externalId, importSettings) -> {
            //TODO: refactor to extract duplicated code from importEntityDataList and reimport
            try {
                EntityExportData entityData = gitServiceQueue.getEntity(user.getTenantId(), ctx.getVersionId(), parents, externalId).get();
                importSettings.setResetExternalIdsOfAnotherTenant(true);
                EntityImportResult<?> importResult = exportImportService.importEntity(user, entityData,
                        importSettings, false, false);

                EntityTypeLoadResult stats = ctx.get(externalId.getEntityType());
                if (importResult.getOldEntity() == null) stats.setCreated(stats.getCreated() + 1);
                else stats.setUpdated(stats.getUpdated() + 1);
                ctx.getSaveReferencesCallbacks().add(importResult.getSaveReferencesCallback());
                ctx.getSendEventsCallbacks().add(importResult.getSendEventsCallback());
                EntityId savedEntityId = importResult.getSavedEntity().getId();
                ctx.getImportedEntities().computeIfAbsent(externalId.getEntityType(), t -> new HashSet<>())
                        .add(savedEntityId);
                createGroupRelations(user, ctx, entityData.getExternalId(), savedEntityId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void removeOtherEntities(SecurityUser user, EntityType entityType, EntitiesImportCtx ctx) {
        DaoUtil.processInBatches(pageLink -> {
            return exportableEntitiesService.findEntitiesByTenantId(user.getTenantId(), entityType, pageLink);
        }, 100, entity -> {
            if (ctx.getImportedEntities().get(entityType) == null || !ctx.getImportedEntities().get(entityType).contains(entity.getId())) {
                try {
                    exportableEntitiesService.checkPermission(user, entity, entityType, Operation.DELETE);
                } catch (ThingsboardException e) {
                    throw new RuntimeException(e);
                }
                exportableEntitiesService.removeById(user.getTenantId(), entity.getId());

                ctx.getSendEventsCallbacks().add(() -> {
                    entityNotificationService.notifyDeleteEntity(user.getTenantId(), entity.getId(),
                            entity, null, ActionType.DELETED, null, user);
                });
                EntityTypeLoadResult result = ctx.getResults().get(entityType);
                result.setDeleted(result.getDeleted() + 1);
            }
        });
    }

    private VersionLoadResult onError(EntityId externalId, Throwable e) {
        return analyze(e, externalId).orElseThrow(() -> new RuntimeException(e));
    }

    private Optional<VersionLoadResult> analyze(Throwable e, EntityId externalId) {
        if (e == null) {
            return Optional.empty();
        } else {
            if (e instanceof DeviceCredentialsValidationException) {
                return Optional.of(VersionLoadResult.error(EntityLoadError.credentialsError(externalId)));
            } else if (e instanceof MissingEntityException) {
                return Optional.of(VersionLoadResult.error(EntityLoadError.referenceEntityError(externalId, ((MissingEntityException) e).getEntityId())));
            } else if (e instanceof DataValidationException) {
                var dve = (DataValidationException) e;
                if (dve.getMessage().equals("Integration with such routing key already exists!")) {
                    return Optional.of(VersionLoadResult.error(EntityLoadError.routingKeyError(externalId)));
                }
            }
            return analyze(e.getCause(), externalId);
        }
    }

    @Override
    public ListenableFuture<EntityDataDiff> compareEntityDataToVersion(SecurityUser user, String branch, EntityId entityId, String versionId) throws Exception {
        HasId<? extends EntityId> entity = findExportableEntityInDb(user.getTenantId(), entityId);

        EntityId externalId = ((ExportableEntity<? extends EntityId>) entity).getExternalId();
        if (externalId == null) externalId = entityId;

        var customerIds = getCustomerExternalIds(user.getTenantId(), entityId);
        ListenableFuture<EntityExportData> future;
        if (EntityType.ENTITY_GROUP.equals(entity.getId().getEntityType())) {
            future = gitServiceQueue.getEntityGroup(user.getTenantId(), versionId, customerIds, ((EntityGroup) entity).getType(), externalId);
        } else {
            future = gitServiceQueue.getEntity(user.getTenantId(), versionId, customerIds, externalId);
        }

        return transformAsync(future,
                otherVersion -> {
                    EntityExportData<?> currentVersion = exportImportService.exportEntity(user, entityId, EntityExportSettings.builder()
                            .exportRelations(otherVersion.hasRelations())
                            .exportAttributes(otherVersion.hasAttributes())
                            .exportCredentials(otherVersion.hasCredentials())
                            .exportPermissions(otherVersion.hasPermissions())
                            .exportGroupEntities(otherVersion.hasGroupEntities())
                            .build());
                    return transform(gitServiceQueue.getContentsDiff(user.getTenantId(),
                                    JacksonUtil.toPrettyString(currentVersion.sort()),
                                    JacksonUtil.toPrettyString(otherVersion.sort())),
                            rawDiff -> new EntityDataDiff(currentVersion, otherVersion, rawDiff), MoreExecutors.directExecutor());
                }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<EntityDataInfo> getEntityDataInfo(SecurityUser user, EntityId externalId, EntityId internalId, String versionId) {
        List<CustomerId> customerIds = internalId != null ? getCustomerExternalIds(user.getTenantId(), internalId) : Collections.emptyList();
        ListenableFuture<EntityExportData> future;
        if (EntityType.ENTITY_GROUP.equals(externalId.getEntityType())) {
            HasId<? extends EntityId> entity = findExportableEntityInDb(user.getTenantId(), internalId != null ? internalId : externalId);
            future = gitServiceQueue.getEntityGroup(user.getTenantId(), versionId, customerIds, ((EntityGroup) entity).getType(), externalId);
        } else {
            future = gitServiceQueue.getEntity(user.getTenantId(), versionId, customerIds, externalId);
        }
        return Futures.transform(future,
                entity -> new EntityDataInfo(entity.hasRelations(), entity.hasAttributes(), entity.hasCredentials(), entity.hasPermissions(), entity.hasGroupEntities()), MoreExecutors.directExecutor());
    }


    @Override
    public ListenableFuture<List<String>> listBranches(TenantId tenantId) throws Exception {
        return gitServiceQueue.listBranches(tenantId);
    }

    @Override
    public RepositorySettings getVersionControlSettings(TenantId tenantId) {
        return repositorySettingsService.get(tenantId);
    }

    @Override
    public ListenableFuture<RepositorySettings> saveVersionControlSettings(TenantId tenantId, RepositorySettings versionControlSettings) {
        var restoredSettings = this.repositorySettingsService.restore(tenantId, versionControlSettings);
        try {
            var future = gitServiceQueue.initRepository(tenantId, restoredSettings);
            return Futures.transform(future, f -> repositorySettingsService.save(tenantId, restoredSettings), MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.debug("{} Failed to init repository: {}", tenantId, versionControlSettings, e);
            throw new RuntimeException("Failed to init repository!", e);
        }
    }

    @Override
    public ListenableFuture<Void> deleteVersionControlSettings(TenantId tenantId) throws Exception {
        if (repositorySettingsService.delete(tenantId)) {
            return gitServiceQueue.clearRepository(tenantId);
        } else {
            return Futures.immediateFuture(null);
        }
    }

    @Override
    public ListenableFuture<Void> checkVersionControlAccess(TenantId tenantId, RepositorySettings settings) throws ThingsboardException {
        settings = this.repositorySettingsService.restore(tenantId, settings);
        try {
            return gitServiceQueue.testRepository(tenantId, settings);
        } catch (Exception e) {
            throw new ThingsboardException(String.format("Unable to access repository: %s", getCauseMessage(e)),
                    ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public ListenableFuture<VersionCreationResult> autoCommit(SecurityUser user, EntityId entityId) throws Exception {
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        var entityType = entityId.getEntityType();
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        SingleEntityVersionCreateRequest vcr = new SingleEntityVersionCreateRequest();
        var autoCommitBranchName = autoCommitConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setEntityId(entityId);
        vcr.setConfig(autoCommitConfig);
        return saveEntitiesVersion(user, vcr);
    }

    @Override
    public ListenableFuture<VersionCreationResult> autoCommit(SecurityUser user, EntityType entityType, List<UUID> entityIds) throws Exception {
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        var autoCommitBranchName = autoCommitConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        ComplexVersionCreateRequest vcr = new ComplexVersionCreateRequest();
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setSyncStrategy(SyncStrategy.MERGE);

        EntityTypeVersionCreateConfig vcrConfig = new EntityTypeVersionCreateConfig();
        vcrConfig.setEntityIds(entityIds);
        vcr.setEntityTypes(Collections.singletonMap(entityType, vcrConfig));
        return saveEntitiesVersion(user, vcr);
    }

    private String getCauseMessage(Exception e) {
        String message;
        if (e.getCause() != null && StringUtils.isNotEmpty(e.getCause().getMessage())) {
            message = e.getCause().getMessage();
        } else {
            message = e.getMessage();
        }
        return message;
    }

    private HasId<? extends EntityId> findExportableEntityInDb(TenantId tenantId, EntityId entityId) {
        HasId<? extends EntityId> entity = exportableEntitiesService.findEntityByTenantIdAndId(tenantId, entityId);
        if (entity == null) throw new IllegalArgumentException("Can't find the entity with id: " + entityId);
        if (!(entity instanceof ExportableEntity)) throw new IllegalArgumentException("Unsupported entity type");
        return entity;
    }
}
