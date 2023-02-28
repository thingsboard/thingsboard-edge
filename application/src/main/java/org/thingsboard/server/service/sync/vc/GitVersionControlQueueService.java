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
package org.thingsboard.server.service.sync.vc;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.EntityVersionsDiff;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.gen.transport.TransportProtos.VersionControlResponseMsg;
import org.thingsboard.server.service.sync.vc.data.CommitGitRequest;

import java.util.List;
import java.util.UUID;

public interface GitVersionControlQueueService {

    ListenableFuture<CommitGitRequest> prepareCommit(User user, VersionCreateRequest request);

    ListenableFuture<Void> addToCommit(CommitGitRequest commit, List<CustomerId> parents, EntityExportData<? extends ExportableEntity<? extends EntityId>> entityData);

    ListenableFuture<Void> addToCommit(CommitGitRequest commit, List<CustomerId> parents, EntityType type, EntityId groupExternalId, List<EntityId> groupEntityIds);

    ListenableFuture<Void> addToCommit(CommitGitRequest commit, EntityExportData<ExportableEntity<EntityId>> entityData);

    ListenableFuture<Void> deleteAll(CommitGitRequest pendingCommit, EntityType entityType);

    ListenableFuture<VersionCreationResult> push(CommitGitRequest commit);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, List<CustomerId> hierarchy, EntityType entityType, EntityId groupId, PageLink pageLink);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, List<CustomerId> hierarchy, EntityId entityId, PageLink pageLink);

    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType);

    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId);

    ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId);

    ListenableFuture<EntityExportData> getEntity(TenantId tenantId, String versionId, List<CustomerId> hierarchy, EntityId entityId);

    ListenableFuture<EntityExportData> getEntityGroup(TenantId tenantId, String versionId, List<CustomerId> hierarchy, EntityType groupType, EntityId groupId);

    ListenableFuture<List<EntityExportData>> getEntities(TenantId tenantId, String versionId, List<CustomerId> hierarchy, EntityType entityType, boolean groups, boolean recursive, int offset, int limit);

    ListenableFuture<List<EntityExportData>> getEntities(TenantId tenantId, String versionId, List<CustomerId> hierarchy, EntityType entityType, List<UUID> ids);

    ListenableFuture<List<EntityVersionsDiff>> getVersionsDiff(TenantId tenantId, EntityType entityType, EntityId externalId, String versionId1, String versionId2);

    ListenableFuture<Void> initRepository(TenantId tenantId, RepositorySettings settings);

    ListenableFuture<Void> testRepository(TenantId tenantId, RepositorySettings settings);

    ListenableFuture<Void> clearRepository(TenantId tenantId);

    void processResponse(VersionControlResponseMsg vcResponseMsg);

    ListenableFuture<List<EntityId>> getGroupEntityIds(TenantId tenantId, String versionId, List<CustomerId> ownerIds, EntityType type, EntityId externalId);
}
