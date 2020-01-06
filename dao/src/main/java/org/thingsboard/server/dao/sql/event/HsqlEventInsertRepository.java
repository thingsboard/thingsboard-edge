/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
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
        return entityManager.find(EventEntity.class, UUIDConverter.fromTimeUUID(entity.getId()));
    }

    private static String getInsertString(String conflictStatement) {
        return "MERGE INTO event USING (VALUES :id, :body, :entity_id, :entity_type, :event_type, :event_uid, :tenant_id) I (id, body, entity_id, entity_type, event_type, event_uid, tenant_id) ON " + conflictStatement + " WHEN MATCHED THEN UPDATE SET event.id = I.id, event.body = I.body, event.entity_id = I.entity_id, event.entity_type = I.entity_type, event.event_type = I.event_type, event.event_uid = I.event_uid, event.tenant_id = I.tenant_id" +
                " WHEN NOT MATCHED THEN INSERT (id, body, entity_id, entity_type, event_type, event_uid, tenant_id) VALUES (I.id, I.body, I.entity_id, I.entity_type, I.event_type, I.event_uid, I.tenant_id)";
    }
}