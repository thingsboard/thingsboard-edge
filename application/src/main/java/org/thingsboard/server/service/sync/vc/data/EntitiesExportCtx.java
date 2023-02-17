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
package org.thingsboard.server.service.sync.vc.data;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public abstract class EntitiesExportCtx<R extends VersionCreateRequest> {

    protected final User user;
    protected final CommitGitRequest commit;
    protected final R request;
    private final List<ListenableFuture<Void>> futures;
    private final Map<EntityId, EntityId> externalIdMap;
    private final boolean exportRelatedCustomers;
    private final boolean exportRelatedEntities;


    public EntitiesExportCtx(User user, CommitGitRequest commit, R request, boolean exportRelatedCustomers, boolean exportRelatedEntities) {
        this.user = user;
        this.commit = commit;
        this.request = request;
        this.futures = new ArrayList<>();
        this.externalIdMap = new HashMap<>();
        this.exportRelatedCustomers = exportRelatedCustomers;
        this.exportRelatedEntities = exportRelatedEntities;
    }

    protected <T extends R> EntitiesExportCtx(EntitiesExportCtx<T> other) {
        this.user = other.getUser();
        this.commit = other.getCommit();
        this.request = other.getRequest();
        this.futures = other.getFutures();
        this.externalIdMap = other.getExternalIdMap();
        this.exportRelatedCustomers = other.isExportRelatedCustomers();
        this.exportRelatedEntities = other.isExportRelatedEntities();
    }

    public void add(ListenableFuture<Void> future) {
        futures.add(future);
    }

    public TenantId getTenantId() {
        return user.getTenantId();
    }

    protected static EntityExportSettings buildExportSettings(VersionCreateConfig config) {
        return EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .exportGroupEntities(config.isSaveGroupEntities())
                .exportPermissions(config.isSavePermissions())
                .build();
    }

    public abstract EntityExportSettings getSettings();

    @SuppressWarnings("unchecked")
    public <ID extends EntityId> ID getExternalId(ID internalId) {
        var result = externalIdMap.get(internalId);
        log.debug("[{}][{}] Local cache {} for id", internalId.getEntityType(), internalId.getId(), result != null ? "hit" : "miss");
        return (ID) result;
    }

    public void putExternalId(EntityId internalId, EntityId externalId) {
        log.debug("[{}][{}] Local cache put: {}", internalId.getEntityType(), internalId.getId(), externalId);
        externalIdMap.put(internalId, externalId != null ? externalId : internalId);
    }

    public boolean shouldExportEntities(EntityType entityType) {
        return entityType != EntityType.USER;
    }

}
