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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
@Slf4j
public class TbEntityRemoteSubsInfo {
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<String, TbSubscriptionsInfo> subs = new ConcurrentHashMap<>(); // By service ID

    public boolean updateAndCheckIsEmpty(String serviceId, TbEntitySubEvent event) {
        var current = subs.get(serviceId);
        if (current != null && current.seqNumber > event.getSeqNumber()) {
            log.warn("[{}][{}] Duplicate subscription event received. Current: {}, Event: {}",
                    tenantId, entityId, current, event.getInfo());
            return false;
        }
        switch (event.getType()) {
            case CREATED:
                subs.put(serviceId, event.getInfo());
                break;
            case UPDATED:
                var newSubInfo = event.getInfo();
                if (newSubInfo.isEmpty()) {
                    subs.remove(serviceId);
                    return isEmpty();
                } else {
                    subs.put(serviceId, newSubInfo);
                }
                break;
            case DELETED:
                subs.remove(serviceId);
                return isEmpty();
        }
        return false;
    }

    public boolean removeAndCheckIsEmpty(String serviceId) {
        if (subs.remove(serviceId) != null) {
            return subs.isEmpty();
        } else {
            return false;
        }
    }

    public boolean isEmpty() {
        return subs.isEmpty();
    }
}
