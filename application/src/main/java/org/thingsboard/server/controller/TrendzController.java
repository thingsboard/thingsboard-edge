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
package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class TrendzController extends BaseController {

    private final TrendzSettingsService trendzSettingsService;

    @ApiOperation(value = "Save Trendz settings (saveTrendzSettings)",
            notes = "Saves Trendz settings for this tenant.\n" + NEW_LINE +
                    "Here is an example of the Trendz settings:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"enabled\": true,\n" +
                    "  \"baseUrl\": \"https://some.domain.com:18888/also_necessary_prefix\"\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/trendz/settings")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    public TrendzSettings saveTrendzSettings(@RequestBody TrendzSettings trendzSettings,
                                             @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        TenantId tenantId = user.getTenantId();
        trendzSettingsService.saveTrendzSettings(tenantId, trendzSettings);
        return trendzSettings;
    }

    @ApiOperation(value = "Get Trendz Settings (getTrendzSettings)",
            notes = "Retrieves Trendz settings for this tenant." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @GetMapping("/trendz/settings")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    public TrendzSettings getTrendzSettings(@AuthenticationPrincipal SecurityUser user) {
        TenantId tenantId = user.getTenantId();
        return trendzSettingsService.findTrendzSettings(tenantId);
    }

}
