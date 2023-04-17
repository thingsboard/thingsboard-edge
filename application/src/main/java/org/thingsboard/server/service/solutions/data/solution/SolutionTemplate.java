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
package org.thingsboard.server.service.solutions.data.solution;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@ApiModel
@Data
@NoArgsConstructor
public class SolutionTemplate {

    @ApiModelProperty(position = 1, value = "ID of the solution template", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String id;
    @ApiModelProperty(position = 2, value = "Template Title", example = "Smart office")
    private String title;
    @ApiModelProperty(position = 3, value = "Level of the subscription that is required to unlock the template", example = "PROTOTYPE")
    private SolutionTemplateLevel level;
    @ApiModelProperty(position = 4, value = "Timeout for the installation UI to wait while template is installing")
    private long installTimeoutMs;
    @ApiModelProperty(position = 5, value = "What keys to delete during template uninstall")
    private List<String> tenantTelemetryKeys;
    @ApiModelProperty(position = 6, value = "What attributes to delete during template uninstall")
    private List<String> tenantAttributeKeys;

    public SolutionTemplate(SolutionTemplate solution) {
        this.id = solution.getId();
        this.title = solution.getTitle();
        this.level = solution.getLevel();
        this.installTimeoutMs = solution.getInstallTimeoutMs();
        this.tenantTelemetryKeys = solution.getTenantTelemetryKeys();
        this.tenantAttributeKeys = solution.getTenantAttributeKeys();
    }
}
