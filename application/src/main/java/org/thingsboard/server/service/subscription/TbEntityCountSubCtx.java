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
package org.thingsboard.server.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityCountUpdate;

@Slf4j
public class TbEntityCountSubCtx extends TbAbstractSubCtx<EntityCountQuery> {

    private volatile int result;

    public TbEntityCountSubCtx(String serviceId, TelemetryWebSocketService wsService, EntityService entityService,
                               TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                               SubscriptionServiceStatistics stats, TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
    }

    @Override
    public void fetchData() {
        result = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), getMergedUserPermissions(), query);
        sendWsMsg(new EntityCountUpdate(cmdId, result));
    }

    @Override
    protected void update() {
        int newCount = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), getMergedUserPermissions(), query);
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new EntityCountUpdate(cmdId, result));
        }
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
