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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

public class EntitiesRelatedDeviceIdAsyncLoader {

    public static ListenableFuture<DeviceId> findDeviceAsync(
            TbContext ctx,
            EntityId originator,
            DeviceRelationsQuery deviceRelationsQuery
    ) {
        var deviceService = ctx.getDeviceService();
        var query = buildQuery(originator, deviceRelationsQuery);
        var devicesListFuture = deviceService.findDevicesByQuery(ctx.getTenantId(), query);
        return Futures.transformAsync(devicesListFuture,
                deviceList -> CollectionUtils.isNotEmpty(deviceList) ?
                        Futures.immediateFuture(deviceList.get(0).getId())
                        : Futures.immediateFuture(null), ctx.getDbCallbackExecutor());
    }

    private static DeviceSearchQuery buildQuery(EntityId originator, DeviceRelationsQuery deviceRelationsQuery) {
        var query = new DeviceSearchQuery();
        var parameters = new RelationsSearchParameters(
                originator,
                deviceRelationsQuery.getDirection(),
                deviceRelationsQuery.getMaxLevel(),
                deviceRelationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setRelationType(deviceRelationsQuery.getRelationType());
        query.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());
        return query;
    }

}
