// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cloud;

import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;

import java.util.UUID;

public interface CloudEventDao extends TsKvCloudEventDao {

    long countEventsByTenantIdAndEntityIdAndActionAndTypeAndStartTimeAndEndTime(UUID tenantId,
                                                                                UUID entityId,
                                                                                CloudEventType cloudEventType,
                                                                                EdgeEventActionType cloudEventAction,
                                                                                Long startTime,
                                                                                Long endTime);

}
