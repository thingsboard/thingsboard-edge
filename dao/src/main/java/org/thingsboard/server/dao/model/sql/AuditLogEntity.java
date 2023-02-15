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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_DATA_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_NAME_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.AUDIT_LOG_COLUMN_FAMILY_NAME)
public class AuditLogEntity extends BaseSqlEntity<AuditLog> implements BaseEntity<AuditLog> {

    @Column(name = AUDIT_LOG_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = AUDIT_LOG_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = AUDIT_LOG_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = AUDIT_LOG_ENTITY_NAME_PROPERTY)
    private String entityName;

    @Column(name = AUDIT_LOG_USER_ID_PROPERTY)
    private UUID userId;

    @Column(name = AUDIT_LOG_USER_NAME_PROPERTY)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ACTION_TYPE_PROPERTY)
    private ActionType actionType;

    @Type(type = "json")
    @Column(name = AUDIT_LOG_ACTION_DATA_PROPERTY)
    private JsonNode actionData;

    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ACTION_STATUS_PROPERTY)
    private ActionStatus actionStatus;

    @Column(name = AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY)
    private String actionFailureDetails;

    public AuditLogEntity() {
        super();
    }

    public AuditLogEntity(AuditLog auditLog) {
        if (auditLog.getId() != null) {
            this.setUuid(auditLog.getId().getId());
        }
        this.setCreatedTime(auditLog.getCreatedTime());
        if (auditLog.getTenantId() != null) {
            this.tenantId = auditLog.getTenantId().getId();
        }
        if (auditLog.getCustomerId() != null) {
            this.customerId = auditLog.getCustomerId().getId();
        }
        if (auditLog.getEntityId() != null) {
            this.entityId = auditLog.getEntityId().getId();
            this.entityType = auditLog.getEntityId().getEntityType();
        }
        if (auditLog.getUserId() != null) {
            this.userId = auditLog.getUserId().getId();
        }
        this.entityName = auditLog.getEntityName();
        this.userName = auditLog.getUserName();
        this.actionType = auditLog.getActionType();
        this.actionData = auditLog.getActionData();
        this.actionStatus = auditLog.getActionStatus();
        this.actionFailureDetails = auditLog.getActionFailureDetails();
    }

    @Override
    public AuditLog toData() {
        AuditLog auditLog = new AuditLog(new AuditLogId(this.getUuid()));
        auditLog.setCreatedTime(createdTime);
        if (tenantId != null) {
            auditLog.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            auditLog.setCustomerId(new CustomerId(customerId));
        }
        if (entityId != null) {
            auditLog.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        }
        if (userId != null) {
            auditLog.setUserId(new UserId(userId));
        }
        auditLog.setEntityName(this.entityName);
        auditLog.setUserName(this.userName);
        auditLog.setActionType(this.actionType);
        auditLog.setActionData(this.actionData);
        auditLog.setActionStatus(this.actionStatus);
        auditLog.setActionFailureDetails(this.actionFailureDetails);
        return auditLog;
    }
}
