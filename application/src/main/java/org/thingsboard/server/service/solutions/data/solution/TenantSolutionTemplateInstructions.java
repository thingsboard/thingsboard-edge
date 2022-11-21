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
package org.thingsboard.server.service.solutions.data.solution;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityGroupId;

@ApiModel
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantSolutionTemplateInstructions {

    @ApiModelProperty(position = 1, value = "Id of the group that contains main dashboard of the solution")
    private EntityGroupId dashboardGroupId;
    @ApiModelProperty(position = 2, value = "Id of the main dashboard of the solution")
    private DashboardId dashboardId;
    @ApiModelProperty(position = 3, value = "Id of the public customer if solution has public entities")
    private CustomerId publicId;
    @ApiModelProperty(position = 4, value = "Is the main dashboard public")
    private boolean mainDashboardPublic;
    @ApiModelProperty(position = 5, value = "Markdown with solution usage instructions")
    private String details;

    public TenantSolutionTemplateInstructions(TenantSolutionTemplateInstructions instructions) {
        this.dashboardGroupId = instructions.getDashboardGroupId();
        this.dashboardId = instructions.getDashboardId();
        this.publicId = instructions.getPublicId();
        this.mainDashboardPublic = instructions.isMainDashboardPublic();
        this.details = instructions.getDetails();
    }
}
