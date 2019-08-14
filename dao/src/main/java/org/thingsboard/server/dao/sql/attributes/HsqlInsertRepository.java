/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.HsqlDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@SqlDao
@HsqlDao
@Repository
public class HsqlInsertRepository extends AttributeKvInsertRepository {

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String INSERT_BOOL_STATEMENT = getInsertString(BOOL_V);
    private static final String INSERT_STR_STATEMENT = getInsertString(STR_V);
    private static final String INSERT_LONG_STATEMENT = getInsertString(LONG_V);
    private static final String INSERT_DBL_STATEMENT = getInsertString(DBL_V);

    private static final String WHERE_STATEMENT = " WHERE entity_type = :entity_type AND entity_id = :entity_id AND attribute_type = :attribute_type AND attribute_key = :attribute_key";

    private static final String UPDATE_BOOL_STATEMENT = getUpdateString(BOOL_V);
    private static final String UPDATE_STR_STATEMENT = getUpdateString(STR_V);
    private static final String UPDATE_LONG_STATEMENT = getUpdateString(LONG_V);
    private static final String UPDATE_DBL_STATEMENT = getUpdateString(DBL_V);

    @Override
    public void saveOrUpdate(AttributeKvEntity entity) {
        DefaultTransactionDefinition insertDefinition = new DefaultTransactionDefinition();
        insertDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus insertTransaction = transactionManager.getTransaction(insertDefinition);
        try {
            processSaveOrUpdate(entity, INSERT_BOOL_STATEMENT, INSERT_STR_STATEMENT, INSERT_LONG_STATEMENT, INSERT_DBL_STATEMENT);
            transactionManager.commit(insertTransaction);
        } catch (Throwable e) {
            transactionManager.rollback(insertTransaction);
            if (e.getCause() instanceof ConstraintViolationException) {
                log.trace("Insert request leaded in a violation of a defined integrity constraint {} for Entity with entityId {} and entityType {}", e.getMessage(), UUIDConverter.fromString(entity.getId().getEntityId()), entity.getId().getEntityType());
                DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
                definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                TransactionStatus transaction = transactionManager.getTransaction(definition);
                try {
                    processSaveOrUpdate(entity, UPDATE_BOOL_STATEMENT, UPDATE_STR_STATEMENT, UPDATE_LONG_STATEMENT, UPDATE_DBL_STATEMENT);
                } catch (Throwable th) {
                    log.trace("Could not execute the update statement for Entity with entityId {} and entityType {}", UUIDConverter.fromString(entity.getId().getEntityId()), entity.getId().getEntityType());
                    transactionManager.rollback(transaction);
                }
                transactionManager.commit(transaction);
            } else {
                log.trace("Could not execute the insert statement for Entity with entityId {} and entityType {}", UUIDConverter.fromString(entity.getId().getEntityId()), entity.getId().getEntityType());
            }
        }
    }

    private static String getInsertString(String value) {
        return "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, " + value + ", last_update_ts) VALUES (:entity_type, :entity_id, :attribute_type, :attribute_key, :" + value + ", :last_update_ts)";
    }

    private static String getUpdateString(String value) {
        return "UPDATE attribute_kv SET " + value + " = :" + value + ", last_update_ts = :last_update_ts" + WHERE_STATEMENT;
    }
}