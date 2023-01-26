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
package org.thingsboard.server.queue.discovery;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;

@Data
@AllArgsConstructor
public class QueueKey {

    private final ServiceType type;
    private final String queueName;
    private final TenantId tenantId;

    public QueueKey(ServiceType type, Queue queue) {
        this.type = type;
        this.queueName = queue.getName();
        this.tenantId = queue.getTenantId();
    }

    public QueueKey(ServiceType type, QueueRoutingInfo queueRoutingInfo) {
        this.type = type;
        this.queueName = queueRoutingInfo.getQueueName();
        this.tenantId = queueRoutingInfo.getTenantId();
    }

    public QueueKey(ServiceType type, TenantId tenantId) {
        this.type = type;
        this.queueName = DataConstants.MAIN_QUEUE_NAME;
        this.tenantId = tenantId != null ? tenantId : TenantId.SYS_TENANT_ID;
    }

    public QueueKey(ServiceType type) {
        this.type = type;
        this.queueName = DataConstants.MAIN_QUEUE_NAME;
        this.tenantId = TenantId.SYS_TENANT_ID;
    }

    public QueueKey(ServiceType type, String queueName) {
        this.type = type;
        this.queueName = queueName;
        this.tenantId = TenantId.SYS_TENANT_ID;
    }

    @Override
    public String toString() {
        return "QK(" + queueName + "," + type + "," +
                (TenantId.SYS_TENANT_ID.equals(tenantId) ? "system" : tenantId) +
                ')';
    }
}
