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
package org.thingsboard.server.dao.sql.relation;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
@Slf4j
@Component
public class JpaRelationDao extends JpaAbstractDaoListeningExecutorService implements RelationDao {

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationInsertRepository relationInsertRepository;

    @Override
    public List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        from.getId(),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        typeGroup.name()));
    }

    @Override
    public List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        to.getId(),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name()));
    }

    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(() -> relationRepository.existsById(key));
    }

    @Override
    public Boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return relationRepository.existsById(key);
    }

    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return DaoUtil.getData(relationRepository.findById(key));
    }

    private RelationCompositeKey getRelationCompositeKey(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return new RelationCompositeKey(from.getId(),
                from.getEntityType().name(),
                to.getId(),
                to.getEntityType().name(),
                relationType,
                typeGroup.name());
    }

    @Override
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        return relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null;
    }

    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        return service.submit(() -> relationInsertRepository.saveOrUpdate(new RelationEntity(relation)) != null);
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    private boolean deleteRelationIfExists(RelationCompositeKey key) {
        boolean relationExistsBeforeDelete = relationRepository.existsById(key);
        if (relationExistsBeforeDelete) {
            try {
                relationRepository.deleteById(key);
            } catch (DataAccessException e) {
                log.debug("[{}] Concurrency exception while deleting relation", key, e);
            }
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public boolean deleteOutboundRelations(TenantId tenantId, EntityId entity) {
        boolean relationExistsBeforeDelete = false;
        try {
            relationExistsBeforeDelete = relationRepository
                    .findAllByFromIdAndFromType(entity.getId(), entity.getEntityType().name())
                    .size() > 0;
            if (relationExistsBeforeDelete) {
                relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
            }
        } catch (ConcurrencyFailureException e) {
            log.debug("Concurrency exception while deleting relations [{}]", entity, e);
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity) {
        return service.submit(
                () -> {
                    boolean relationExistsBeforeDelete = relationRepository
                            .findAllByFromIdAndFromType(entity.getId(), entity.getEntityType().name())
                            .size() > 0;
                    if (relationExistsBeforeDelete) {
                        relationRepository.deleteByFromIdAndFromType(entity.getId(), entity.getEntityType().name());
                    }
                    return relationExistsBeforeDelete;
                });
    }

    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit) {
        return DaoUtil.convertDataList(relationRepository.findRuleNodeToRuleChainRelations(ruleChainType, PageRequest.of(0, limit)));
    }
}
