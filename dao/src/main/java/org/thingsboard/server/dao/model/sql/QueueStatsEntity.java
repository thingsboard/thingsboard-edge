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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.QUEUE_STATS_TABLE_NAME)
public class QueueStatsEntity extends BaseSqlEntity<QueueStats> {

    @Column(name = ModelConstants.QUEUE_STATS_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.QUEUE_STATS_QUEUE_NAME_PROPERTY)
    private String queueName;

    @Column(name = ModelConstants.QUEUE_STATS_SERVICE_ID_PROPERTY)
    private String serviceId;

    public QueueStatsEntity() {
    }

    public QueueStatsEntity(QueueStats queueStats) {
        if (queueStats.getId() != null) {
            this.setId(queueStats.getId().getId());
        }
        this.setCreatedTime(queueStats.getCreatedTime());
        this.tenantId = DaoUtil.getId(queueStats.getTenantId());
        this.queueName = queueStats.getQueueName();
        this.serviceId = queueStats.getServiceId();
    }

    @Override
    public QueueStats toData() {
        QueueStats queueStats = new QueueStats(new QueueStatsId(getUuid()));
        queueStats.setCreatedTime(createdTime);
        queueStats.setTenantId(TenantId.fromUUID(tenantId));
        queueStats.setQueueName(queueName);
        queueStats.setServiceId(serviceId);
        return queueStats;
    }
}