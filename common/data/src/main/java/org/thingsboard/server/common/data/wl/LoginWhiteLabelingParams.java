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
package org.thingsboard.server.common.data.wl;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class LoginWhiteLabelingParams extends WhiteLabelingParams {

    @Schema(description = "Login page background color", example = "#d90f0f")
    private String pageBackgroundColor;
    @Schema(description = "Enable/Disable dark foreground")
    private boolean darkForeground;
    @Schema(description = "Domain name of the login page", example = "iot.mycompany.com")
    private String domainName;
    @Schema(description = "Base URL for the activation link, etc", example = "https://iot.mycompany.com")
    private String baseUrl;
    @Schema(description = "Prohibit use of other URLs. It is recommended to enable this setting", example = "true")
    private boolean prohibitDifferentUrl;
    @Schema(description = "Id of the settings object that store this parameters")
    private String adminSettingsId;
    @Schema(description = "Show platform name and version on login page")
    private Boolean showNameBottom;

    public LoginWhiteLabelingParams merge(LoginWhiteLabelingParams otherWlParams) {
        Integer prevLogoImageHeight = this.logoImageHeight;
        super.merge(otherWlParams);
        if (prevLogoImageHeight == null) {
            this.logoImageHeight = null;
        }
        if (this.showNameBottom == null) {
            this.showNameBottom = otherWlParams.showNameBottom;
        }
        return this;
    }
}
