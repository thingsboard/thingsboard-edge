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
package org.thingsboard.server.dao.sql.event;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.util.HsqlDao;

import javax.persistence.Query;

@HsqlDao
@Repository
public class HsqlEventInsertRepository extends AbstractEventInsertRepository {

    private static final String P_KEY_CONFLICT_STATEMENT = "(event.id=I.id)";
    private static final String UNQ_KEY_CONFLICT_STATEMENT = "(event.tenant_id=I.tenant_id AND event.entity_type=I.entity_type AND event.entity_id=I.entity_id AND event.event_type=I.event_type AND event.event_uid=I.event_uid)";

    private static final String INSERT_OR_UPDATE_ON_P_KEY_CONFLICT = getInsertString(P_KEY_CONFLICT_STATEMENT);
    private static final String INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT = getInsertString(UNQ_KEY_CONFLICT_STATEMENT);

    @Override
    public EventEntity saveOrUpdate(EventEntity entity) {
        return saveAndGet(entity, INSERT_OR_UPDATE_ON_P_KEY_CONFLICT, INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT);
    }

    @Override
    protected EventEntity doProcessSaveOrUpdate(EventEntity entity, String query) {
        getQuery(entity, query).executeUpdate();
        return entityManager.find(EventEntity.class, entity.getUuid());
    }

    protected Query getQuery(EventEntity entity, String query) {
        return entityManager.createNativeQuery(query, EventEntity.class)
                .setParameter("id", entity.getUuid().toString())
                .setParameter("created_time", entity.getCreatedTime())
                .setParameter("body", entity.getBody().toString())
                .setParameter("entity_id", entity.getEntityId().toString())
                .setParameter("entity_type", entity.getEntityType().name())
                .setParameter("event_type", entity.getEventType())
                .setParameter("event_uid", entity.getEventUid())
                .setParameter("tenant_id", entity.getTenantId().toString())
                .setParameter("ts", entity.getTs());
    }

    private static String getInsertString(String conflictStatement) {
        return "MERGE INTO event USING (VALUES UUID(:id), :created_time, :body, UUID(:entity_id), :entity_type, :event_type, :event_uid, UUID(:tenant_id), :ts) I (id, created_time, body, entity_id, entity_type, event_type, event_uid, tenant_id, ts) ON " + conflictStatement
                + " WHEN MATCHED THEN UPDATE SET event.id = I.id, event.created_time = I.created_time, event.body = I.body, event.entity_id = I.entity_id, event.entity_type = I.entity_type, event.event_type = I.event_type, event.event_uid = I.event_uid, event.tenant_id = I.tenant_id, event.ts = I.ts" +
                " WHEN NOT MATCHED THEN INSERT (id, created_time, body, entity_id, entity_type, event_type, event_uid, tenant_id, ts) VALUES (I.id, I.created_time, I.body, I.entity_id, I.entity_type, I.event_type, I.event_uid, I.tenant_id, I.ts)";
    }
}