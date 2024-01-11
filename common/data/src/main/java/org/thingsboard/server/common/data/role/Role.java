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
package org.thingsboard.server.common.data.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseDataWithAdditionalInfo<RoleId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId, ExportableEntity<RoleId> {

    private static final long serialVersionUID = 5582010124562018986L;

    public static final String ROLE_TENANT_ADMIN_NAME = "Tenant Administrator";
    public static final String ROLE_TENANT_USER_NAME = "Tenant User";
    public static final String ROLE_CUSTOMER_ADMIN_NAME = "Customer Administrator";
    public static final String ROLE_CUSTOMER_USER_NAME = "Customer User";
    public static final String ROLE_PUBLIC_USER_NAME = "Public User";
    public static final String ROLE_PUBLIC_USER_ENTITY_GROUP_NAME = "Entity Group Public User";
    public static final String ROLE_READ_ONLY_ENTITY_GROUP_NAME = "Entity Group Read-only User";
    public static final String ROLE_WRITE_ENTITY_GROUP_NAME = "Entity Group Write User";

    @Schema(required = true, description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id. ", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    @Schema(required = true, description = "Role Name", example = "Read-Only")
    private String name;
    @Schema(required = true, description = "Type of the role: generic or group", example = "GROUP")
    private RoleType type;
    @Schema(description = "JSON object with the set of permissions. Structure is specific for role type", accessMode = Schema.AccessMode.READ_ONLY)
    private transient JsonNode permissions;
    @JsonIgnore
    private byte[] permissionsBytes;

    private RoleId externalId;

    public Role() {
        super();
    }

    public Role(RoleId id) {
        super(id);
    }

    public Role(Role role) {
        super(role);
        setPermissions(role.getPermissions());
        externalId = role.getExternalId();
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

    public JsonNode getPermissions() {
        return getJson(() -> permissions, () -> permissionsBytes);
    }

    public void setPermissions(JsonNode permissions) {
        setJson(permissions, json -> this.permissions = json, bytes -> this.permissionsBytes = bytes);
    }

    @Schema(description = "JSON object with the Role Id. " +
            "Specify this field to update the Role. " +
            "Referencing non-existing Role Id will cause error. " +
            "Omit this field to create new Role." )
    @Override
    public RoleId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the role creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Additional parameters of the role", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}
