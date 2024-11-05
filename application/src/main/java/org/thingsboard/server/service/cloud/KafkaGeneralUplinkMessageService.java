/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

@Slf4j
@Service
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
public class KafkaGeneralUplinkMessageService extends KafkaUplinkMessageService implements GeneralUplinkMessageService {

    @Override
    protected PageData<CloudEvent> findCloudEvents(TenantId tenantId) {
        PageData<CloudEvent> cloudEvents = cloudEventService.findCloudEvents(tenantId, null, null, null);

        cloudEventService.commit(false);

        return cloudEvents;
    }

    @Override
    public TimePageLink newCloudEventsAvailable(TenantId tenantId, Long queueSeqIdStart) {
        PageData<CloudEvent> cloudEvents = cloudEventService.findCloudEvents(tenantId, null, null, null);

        return cloudEvents.getTotalElements() != 0 ? new TimePageLink(0) : null;
    }

    @Override
    public ListenableFuture<Long> getQueueStartTs(TenantId tenantId) {
        SettableFuture<Long> futureToSet = SettableFuture.create();

        futureToSet.set(0L);

        return futureToSet;
    }

    @Override
    protected boolean newMessagesAvailableInGeneralQueue(TenantId tenantId) {
        return false;
    }
}
