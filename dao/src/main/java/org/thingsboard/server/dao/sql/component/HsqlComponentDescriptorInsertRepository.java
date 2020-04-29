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
package org.thingsboard.server.dao.sql.component;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
@HsqlDao
@Repository
public class HsqlComponentDescriptorInsertRepository extends AbstractComponentDescriptorInsertRepository {

    private static final String P_KEY_CONFLICT_STATEMENT = "(component_descriptor.id=I.id)";
    private static final String UNQ_KEY_CONFLICT_STATEMENT = "(component_descriptor.clazz=I.clazz)";

    private static final String INSERT_OR_UPDATE_ON_P_KEY_CONFLICT = getInsertString(P_KEY_CONFLICT_STATEMENT);
    private static final String INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT = getInsertString(UNQ_KEY_CONFLICT_STATEMENT);

    @Override
    public ComponentDescriptorEntity saveOrUpdate(ComponentDescriptorEntity entity) {
        return saveAndGet(entity, INSERT_OR_UPDATE_ON_P_KEY_CONFLICT, INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT);
    }

    @Override
    protected ComponentDescriptorEntity doProcessSaveOrUpdate(ComponentDescriptorEntity entity, String query) {
        getQuery(entity, query).executeUpdate();
        return entityManager.find(ComponentDescriptorEntity.class, UUIDConverter.fromTimeUUID(entity.getUuid()));
    }

    private static String getInsertString(String conflictStatement) {
        return "MERGE INTO component_descriptor USING (VALUES :id, :actions, :clazz, :configuration_descriptor, :name, :scope, :search_text, :type) I (id, actions, clazz, configuration_descriptor, name, scope, search_text, type) ON " + conflictStatement + " WHEN MATCHED THEN UPDATE SET component_descriptor.id = I.id, component_descriptor.actions = I.actions, component_descriptor.clazz = I.clazz, component_descriptor.configuration_descriptor = I.configuration_descriptor, component_descriptor.name = I.name, component_descriptor.scope = I.scope, component_descriptor.search_text = I.search_text, component_descriptor.type = I.type" +
                " WHEN NOT MATCHED THEN INSERT (id, actions, clazz, configuration_descriptor, name, scope, search_text, type) VALUES (I.id, I.actions, I.clazz, I.configuration_descriptor, I.name, I.scope, I.search_text, I.type)";
    }
}
