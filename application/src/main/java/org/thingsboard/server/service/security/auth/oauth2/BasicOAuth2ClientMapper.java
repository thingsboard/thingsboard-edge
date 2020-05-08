/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.dao.oauth2.OAuth2ClientMapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service(value = "basicOAuth2ClientMapper")
@Slf4j
public class BasicOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {

    private static final String START_PLACEHOLDER_PREFIX = "%{";
    private static final String END_PLACEHOLDER_PREFIX = "}";
    private static final String EMAIL_TENANT_STRATEGY = "email";
    private static final String DOMAIN_TENANT_STRATEGY = "domain";
    private static final String CUSTOM_TENANT_STRATEGY = "custom";

    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(OAuth2AuthenticationToken token, OAuth2ClientMapperConfig config) {
        OAuth2User oauth2User = new OAuth2User();
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        String email = getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
        oauth2User.setEmail(email);
        oauth2User.setTenantName(getTenantName(attributes, config));
        if (!StringUtils.isEmpty(config.getBasic().getLastNameAttributeKey())) {
            String lastName = getStringAttributeByKey(attributes, config.getBasic().getLastNameAttributeKey());
            oauth2User.setLastName(lastName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getFirstNameAttributeKey())) {
            String firstName = getStringAttributeByKey(attributes, config.getBasic().getFirstNameAttributeKey());
            oauth2User.setFirstName(firstName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getCustomerNamePattern())) {
            StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
            String customerName = sub.replace(config.getBasic().getCustomerNamePattern());
            oauth2User.setCustomerName(customerName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getParentCustomerNamePattern())) {
            StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
            String parentCustomerName = sub.replace(config.getBasic().getParentCustomerNamePattern());
            oauth2User.setParentCustomerName(parentCustomerName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getUserGroupsNamePattern())) {
            String[] userGroupNamePatterns = config.getBasic().getUserGroupsNamePattern().split(",");
            List<String> userGroups = new ArrayList<>();
            for (String userGroupNamePattern : userGroupNamePatterns) {
                StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
                String userGroupName = sub.replace(userGroupNamePattern);
                userGroups.add(userGroupName);
            }
            oauth2User.setUserGroups(userGroups);
        }
        return getOrCreateSecurityUserFromOAuth2User(oauth2User, config.isAllowUserCreation(), config.isActivateUser());
    }

    private String getTenantName(Map<String, Object> attributes, OAuth2ClientMapperConfig config) {
        switch (config.getBasic().getTenantNameStrategy()) {
            case EMAIL_TENANT_STRATEGY:
                return getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
            case DOMAIN_TENANT_STRATEGY:
                String email = getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
                return email.substring(email .indexOf("@") + 1);
            case CUSTOM_TENANT_STRATEGY:
                StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
                return sub.replace(config.getBasic().getTenantNamePattern());
            default:
                throw new RuntimeException("Tenant Name Strategy with type " + config.getBasic().getTenantNameStrategy() + " is not supported!");
        }
    }

    private String getStringAttributeByKey(Map<String, Object> attributes, String key) {
        String result = null;
        try {
            result = (String) attributes.get(key);
        } catch (Exception e) {
            log.warn("Can't convert attribute to String by key " + key);
        }
        return result;
    }
}
