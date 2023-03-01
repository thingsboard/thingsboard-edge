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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.servlet.http.HttpServletRequest;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

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
            @RequestParam(required = false) String faviconChecksum) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            WhiteLabelingParams whiteLabelingParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getMergedSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID, logoImageChecksum, faviconChecksum);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(getCurrentUser().getTenantId(),
                        logoImageChecksum, faviconChecksum);
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(getCurrentUser().getTenantId(),
                        getCurrentUser().getCustomerId(), logoImageChecksum, faviconChecksum);
            }
            return whiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
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
            HttpServletRequest request) throws ThingsboardException {
        try {
            return whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, WhiteLabelingService.EDGE_LOGIN_WHITE_LABEL_DOMAIN_NAME, logoImageChecksum, faviconChecksum);
            // TODO: @voba - on edge domain name hardcoded - using login white labeling of the edge owner and not by domain
            // return whiteLabelingService.getMergedLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID, request.getServerName(), logoImageChecksum, faviconChecksum);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get White Labeling configuration (getCurrentWhiteLabelParams)",
            notes = "Fetch the White Labeling configuration that corresponds to the authority of the user. " +
                    "The API call is designed to load the White Labeling configuration for edition. " +
                    "So, the result is NOT merged with the parent level White Labeling configuration. " +
                    "Let's assume there is a custom White Labeling  configured on a system level. " +
                    "And there is no custom White Labeling  items configured on a tenant level. " +
                    "In such a case, the API call will return default object for the tenant administrator. " +
                    ControllerConstants.WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getCurrentWhiteLabelParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.READ);
            WhiteLabelingParams whiteLabelingParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getSystemWhiteLabelingParams(TenantId.SYS_TENANT_ID);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getTenantWhiteLabelingParams(getCurrentUser().getTenantId()).get();
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId()).get();
            }
            return whiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Login White Labeling configuration (getCurrentWhiteLabelParams)",
            notes = "Fetch the Login  White Labeling configuration that corresponds to the authority of the user. " +
                    "The API call is designed to load the Login White Labeling configuration for edition. " +
                    "So, the result is NOT merged with the parent level White Labeling configuration. " +
                    "Let's assume there is a custom White Labeling  configured on a system level. " +
                    "And there is no custom White Labeling  items configured on a tenant level. " +
                    "In such a case, the API call will return default object for the tenant administrator. " +
                    ControllerConstants.WL_READ_CHECK
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentLoginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getCurrentLoginWhiteLabelParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.READ);
            LoginWhiteLabelingParams loginWhiteLabelingParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                loginWhiteLabelingParams = whiteLabelingService.getSystemLoginWhiteLabelingParams(TenantId.SYS_TENANT_ID);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                loginWhiteLabelingParams = whiteLabelingService.getTenantLoginWhiteLabelingParams(getCurrentUser().getTenantId());
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                loginWhiteLabelingParams = whiteLabelingService.getCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId());
            }
            return loginWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update White Labeling configuration (saveWhiteLabelParams)",
            notes = "Creates or Updates the White Labeling configuration." +
                    ControllerConstants.WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams saveWhiteLabelParams(
            @ApiParam(value = "A JSON value representing the white labeling configuration")
            @RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            WhiteLabelingParams savedWhiteLabelingParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                savedWhiteLabelingParams = whiteLabelingService.saveSystemWhiteLabelingParams(whiteLabelingParams);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                savedWhiteLabelingParams = whiteLabelingService.saveTenantWhiteLabelingParams(getCurrentUser().getTenantId(), whiteLabelingParams).get();
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), whiteLabelingParams).get();
            }
            notificationEntityService.notifySendMsgToEdgeService(getCurrentUser().getTenantId(),
                    getCurrentUser().getOwnerId(), EdgeEventType.WHITE_LABELING, EdgeEventActionType.UPDATED);
            return savedWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Login White Labeling configuration (saveWhiteLabelParams)",
            notes = "Creates or Updates the White Labeling configuration." +
                    ControllerConstants.WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/loginWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(
            @ApiParam(value = "A JSON value representing the login white labeling configuration")
            @RequestBody LoginWhiteLabelingParams loginWhiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            LoginWhiteLabelingParams savedLoginWhiteLabelingParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveTenantLoginWhiteLabelingParams(getCurrentUser().getTenantId(), loginWhiteLabelingParams);
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                savedLoginWhiteLabelingParams = whiteLabelingService.saveCustomerLoginWhiteLabelingParams(getTenantId(), getCurrentUser().getCustomerId(), loginWhiteLabelingParams);
            }
            notificationEntityService.notifySendMsgToEdgeService(getCurrentUser().getTenantId(),
                    getCurrentUser().getOwnerId(), EdgeEventType.LOGIN_WHITE_LABELING, EdgeEventActionType.UPDATED);
            return savedLoginWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Preview Login White Labeling configuration (saveWhiteLabelParams)",
            notes = "Merge the White Labeling configuration with the parent configuration and return the result." +
                    ControllerConstants.WL_WRITE_CHECK, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/previewWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams previewWhiteLabelParams(
            @ApiParam(value = "A JSON value representing the white labeling configuration")
            @RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkWhiteLabelingPermissions(Operation.WRITE);
            WhiteLabelingParams mergedWhiteLabelingParams = null;
            if (Authority.SYS_ADMIN.equals(authority)) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeSystemWhiteLabelingParams(whiteLabelingParams);
            } else if (Authority.TENANT_ADMIN.equals(authority)) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeTenantWhiteLabelingParams(getTenantId(), whiteLabelingParams);
            } else if (Authority.CUSTOMER_USER.equals(authority)) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeCustomerWhiteLabelingParams(getCurrentUser().getTenantId(), whiteLabelingParams);
            }
            return mergedWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check White Labeling Allowed",
            notes = "Check if the White Labeling is enabled for the current user owner (tenant or customer)" +
                    ControllerConstants.WL_WRITE_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/isWhiteLabelingAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isWhiteLabelingAllowed() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            EntityId entityId;
            if (Authority.TENANT_ADMIN.equals(authority)) {
                entityId = getCurrentUser().getTenantId();
            } else {
                entityId = getCurrentUser().getCustomerId();
            }
            return whiteLabelingService.isWhiteLabelingAllowed(getTenantId(), entityId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check Customer White Labeling Allowed",
            notes = "Check if the White Labeling is enabled for the customers of the current tenant" +
                    ControllerConstants.WL_WRITE_CHECK + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/whiteLabel/isCustomerWhiteLabelingAllowed", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isCustomerWhiteLabelingAllowed() throws ThingsboardException {
        try {
            return whiteLabelingService.isCustomerWhiteLabelingAllowed(getCurrentUser().getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}
