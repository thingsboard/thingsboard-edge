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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.mail.MailTemplates;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.WL_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.WL_WRITE_CHECK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WhiteLabelingController extends BaseController {

    private static final String LOGO_CHECKSUM_DESC = "Logo image checksum. Expects value from the browser cache to compare it with the value from settings. If value matches, the 'logoImageUrl' will be null.";
    private static final String FAVICON_CHECKSUM_DESC = "Favicon image checksum. Expects value from the browser cache to compare it with the value from settings. If value matches, the 'faviconImageUrl' will be null.";

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @ApiOperation(value = "Get White Labeling parameters",
            notes = "Returns white-labeling parameters for the current user.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getWhiteLabelParams(
            @ApiParam(value = LOGO_CHECKSUM_DESC)
            @RequestParam(required = false) String logoImageChecksum,
            @ApiParam(value = FAVICON_CHECKSUM_DESC)
            @RequestParam(required = false) String faviconChecksum) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        WhiteLabelingParams whiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getMergedSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID, logoImageChecksum, faviconChecksum);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(getTenantId(),
                    logoImageChecksum, faviconChecksum);
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(getTenantId(),
                    getCurrentUser().getCustomerId(), logoImageChecksum, faviconChecksum);
        }
        return whiteLabelingParams;
    }

    @ApiOperation(value = "Get Login White Labeling parameters",
            notes = "Returns login white-labeling parameters based on the hostname from request.", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = "/noauth/whiteLabel/loginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getLoginWhiteLabelParams(
            @ApiParam(value = LOGO_CHECKSUM_DESC)
            @RequestParam(required = false) String logoImageChecksum,
            @ApiParam(value = FAVICON_CHECKSUM_DESC)
            @RequestParam(required = false) String faviconChecksum,
            HttpServletRequest request) throws Exception {
        return whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, WhiteLabelingService.EDGE_LOGIN_WHITE_LABEL_DOMAIN_NAME, logoImageChecksum, faviconChecksum);
        // TODO: @voba - on edge domain name hardcoded - using login white labeling of the edge owner and not by domain
        // return whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, request.getServerName(), logoImageChecksum, faviconChecksum);
    }

    @ApiOperation(value = "Get White Labeling configuration (getCurrentWhiteLabelParams)",
            notes = "Fetch the White Labeling configuration that corresponds to the authority of the user. " +
                    "The API call is designed to load the White Labeling configuration for edition. " +
                    "So, the result is NOT merged with the parent level White Labeling configuration. " +
                    "Let's assume there is a custom White Labeling  configured on a system level. " +
                    "And there is no custom White Labeling  items configured on a tenant level. " +
                    "In such a case, the API call will return default object for the tenant administrator. " +
                    WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getCurrentWhiteLabelParams(@ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
                                                              @RequestParam(value = "customerId", required = false) String strCustomerId) throws ThingsboardException, ExecutionException, InterruptedException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.READ);
        WhiteLabelingParams whiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                whiteLabelingParams = whiteLabelingService.getTenantWhiteLabelingParams(getTenantId());
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getTenantId(), customerId);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
        }
        return whiteLabelingParams;
    }

    @ApiOperation(value = "Get Login White Labeling configuration (getCurrentWhiteLabelParams)",
            notes = "Fetch the Login  White Labeling configuration that corresponds to the authority of the user. " +
                    "The API call is designed to load the Login White Labeling configuration for edition. " +
                    "So, the result is NOT merged with the parent level White Labeling configuration. " +
                    "Let's assume there is a custom White Labeling  configured on a system level. " +
                    "And there is no custom White Labeling  items configured on a tenant level. " +
                    "In such a case, the API call will return default object for the tenant administrator. " +
                    WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentLoginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getCurrentLoginWhiteLabelParams(@ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
                                                                        @RequestParam(value = "customerId", required = false) String strCustomerId) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.READ);
        LoginWhiteLabelingParams loginWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            loginWhiteLabelingParams = whiteLabelingService.getSystemLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                loginWhiteLabelingParams = whiteLabelingService.getTenantLoginWhiteLabelingParams(getTenantId());
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(getTenantId(), customerId);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
        }
        return loginWhiteLabelingParams;
    }

    @ApiOperation(value = "Create Or Update White Labeling configuration (saveWhiteLabelParams)",
            notes = "Creates or Updates the White Labeling configuration." +
                    WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams saveWhiteLabelParams(
            @ApiParam(value = "A JSON value representing the white labeling configuration")
            @RequestBody WhiteLabelingParams whiteLabelingParams,
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @RequestParam(value = "customerId", required = false) String strCustomerId) throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        WhiteLabelingParams savedWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            savedWhiteLabelingParams = whiteLabelingService.saveSystemWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                savedWhiteLabelingParams = whiteLabelingService.saveTenantWhiteLabelingParams(getTenantId(), whiteLabelingParams);
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getTenantId(), customerId, whiteLabelingParams);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), whiteLabelingParams);
        }
        return savedWhiteLabelingParams;
    }

    @ApiOperation(value = "Create Or Update Login White Labeling configuration (saveWhiteLabelParams)",
            notes = "Creates or Updates the White Labeling configuration." +
                    WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/loginWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(
            @ApiParam(value = "A JSON value representing the login white labeling configuration")
            @RequestBody LoginWhiteLabelingParams loginWhiteLabelingParams,
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @RequestParam(name = "customerId", required = false) String strCustomerId) throws Exception {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        LoginWhiteLabelingParams savedLoginWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            savedLoginWhiteLabelingParams = whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            if (StringUtils.isEmpty(strCustomerId)) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveTenantLoginWhiteLabelingParams(getTenantId(), loginWhiteLabelingParams);
            } else {
                CustomerId customerId = new CustomerId(toUUID(strCustomerId));
                checkCustomerId(customerId, Operation.READ);
                savedLoginWhiteLabelingParams = whiteLabelingService.saveCustomerLoginWhiteLabelingParams(getTenantId(), customerId, loginWhiteLabelingParams);
            }
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            savedLoginWhiteLabelingParams = whiteLabelingService.saveCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), loginWhiteLabelingParams);
        }
        return savedLoginWhiteLabelingParams;
    }

    @ApiOperation(value = "Preview Login White Labeling configuration (saveWhiteLabelParams)",
            notes = "Merge the White Labeling configuration with the parent configuration and return the result." +
                    WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/previewWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams previewWhiteLabelParams(
            @ApiParam(value = "A JSON value representing the white labeling configuration")
            @RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        checkWhiteLabelingPermissions(Operation.WRITE);
        WhiteLabelingParams mergedWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals(authority)) {
            mergedWhiteLabelingParams = whiteLabelingService.mergeSystemWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals(authority)) {
            mergedWhiteLabelingParams = whiteLabelingService.mergeTenantWhiteLabelingParams(getTenantId(), whiteLabelingParams);
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            mergedWhiteLabelingParams = whiteLabelingService.mergeCustomerWhiteLabelingParams(getTenantId(), whiteLabelingParams);
        }
        return mergedWhiteLabelingParams;
    }

    @ApiOperation(value = "Check White Labeling Allowed",
            notes = "Check if the White Labeling is enabled for the current user owner (tenant or customer)" +
                    WL_WRITE_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/isWhiteLabelingAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isWhiteLabelingAllowed() throws ThingsboardException {
        Authority authority = getCurrentUser().getAuthority();
        EntityId entityId;
        if (Authority.TENANT_ADMIN.equals(authority)) {
            entityId = getCurrentUser().getTenantId();
        } else {
            entityId = getCurrentUser().getCustomerId();
        }
        return whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), entityId);
    }

    @ApiOperation(value = "Check Customer White Labeling Allowed",
            notes = "Check if the White Labeling is enabled for the customers of the current tenant" +
                    WL_WRITE_CHECK + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/whiteLabel/isCustomerWhiteLabelingAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isCustomerWhiteLabelingAllowed() throws ThingsboardException {
        return whiteLabelingService.isCustomerWhiteLabelingAllowed(getTenantId());
    }

    @ApiOperation(value = "Save the Mail templates settings (saveMailTemplates)",
            notes = "Creates or Updates the Mail templates settings." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + WL_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/whiteLabel/mailTemplates", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public JsonNode saveMailTemplates(
            @ApiParam(value = "A JSON value representing the Administration Settings.")
            @RequestBody JsonNode mailTemplates) throws Exception {
        checkWhiteLabelingPermissions(Operation.WRITE);
        return whiteLabelingService.saveMailTemplates(getTenantId(), mailTemplates);
    }

    @ApiOperation(value = "Get the Mail templates settings (getMailTemplates)",
            notes = "Fetch Mail template settings. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH + WL_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/whiteLabel/mailTemplates", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public JsonNode getMailTemplates(@ApiParam(value = "Use system settings if settings are not defined on tenant level.")
                                         @RequestParam(required = false, defaultValue = "false") boolean systemByDefault) throws Exception {
        checkWhiteLabelingPermissions(Operation.READ);
        JsonNode mailTemplates = whiteLabelingService.getCurrentTenantMailTemplates(getTenantId(), systemByDefault);

        ((ObjectNode) mailTemplates).remove(MailTemplates.API_USAGE_STATE_ENABLED);
        ((ObjectNode) mailTemplates).remove(MailTemplates.API_USAGE_STATE_WARNING);
        ((ObjectNode) mailTemplates).remove(MailTemplates.API_USAGE_STATE_DISABLED);

        return mailTemplates;
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}
