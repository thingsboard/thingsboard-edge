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
package org.thingsboard.server.common.data.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomMenuInfo extends BaseData<CustomMenuId> implements HasTenantId {

    @Schema(description = "JSON object with Tenant Id that owns the menu.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with Customer Id that owns the menu.", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Custom menu name", example = "Customer A custom menu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = "Custom menu scope. Possible values: SYSTEM, TENANT, CUSTOMER", example = "TENANT", requiredMode = Schema.RequiredMode.REQUIRED)
    private CMScope scope;

    @NotNull
    @Schema(description = "Custom menu assignee type. Possible values are: All (all users of specified scope), " +
            "CUSTOMERS (specified customers), USERS (specified list of users), NO_ASSIGN (no assignees)", example = "ALL",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private CMAssigneeType assigneeType;

    public CustomMenuInfo() {
        super();
    }

    public CustomMenuInfo(CustomMenuId id) {
        super(id);
    }

    public CustomMenuInfo(CustomMenuInfo customMenuInfo) {
        super(customMenuInfo);
        this.tenantId = customMenuInfo.getTenantId();
        this.customerId = customMenuInfo.getCustomerId();
        this.name = customMenuInfo.getName();
        this.scope = customMenuInfo.getScope();
        this.assigneeType = customMenuInfo.getAssigneeType();
    }

}
