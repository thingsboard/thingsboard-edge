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
package org.thingsboard.server.common.data.menu;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@ApiModel
@Data
@EqualsAndHashCode
public class CustomMenuItem {

    @ApiModelProperty(position = 1, value = "Name of the menu item", example = "My Custom Menu", required = true)
    private String name;
    @ApiModelProperty(position = 2, value = "URL of the menu item icon. Overrides 'materialIcon'", example = "My Custom Menu")
    private String iconUrl;
    @ApiModelProperty(position = 3, value = "Material icon name. See [Material Icons](https://fonts.google.com/icons?selected=Material+Icons) for examples", example = "Info")
    private String materialIcon;
    @ApiModelProperty(position = 4, value = "URL to open in the iframe, when user clicks the menu item", example = "https://myexternalurl.com")
    private String iframeUrl;
    @ApiModelProperty(position = 5, value = "Id of the Dashboard to open, when user clicks the menu item", example = "https://mycompany.com")
    private String dashboardId;
    @ApiModelProperty(position = 6, value = "Hide the dashboard toolbar")
    private Boolean hideDashboardToolbar;
    @ApiModelProperty(position = 7, value = "Set the access token of the current user to a new dashboard")
    private boolean setAccessToken;
    @ApiModelProperty(position = 8, value = "List of child menu items")
    private List<CustomMenuItem> childMenuItems = new ArrayList<>();

}
