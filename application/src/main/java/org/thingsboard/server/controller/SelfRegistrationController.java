/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;

@RestController
@TbCoreComponent
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
            SecurityUser securityUser = getCurrentUser();
            Authority authority = securityUser.getAuthority();
            checkSelfRegistrationPermissions(Operation.WRITE);
            SelfRegistrationParams savedSelfRegistrationParams = null;
            if (Authority.TENANT_ADMIN.equals(authority)) {
                savedSelfRegistrationParams = selfRegistrationService.saveTenantSelfRegistrationParams(getTenantId(), selfRegistrationParams);
                JsonNode privacyPolicyNode = MAPPER.readTree(selfRegistrationService.getTenantPrivacyPolicy(securityUser.getTenantId()));
                if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                    savedSelfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
                }
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
            if (Authority.TENANT_ADMIN.equals(securityUser.getAuthority())) {
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
