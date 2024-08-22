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
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.common.data.menu.MenuItemType.CUSTOM;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class CustomMenuItem implements MenuItem {

    @Schema(description = "Name of the menu item", example = "My Custom Menu", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;
    @Schema(description = "URL of the menu item icon. Overrides 'materialIcon'", example = "My Custom Menu")
    @NotNull
    private String icon;
    @Schema(description = "Type of menu item (LINK or SECTION). LINK type means item has no child items, SECTION type should have at least one child", example = "LINK")
    @NotNull
    private CMSectionType sectionType;
    @Schema(description = "Type of menu item. LINK type means item without child items, SECTION type should have at least one child", example = "LINK")
    private CMItemLinkType linkType;
    @Schema(description = "Id of the Dashboard to open, when user clicks the menu item", example = "https://mycompany.com")
    private String dashboardId;
    @Schema(description = "Hide the dashboard toolbar")
    private Boolean hideDashboardToolbar;
    @Schema(description = "URL to open in the iframe, when user clicks the menu item", example = "https://myexternalurl.com")
    private String url;
    @Schema(description = "Set the access token of the current user to a new dashboard")
    private boolean setAccessToken;
    @Schema(description = "Mark if menu item is visible for user")
    private boolean visible;
    @Schema(description = "List of child menu items")
    private List<CustomMenuItem> pages = new ArrayList<>();

    @Override
    public MenuItemType getType() {
        return CUSTOM;
    }

}
