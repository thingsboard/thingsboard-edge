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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class CustomTranslationController extends BaseController {

    private static final String CUSTOM_TRANSLATION_EXAMPLE = "\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"translationMap\":{\"es_ES\":\"{\\\"home\\\":\\\"MyHome\\\"}\"}}" +
            MARKDOWN_CODE_BLOCK_END;

    @Autowired
    private CustomTranslationService customTranslationService;

    @ApiOperation(value = "Get end-user Custom Translation configuration (getCustomTranslation)",
            notes = "Fetch the Custom Translation map for the end user. The custom translation is configured in the white labeling parameters. " +
                    "If custom translation translation is defined on the tenant level, it overrides the custom translation of the system level. " +
                    "Similar, if the custom translation is defined on the customer level, it overrides the translation configuration of the tenant level."
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customTranslation/customTranslation", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomTranslation getCustomTranslation() throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        CustomTranslation customTranslation = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            customTranslation = customTranslationService.getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            customTranslation = customTranslationService.getMergedTenantCustomTranslation(getCurrentUser().getTenantId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            customTranslation = customTranslationService.getMergedCustomerCustomTranslation(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
        }
        return customTranslation;
    }

    @ApiOperation(value = "Get Custom Translation configuration (getCurrentCustomTranslation)",
            notes = "Fetch the Custom Translation map that corresponds to the authority of the user. " +
                    "The API call is designed to load the custom translation items for edition. " +
                    "So, the result is NOT merged with the parent level configuration. " +
                    "Let's assume there is a custom translation configured on a system level. " +
                    "And there is no custom translation items configured on a tenant level. " +
                    "In such a case, the API call will return empty object for the tenant administrator. " +
                    "\n\n Response example: " + CUSTOM_TRANSLATION_EXAMPLE +
                    ControllerConstants.WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customTranslation/currentCustomTranslation", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public CustomTranslation getCurrentCustomTranslation() throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.READ);
        CustomTranslation customTranslation = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            customTranslation = customTranslationService.getSystemCustomTranslation(TenantId.SYS_TENANT_ID);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            customTranslation = customTranslationService.getTenantCustomTranslation(getTenantId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            customTranslation = customTranslationService.getCustomerCustomTranslation(getTenantId(), getCurrentUser().getCustomerId());
        }
        return customTranslation;
    }

    @ApiOperation(value = "Create Or Update Custom Translation (saveCustomTranslation)",
            notes = "Creates or Updates the Custom Translation map." +
                    "\n\n Request example: " + CUSTOM_TRANSLATION_EXAMPLE +
                    ControllerConstants.WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customTranslation/customTranslation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public CustomTranslation saveCustomTranslation(
            @ApiParam(value = "A JSON value representing the custom translation. See API call notes above for valid example.")
            @RequestBody CustomTranslation customTranslation) throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        CustomTranslation savedCustomTranslation = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            savedCustomTranslation = customTranslationService.saveSystemCustomTranslation(customTranslation);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            savedCustomTranslation = customTranslationService.saveTenantCustomTranslation(getCurrentUser().getTenantId(), customTranslation);
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            savedCustomTranslation = customTranslationService.saveCustomerCustomTranslation(getTenantId(), getCurrentUser().getCustomerId(), customTranslation);
        }
        notificationEntityService.notifySendMsgToEdgeService(getCurrentUser().getTenantId(),
                getCurrentUser().getOwnerId(), EdgeEventType.CUSTOM_TRANSLATION, EdgeEventActionType.UPDATED);
        return savedCustomTranslation;
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
