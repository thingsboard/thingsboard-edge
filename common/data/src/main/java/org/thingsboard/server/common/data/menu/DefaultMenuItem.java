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

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Views;

import java.util.List;

import static org.thingsboard.server.common.data.menu.MenuItemType.DEFAULT;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DefaultMenuItem implements MenuItem {

    @Schema(description = "Unique identifier for predefined menu items", example = "home", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonView(Views.Public.class)
    private String id;
    @Schema(description = "Name of the menu item", example = "My Custom Menu")
    @JsonView(Views.Public.class)
    private String name;
    @Schema(description = "URL of the menu item icon. Overrides 'materialIcon'", example = "My Custom Menu")
    @JsonView(Views.Public.class)
    private String icon;
    @Schema(description = "Mark if menu item is visible for user")
    @JsonView(Views.Private.class)
    private boolean visible;
    @Schema(description = "List of child menu items")
    @JsonView(Views.Public.class)
    private List<DefaultMenuItem> pages;

    @Override
    @JsonView(Views.Private.class)
    public MenuItemType getType() {
        return DEFAULT;
    }

}
