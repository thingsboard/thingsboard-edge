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
package org.thingsboard.server.common.data.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.CustomMenuId;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomMenuInfo extends BaseData<CustomMenuId> {

    @Serial
    private static final long serialVersionUID = -628571812163942630L;
    @Schema(description = "Custom menu title (e.g. Customer A menu)")
    private String name;

    @Schema(description = "User level custom menu is applied to", example = "TENANT")
    private CMScope scope;

    @Schema(description = "Custom menu could be applied to whole tenant/customer or separete list of users. " +
            "So possible values are: All (means all users of specified scope), Tenant (specified tenants), Customer (specified customers)," +
            " User list (specified list of users)", example = "ALL")
    private CMAssigneeType assigneeType;

    @Valid
    @Schema(description = "Assignee list", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> assigneeList;

    public CustomMenuInfo() {
        super();
    }

    public CustomMenuInfo(CustomMenu customMenu) {
        this(customMenu, Collections.emptyList());
    }

    public CustomMenuInfo(CustomMenu customMenu, List<EntityInfo> assigneeList) {
        super(customMenu);
        this.name = customMenu.getName();
        this.scope = customMenu.getScope();
        this.assigneeType = customMenu.getAssigneeType();
        this.assigneeList = assigneeList;
    }
}
