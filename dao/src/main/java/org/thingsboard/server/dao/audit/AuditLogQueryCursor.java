/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.nosql.AuditLogEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuditLogQueryCursor {
    @Getter
    private final UUID tenantId;
    @Getter
    private final List<AuditLogEntity> data;
    @Getter
    private final TimePageLink pageLink;

    private final List<Long> partitions;

    private int partitionIndex;
    private int currentLimit;

    public AuditLogQueryCursor(UUID tenantId, TimePageLink pageLink, List<Long> partitions) {
        this.tenantId = tenantId;
        this.partitions = partitions;
        this.partitionIndex = partitions.size() - 1;
        this.data = new ArrayList<>();
        this.currentLimit = pageLink.getLimit();
        this.pageLink = pageLink;
    }

    public boolean hasNextPartition() {
        return partitionIndex >= 0;
    }

    public boolean isFull() {
        return currentLimit <= 0;
    }

    public long getNextPartition() {
        long partition = partitions.get(partitionIndex);
        partitionIndex--;
        return partition;
    }

    public int getCurrentLimit() {
        return currentLimit;
    }

    public void addData(List<AuditLogEntity> newData) {
        currentLimit -= newData.size();
        data.addAll(newData);
    }
}
