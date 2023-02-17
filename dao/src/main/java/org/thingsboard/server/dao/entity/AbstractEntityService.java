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
package org.thingsboard.server.dao.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class AbstractEntityService {

    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Lazy
    @Autowired
    protected RelationService relationService;

    @Lazy
    @Autowired
    protected AlarmService alarmService;

    @Lazy
    @Autowired
    protected EntityGroupService entityGroupService;

    @Lazy
    @Autowired(required = false)
    protected EdgeService edgeService;

    protected void createRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    protected void deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    protected void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        relationService.deleteEntityRelations(tenantId, entityId);
        log.trace("Executing deleteEntityAlarms [{}]", entityId);
        alarmService.deleteEntityAlarmRelations(tenantId, entityId);
    }

    protected void deleteEntityGroups(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityGroups [{}]", entityId);
        entityGroupService.deleteAllEntityGroups(tenantId, entityId);
    }

    public static final void checkConstraintViolation(Exception t, String constraintName, String constraintMessage) {
        checkConstraintViolation(t, Collections.singletonMap(constraintName, constraintMessage));
    }

    public static final void checkConstraintViolation(Exception t, String constraintName1, String constraintMessage1, String constraintName2, String constraintMessage2) {
        checkConstraintViolation(t, Map.of(constraintName1, constraintMessage1, constraintName2, constraintMessage2));
    }

    public static final void checkConstraintViolation(Exception t, Map<String, String> constraints) {
        var exOpt = DaoUtil.extractConstraintViolationException(t);
        if (exOpt.isPresent()) {
            var ex = exOpt.get();
            if (StringUtils.isNotEmpty(ex.getConstraintName())) {
                var constraintName = ex.getConstraintName();
                for (var constraintMessage : constraints.entrySet()) {
                    if (constraintName.equals(constraintMessage.getKey())) {
                        throw new DataValidationException(constraintMessage.getValue());
                    }
                }
            }
        }
    }

}
