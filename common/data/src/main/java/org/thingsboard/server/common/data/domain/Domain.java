/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class Domain extends BaseData<DomainId> implements HasName, TenantEntity, HasOwnerId {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id")
    private CustomerId customerId;
    @Schema(description = "Domain name. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "name")
    private String name;
    @Schema(description = "Whether OAuth2 settings are enabled or not")
    private boolean oauth2Enabled;
    @Schema(description = "Whether OAuth2 settings are enabled on Edge or not")
    private boolean propagateToEdge;

    public Domain() {
        super();
    }

    public Domain(DomainId id) {
        super(id);
    }

    public Domain(Domain domain) {
        super(domain);
        this.tenantId = domain.tenantId;
        this.customerId = domain.customerId;
        this.name = domain.name;
        this.oauth2Enabled = domain.oauth2Enabled;
        this.propagateToEdge = domain.propagateToEdge;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOMAIN;
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
}
