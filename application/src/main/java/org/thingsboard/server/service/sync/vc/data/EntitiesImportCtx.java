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
package org.thingsboard.server.service.sync.vc.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.sync.ThrowingRunnable;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;

import java.util.*;

@RequiredArgsConstructor
@Data
@Slf4j
public class EntitiesImportCtx {
    private final String versionId;
    private final Map<EntityType, EntityTypeLoadResult> results = new HashMap<>();
    private final Map<EntityType, Set<EntityId>> importedEntities = new HashMap<>();
    private final Map<EntityId, EntityImportSettings> toReimport = new HashMap<>();
    private final List<ThrowingRunnable> saveReferencesCallbacks = new ArrayList<>();
    private final List<ThrowingRunnable> sendEventsCallbacks = new ArrayList<>();
    private final Map<EntityId, EntityId> externalToInternalIdMap = new HashMap<>();

    public void put(EntityType entityType, EntityTypeLoadResult importEntities) {
        results.put(entityType, importEntities);
    }

    public EntityTypeLoadResult get(EntityType entityType) {
        return results.get(entityType);
    }

    public void executeCallbacks() {
        for (ThrowingRunnable saveReferencesCallback : saveReferencesCallbacks) {
            try {
                saveReferencesCallback.run();
            } catch (ThingsboardException e) {
                throw new RuntimeException(e);
            }
        }
        for (ThrowingRunnable sendEventsCallback : sendEventsCallbacks) {
            try {
                sendEventsCallback.run();
            } catch (Exception e) {
                log.error("Failed to send events for entity", e);
            }
        }
    }

    public EntityId getInternalId(EntityId externalId){
        return externalToInternalIdMap.get(externalId);
    }

    public void putInternalId(EntityId externalId, EntityId internalId) {
        externalToInternalIdMap.put(externalId, internalId);
    }
}
