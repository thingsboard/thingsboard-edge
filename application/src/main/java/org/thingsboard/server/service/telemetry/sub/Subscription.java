/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.telemetry.sub;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.service.telemetry.TelemetryFeature;

import java.util.Map;

@Data
@AllArgsConstructor
public class Subscription {

    private final SubscriptionState sub;
    private final boolean local;
    private ServerAddress server;
    private long startTime;
    private long endTime;

    public Subscription(SubscriptionState sub, boolean local, ServerAddress server) {
        this(sub, local, server, 0L, 0L);
    }

    public String getWsSessionId() {
        return getSub().getWsSessionId();
    }

    public int getSubscriptionId() {
        return getSub().getSubscriptionId();
    }

    public EntityId getEntityId() {
        return getSub().getEntityId();
    }

    public TelemetryFeature getType() {
        return getSub().getType();
    }

    public String getScope() {
        return getSub().getScope();
    }

    public boolean isAllKeys() {
        return getSub().isAllKeys();
    }

    public Map<String, Long> getKeyStates() {
        return getSub().getKeyStates();
    }

    public void setKeyState(String key, long ts) {
        getSub().getKeyStates().put(key, ts);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "sub=" + sub +
                ", local=" + local +
                ", server=" + server +
                '}';
    }
}
