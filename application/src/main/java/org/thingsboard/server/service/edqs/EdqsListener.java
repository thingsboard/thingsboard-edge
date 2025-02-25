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
package org.thingsboard.server.service.edqs;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "queue.edqs.sync.enabled", havingValue = "true")
public class EdqsListener {

    private final EdqsService edqsService;

    @TransactionalEventListener(fallbackExecution = true)
    public void onUpdate(SaveEntityEvent<?> event) {
        if (event.getEntityId() == null || event.getEntity() == null) {
            return;
        }
        edqsService.onUpdate(event.getTenantId(), event.getEntityId(), event.getEntity());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onDelete(DeleteEntityEvent<?> event) {
        if (event.getEntityId() == null) {
            return;
        }
        edqsService.onDelete(event.getTenantId(), event.getEntityId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent relationEvent) {
        if (relationEvent.getActionType() == ActionType.RELATION_ADD_OR_UPDATE) {
            edqsService.onUpdate(relationEvent.getTenantId(), ObjectType.RELATION, relationEvent.getRelation());
        } else if (relationEvent.getActionType() == ActionType.RELATION_DELETED) {
            edqsService.onDelete(relationEvent.getTenantId(), ObjectType.RELATION, relationEvent.getRelation());
        }
    }

}
