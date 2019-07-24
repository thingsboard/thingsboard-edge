/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.dao.selfregistration.SelfRegistrationService;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class SelfRegistrationController extends BaseController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PRIVACY_POLICY = "privacyPolicy";

    @Autowired
    private SelfRegistrationService selfRegistrationService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/selfRegistration/selfRegistrationParams", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public SelfRegistrationParams saveSelfRegistrationParams(@RequestBody SelfRegistrationParams selfRegistrationParams) throws ThingsboardException {
        try {
            Authority authority = getCurrentUser().getAuthority();
            checkSelfRegistrationPermissions(Operation.WRITE);
            SelfRegistrationParams savedSelfRegistrationParams = null;
            if (authority == Authority.TENANT_ADMIN) {
                savedSelfRegistrationParams = selfRegistrationService.saveTenantSelfRegistrationParams(getTenantId(), selfRegistrationParams);
            }
            return savedSelfRegistrationParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/selfRegistration/selfRegistrationParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SelfRegistrationParams getSelfRegistrationParams() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            checkSelfRegistrationPermissions(Operation.READ);
            SelfRegistrationParams selfRegistrationParams = null;
            if (securityUser.getAuthority() == Authority.TENANT_ADMIN) {
                selfRegistrationParams = selfRegistrationService.getTenantSelfRegistrationParams(securityUser.getTenantId());
                JsonNode privacyPolicyNode = MAPPER.readTree(selfRegistrationService.getTenantPrivacyPolicy(securityUser.getTenantId()));
                if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                    selfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
                }
            }
            return selfRegistrationParams;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/selfRegistration/signUpSelfRegistrationParams", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public SignUpSelfRegistrationParams getSignUpSelfRegistrationParams(
            HttpServletRequest request) throws ThingsboardException {
        try {
            SelfRegistrationParams selfRegistrationParams = selfRegistrationService.getSelfRegistrationParams(TenantId.SYS_TENANT_ID,
                    request.getServerName());

            SignUpSelfRegistrationParams result = new SignUpSelfRegistrationParams();
            result.setSignUpTextMessage(selfRegistrationParams.getSignUpTextMessage());
            result.setCaptchaSiteKey(selfRegistrationParams.getCaptchaSiteKey());

            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @RequestMapping(value = "/noauth/selfRegistration/privacyPolicy", method = RequestMethod.GET)
    @ResponseBody
    public String getPrivacyPolicy(HttpServletRequest request) throws ThingsboardException {
        try {
            JsonNode privacyPolicyNode = MAPPER.readTree(selfRegistrationService.getPrivacyPolicy(TenantId.SYS_TENANT_ID,
                    request.getServerName()));
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                return privacyPolicyNode.get(PRIVACY_POLICY).toString();
            }
            return "";
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void checkSelfRegistrationPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

}
