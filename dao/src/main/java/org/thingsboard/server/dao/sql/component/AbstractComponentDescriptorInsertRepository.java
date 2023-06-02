/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Slf4j
public abstract class AbstractComponentDescriptorInsertRepository implements ComponentDescriptorInsertRepository {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    protected ComponentDescriptorEntity saveAndGet(ComponentDescriptorEntity entity, String insertOrUpdateOnPrimaryKeyConflict, String insertOrUpdateOnUniqueKeyConflict) {
        ComponentDescriptorEntity componentDescriptorEntity = null;
        TransactionStatus insertTransaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            componentDescriptorEntity = processSaveOrUpdate(entity, insertOrUpdateOnPrimaryKeyConflict);
            transactionManager.commit(insertTransaction);
        } catch (Throwable throwable) {
            transactionManager.rollback(insertTransaction);
            if (throwable.getCause() instanceof ConstraintViolationException) {
                log.trace("Insert request leaded in a violation of a defined integrity constraint {} for Component Descriptor with id {}, name {} and entityType {}", throwable.getMessage(), entity.getUuid(), entity.getName(), entity.getType());
                TransactionStatus transaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    componentDescriptorEntity = processSaveOrUpdate(entity, insertOrUpdateOnUniqueKeyConflict);
                    transactionManager.commit(transaction);
                } catch (Throwable th) {
                    log.trace("Could not execute the update statement for Component Descriptor with id {}, name {} and entityType {}", entity.getUuid(), entity.getName(), entity.getType());
                    transactionManager.rollback(transaction);
                }
            } else {
                log.trace("Could not execute the insert statement for Component Descriptor with id {}, name {} and entityType {}", entity.getUuid(), entity.getName(), entity.getType());
            }
        }
        return componentDescriptorEntity;
    }

    @Modifying
    protected abstract ComponentDescriptorEntity doProcessSaveOrUpdate(ComponentDescriptorEntity entity, String query);

    protected Query getQuery(ComponentDescriptorEntity entity, String query) {
        return entityManager.createNativeQuery(query, ComponentDescriptorEntity.class)
                .setParameter("id", entity.getUuid())
                .setParameter("created_time", entity.getCreatedTime())
                .setParameter("actions", entity.getActions())
                .setParameter("clazz", entity.getClazz())
                .setParameter("configuration_descriptor", entity.getConfigurationDescriptor().toString())
                .setParameter("name", entity.getName())
                .setParameter("scope", entity.getScope().name())
                .setParameter("type", entity.getType().name())
                .setParameter("clustering_mode", entity.getClusteringMode().name());
    }

    private ComponentDescriptorEntity processSaveOrUpdate(ComponentDescriptorEntity entity, String query) {
        return doProcessSaveOrUpdate(entity, query);
    }

    private TransactionStatus getTransactionStatus(int propagationRequired) {
        DefaultTransactionDefinition insertDefinition = new DefaultTransactionDefinition();
        insertDefinition.setPropagationBehavior(propagationRequired);
        return transactionManager.getTransaction(insertDefinition);
    }
}
