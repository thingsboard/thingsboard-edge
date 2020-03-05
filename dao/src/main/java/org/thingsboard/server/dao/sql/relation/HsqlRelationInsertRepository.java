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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@HsqlDao
@SqlDao
@Repository
@Transactional
public class HsqlRelationInsertRepository extends AbstractRelationInsertRepository implements RelationInsertRepository {

    private static final String INSERT_ON_CONFLICT_DO_UPDATE = "MERGE INTO relation USING (VALUES :fromId, :fromType, :toId, :toType, :relationTypeGroup, :relationType, :additionalInfo) R " +
            "(from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info) " +
            "ON (relation.from_id = R.from_id AND relation.from_type = R.from_type AND relation.relation_type_group = R.relation_type_group AND relation.relation_type = R.relation_type AND relation.to_id = R.to_id AND relation.to_type = R.to_type) " +
            "WHEN MATCHED THEN UPDATE SET relation.additional_info = R.additional_info " +
            "WHEN NOT MATCHED THEN INSERT (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info) VALUES (R.from_id, R.from_type, R.to_id, R.to_type, R.relation_type_group, R.relation_type, R.additional_info)";

    @Override
    public RelationEntity saveOrUpdate(RelationEntity entity) {
        return processSaveOrUpdate(entity);
    }

    @Override
    protected RelationEntity processSaveOrUpdate(RelationEntity entity) {
        getQuery(entity, INSERT_ON_CONFLICT_DO_UPDATE).executeUpdate();
        return entityManager.find(RelationEntity.class, new RelationCompositeKey(entity.toData()));
    }
}
