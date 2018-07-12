/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import org.thingsboard.server.dao.model.type.ActionStatusCodec;
import org.thingsboard.server.dao.model.type.ActionTypeCodec;
import org.thingsboard.server.dao.model.type.EntityTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_DATA_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_BY_ENTITY_ID_CF;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;

@Table(name = AUDIT_LOG_BY_ENTITY_ID_CF)
@Data
@NoArgsConstructor
public class AuditLogEntity implements BaseEntity<AuditLog> {

    @Column(name = ID_PROPERTY)
    private UUID id;

    @Column(name = AUDIT_LOG_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = AUDIT_LOG_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = AUDIT_LOG_ENTITY_TYPE_PROPERTY, codec = EntityTypeCodec.class)
    private EntityType entityType;

    @Column(name = AUDIT_LOG_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = AUDIT_LOG_ENTITY_NAME_PROPERTY)
    private String entityName;

    @Column(name = AUDIT_LOG_USER_ID_PROPERTY)
    private UUID userId;

    @Column(name = AUDIT_LOG_USER_NAME_PROPERTY)
    private String userName;

    @Column(name = AUDIT_LOG_ACTION_TYPE_PROPERTY, codec = ActionTypeCodec.class)
    private ActionType actionType;

    @Column(name = AUDIT_LOG_ACTION_DATA_PROPERTY, codec = JsonCodec.class)
    private JsonNode actionData;

    @Column(name = AUDIT_LOG_ACTION_STATUS_PROPERTY, codec = ActionStatusCodec.class)
    private ActionStatus actionStatus;

    @Column(name = AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY)
    private String actionFailureDetails;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public AuditLogEntity(AuditLog auditLog) {
        if (auditLog.getId() != null) {
            this.id = auditLog.getId().getId();
        }
        if (auditLog.getTenantId() != null) {
            this.tenantId = auditLog.getTenantId().getId();
        }
        if (auditLog.getEntityId() != null) {
            this.entityType = auditLog.getEntityId().getEntityType();
            this.entityId = auditLog.getEntityId().getId();
        }
        if (auditLog.getCustomerId() != null) {
            this.customerId = auditLog.getCustomerId().getId();
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
        AuditLog auditLog = new AuditLog(new AuditLogId(id));
        auditLog.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            auditLog.setTenantId(new TenantId(tenantId));
        }
        if (entityId != null && entityType != null) {
            auditLog.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        }
        if (customerId != null) {
            auditLog.setCustomerId(new CustomerId(customerId));
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
