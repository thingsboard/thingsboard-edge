/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.housekeeper.LatestTsDeletionHousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.TsHistoryDeletionHousekeeperTask;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final TimeseriesService timeseriesService;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        EntityId entityId = task.getEntityId();
        List<String> keys = timeseriesService.findAllKeysByEntityIds(tenantId, List.of(entityId));

        for (String key : keys) {
            var latestTsDeletionTask = new LatestTsDeletionHousekeeperTask(tenantId, entityId, key);
            housekeeperClient.submitTask(latestTsDeletionTask);

            var tsHistoryDeletionTask = new TsHistoryDeletionHousekeeperTask(tenantId, entityId, key);
            housekeeperClient.submitTask(tsHistoryDeletionTask);
        }

        log.trace("[{}][{}][{}] Submitted latest and ts history deletion tasks for {} keys", tenantId, entityId.getEntityType(), entityId, keys.size());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_TELEMETRY;
    }

}
