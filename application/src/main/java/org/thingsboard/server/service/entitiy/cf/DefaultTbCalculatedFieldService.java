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
package org.thingsboard.server.service.entitiy.cf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbCalculatedFieldService extends AbstractTbEntityService implements TbCalculatedFieldService {

    private final CalculatedFieldService calculatedFieldService;

    @Override
    public CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException {
        ActionType actionType = calculatedField.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = calculatedField.getTenantId();
        try {
            if (ActionType.UPDATED.equals(actionType)) {
                CalculatedField existingCf = calculatedFieldService.findById(tenantId, calculatedField.getId());
                checkForEntityChange(existingCf, calculatedField);
            }
            checkEntityExistence(tenantId, calculatedField.getEntityId());
            checkReferencedEntities(calculatedField.getConfiguration(), user);
            CalculatedField savedCalculatedField = checkNotNull(calculatedFieldService.save(calculatedField));
            logEntityActionService.logEntityAction(tenantId, savedCalculatedField.getId(), savedCalculatedField, actionType, user);
            return savedCalculatedField;
        } catch (ThingsboardException e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CALCULATED_FIELD), calculatedField, actionType, user, e);
            throw e;
        }
    }

    @Override
    public CalculatedField findById(CalculatedFieldId calculatedFieldId, SecurityUser user) {
        return calculatedFieldService.findById(user.getTenantId(), calculatedFieldId);
    }

    @Override
    public PageData<CalculatedField> findAllByTenantIdAndEntityId(EntityId entityId, SecurityUser user, PageLink pageLink) {
        TenantId tenantId = user.getTenantId();
        checkEntityExistence(tenantId, entityId);
        return calculatedFieldService.findAllCalculatedFieldsByEntityId(tenantId, entityId, pageLink);
    }

    @Override
    @Transactional
    public void delete(CalculatedField calculatedField, SecurityUser user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = calculatedField.getTenantId();
        CalculatedFieldId calculatedFieldId = calculatedField.getId();
        try {
            calculatedFieldService.deleteCalculatedField(tenantId, calculatedFieldId);
            logEntityActionService.logEntityAction(tenantId, calculatedFieldId, calculatedField, actionType, user, calculatedFieldId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CALCULATED_FIELD), actionType, user, e, calculatedFieldId.toString());
            throw e;
        }
    }

    private void checkForEntityChange(CalculatedField oldCalculatedField, CalculatedField newCalculatedField) {
        if (!oldCalculatedField.getEntityId().equals(newCalculatedField.getEntityId())) {
            throw new IllegalArgumentException("Changing the calculated field target entity after initialization is prohibited.");
        }
    }

    private void checkEntityExistence(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case ASSET, DEVICE, ASSET_PROFILE, DEVICE_PROFILE ->
                    Optional.ofNullable(entityService.fetchEntity(tenantId, entityId))
                            .orElseThrow(() -> new IllegalArgumentException(entityId.getEntityType().getNormalName() + " with id [" + entityId.getId() + "] does not exist."));
            default ->
                    throw new IllegalArgumentException("Entity type '" + entityId.getEntityType() + "' does not support calculated fields.");
        }
    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> void checkReferencedEntities(CalculatedFieldConfiguration calculatedFieldConfig, SecurityUser user) throws ThingsboardException {
        List<EntityId> referencedEntityIds = calculatedFieldConfig.getReferencedEntities();
        for (EntityId referencedEntityId : referencedEntityIds) {
            validateEntityId(referencedEntityId, id -> "Invalid entity id " + id);
            E entity = findEntity(user.getTenantId(), referencedEntityId);
            checkNotNull(entity);
            checkEntity(user, entity, Operation.READ);
        }
    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> E findEntity(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case TENANT, CUSTOMER, ASSET, DEVICE -> (E) entityService.fetchEntity(tenantId, entityId).orElse(null);
            default ->
                    throw new IllegalArgumentException("Calculated fields do not support entity type '" + entityId.getEntityType() + "' for referenced entities.");
        };
    }

}
