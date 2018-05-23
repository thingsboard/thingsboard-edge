/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.common.data.exception.ThingsboardException;

@RestController
@RequestMapping("/api")
public class WhiteLabelingController extends BaseController {

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getWhiteLabelParams(
            @RequestParam(required = false) String logoImageChecksum,
            @RequestParam(required = false) String faviconChecksum) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            WhiteLabelingParams whiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getMergedSystemWhiteLabelingParams(logoImageChecksum, faviconChecksum);
            } else if (authority == Authority.TENANT_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getMergedTenantWhiteLabelingParams(getCurrentUser().getTenantId(),
                        logoImageChecksum, faviconChecksum);
            } else if (authority == Authority.CUSTOMER_USER) {
                whiteLabelingParams = whiteLabelingService.getMergedCustomerWhiteLabelingParams(getCurrentUser().getTenantId(),
                        getCurrentUser().getCustomerId(), logoImageChecksum, faviconChecksum);
            }
            return whiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/whiteLabel/loginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getLoginWhiteLabelParams(
            @RequestParam(required = false) String logoImageChecksum,
            @RequestParam(required = false) String faviconChecksum) throws ThingsboardException {
        try {
            return whiteLabelingService.getMergedLoginWhiteLabelingParams(logoImageChecksum, faviconChecksum);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/currentWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public WhiteLabelingParams getCurrentWhiteLabelParams() throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            WhiteLabelingParams whiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getSystemWhiteLabelingParams();
            } else if (authority == Authority.TENANT_ADMIN) {
                whiteLabelingParams = whiteLabelingService.getTenantWhiteLabelingParams(getCurrentUser().getTenantId());
            } else if (authority == Authority.CUSTOMER_USER) {
                whiteLabelingParams = whiteLabelingService.getCustomerWhiteLabelingParams(getCurrentUser().getCustomerId());
            }
            return whiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/whiteLabel/currentLoginWhiteLabelParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public LoginWhiteLabelingParams getCurrentLoginWhiteLabelParams() throws ThingsboardException {
        try {
            return whiteLabelingService.getLoginWhiteLabelingParams();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/whiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams saveWhiteLabelParams(@RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            WhiteLabelingParams savedWhiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                savedWhiteLabelingParams = whiteLabelingService.saveSystemWhiteLabelingParams(whiteLabelingParams);
            } else if (authority == Authority.TENANT_ADMIN) {
                savedWhiteLabelingParams = whiteLabelingService.saveTenantWhiteLabelingParams(getCurrentUser().getTenantId(), whiteLabelingParams);
            } else if (authority == Authority.CUSTOMER_USER) {
                savedWhiteLabelingParams = whiteLabelingService.saveCustomerWhiteLabelingParams(getCurrentUser().getCustomerId(), whiteLabelingParams);
            }
            return savedWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/whiteLabel/loginWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(@RequestBody LoginWhiteLabelingParams loginWhiteLabelingParams) throws ThingsboardException {
        try {
            return whiteLabelingService.saveLoginWhiteLabelingParams(loginWhiteLabelingParams);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/whiteLabel/previewWhiteLabelParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public WhiteLabelingParams previewWhiteLabelParams(@RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            WhiteLabelingParams mergedWhiteLabelingParams = null;
            if (authority == Authority.SYS_ADMIN) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeSystemWhiteLabelingParams(whiteLabelingParams);
            } else if (authority == Authority.TENANT_ADMIN) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeTenantWhiteLabelingParams(whiteLabelingParams);
            } else if (authority == Authority.CUSTOMER_USER) {
                mergedWhiteLabelingParams = whiteLabelingService.mergeCustomerWhiteLabelingParams(getCurrentUser().getTenantId(), whiteLabelingParams);
            }
            return mergedWhiteLabelingParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
