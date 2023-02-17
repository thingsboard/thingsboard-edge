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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.GitRepository.Diff;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface GitRepositoryService {

    Set<TenantId> getActiveRepositoryTenants();

    void prepareCommit(PendingCommit pendingCommit);

    PageData<EntityVersion> listVersions(TenantId tenantId, String branch, String path, PageLink pageLink) throws Exception;

    Stream<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String versionId, String folder, EntityType entityType, boolean groups, boolean recursive) throws Exception;

    void testRepository(TenantId tenantId, RepositorySettings settings) throws Exception;

    void initRepository(TenantId tenantId, RepositorySettings settings) throws Exception;

    RepositorySettings getRepositorySettings(TenantId tenantId) throws Exception;

    void clearRepository(TenantId tenantId) throws IOException;

    void add(PendingCommit commit, String relativePath, String entityDataJson) throws IOException;

    void deleteFolderContent(PendingCommit commit, String folder, boolean recursively) throws IOException;

    VersionCreationResult push(PendingCommit commit);

    void cleanUp(PendingCommit commit);

    void abort(PendingCommit commit);

    List<BranchInfo> listBranches(TenantId tenantId);

    String getFileContentAtCommit(TenantId tenantId, String relativePath, String versionId) throws IOException;

    List<Diff> getVersionsDiffList(TenantId tenantId, String path, String versionId1, String versionId2) throws IOException;

    String getContentsDiff(TenantId tenantId, String content1, String content2) throws IOException;

    void fetch(TenantId tenantId) throws GitAPIException;
}
