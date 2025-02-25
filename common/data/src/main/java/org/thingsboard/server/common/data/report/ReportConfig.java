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
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReportConfig {

    @Schema(description = "Base URL of ThingsBoard UI that should be accessible by Report Server.", example = "https:thingsboard.cloud", requiredMode = Schema.RequiredMode.REQUIRED)
    String baseUrl;
    @Schema(description = "A string value representing the dashboard id.", example = "784f394c-42b6-435a-983c-b7beff2784f9", requiredMode = Schema.RequiredMode.REQUIRED)
    String dashboardId;
    @Schema(description = "Target dashboard state for report generation.")
    String state;
    @Schema(description = "Timezone in which target dashboard will be presented in report.", example = "Europe/Kiev", requiredMode = Schema.RequiredMode.REQUIRED)
    String timezone;
    @Schema(description = "If set, timewindow configured in the target dashboard will be used during report generation.", example = "true")
    boolean useDashboardTimewindow;
    @Schema(description = "Specific dashboard timewindow that will be used during report generation.")
    JsonNode timewindow;
    @Schema(description = "If set, timewindow configured in the target dashboard will be used during report generation.", example = "report-%d{yyyy-MM-dd_HH:mm:ss}", requiredMode = Schema.RequiredMode.REQUIRED)
    String namePattern;
    @Schema(description = "Report file type, can be PDF | PNG | JPEG.", example = "pdf")
    String type;
    @Schema(description = "If set, credentials of user created this report configuration will be used to open dashboard UI during report generation.", example = "true")
    boolean useCurrentUserCredentials;
    @Schema(description = "A string value representing the user id.", example = "784f394c-42b6-435a-983c-b7beff2784f9", requiredMode = Schema.RequiredMode.REQUIRED)
    String userId;

}
