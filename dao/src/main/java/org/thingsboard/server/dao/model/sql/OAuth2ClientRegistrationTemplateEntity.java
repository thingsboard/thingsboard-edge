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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.TenantNameStrategyType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_TEMPLATE_TABLE_NAME)
public class OAuth2ClientRegistrationTemplateEntity extends BaseSqlEntity<OAuth2ClientRegistrationTemplate> {

    @Column(name = ModelConstants.OAUTH2_TEMPLATE_PROVIDER_ID_PROPERTY)
    private String providerId;
    @Column(name = ModelConstants.OAUTH2_AUTHORIZATION_URI_PROPERTY)
    private String authorizationUri;
    @Column(name = ModelConstants.OAUTH2_TOKEN_URI_PROPERTY)
    private String tokenUri;
    @Column(name = ModelConstants.OAUTH2_SCOPE_PROPERTY)
    private String scope;
    @Column(name = ModelConstants.OAUTH2_USER_INFO_URI_PROPERTY)
    private String userInfoUri;
    @Column(name = ModelConstants.OAUTH2_USER_NAME_ATTRIBUTE_NAME_PROPERTY)
    private String userNameAttributeName;
    @Column(name = ModelConstants.OAUTH2_JWK_SET_URI_PROPERTY)
    private String jwkSetUri;
    @Column(name = ModelConstants.OAUTH2_CLIENT_AUTHENTICATION_METHOD_PROPERTY)
    private String clientAuthenticationMethod;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_MAPPER_TYPE_PROPERTY)
    private MapperType type;
    @Column(name = ModelConstants.OAUTH2_EMAIL_ATTRIBUTE_KEY_PROPERTY)
    private String emailAttributeKey;
    @Column(name = ModelConstants.OAUTH2_FIRST_NAME_ATTRIBUTE_KEY_PROPERTY)
    private String firstNameAttributeKey;
    @Column(name = ModelConstants.OAUTH2_LAST_NAME_ATTRIBUTE_KEY_PROPERTY)
    private String lastNameAttributeKey;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_STRATEGY_PROPERTY)
    private TenantNameStrategyType tenantNameStrategy;
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_PATTERN_PROPERTY)
    private String tenantNamePattern;
    @Column(name = ModelConstants.OAUTH2_CUSTOMER_NAME_PATTERN_PROPERTY)
    private String customerNamePattern;
    @Column(name = ModelConstants.OAUTH2_DEFAULT_DASHBOARD_NAME_PROPERTY)
    private String defaultDashboardName;
    @Column(name = ModelConstants.OAUTH2_ALWAYS_FULL_SCREEN_PROPERTY)
    private Boolean alwaysFullScreen;
    @Column(name = ModelConstants.OAUTH2_PARENT_CUSTOMER_NAME_PATTERN_PROPERTY)
    private String parentCustomerNamePattern;
    @Column(name = ModelConstants.OAUTH2_USER_GROUPS_NAME_PATTERN_PROPERTY)
    private String userGroupsNamePattern;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_COMMENT_PROPERTY)
    private String comment;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_LOGIN_BUTTON_ICON_PROPERTY)
    private String loginButtonIcon;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_LOGIN_BUTTON_LABEL_PROPERTY)
    private String loginButtonLabel;
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_HELP_LINK_PROPERTY)
    private String helpLink;

    @Type(type = "json")
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public OAuth2ClientRegistrationTemplateEntity() {
    }

    public OAuth2ClientRegistrationTemplateEntity(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        if (clientRegistrationTemplate.getId() != null) {
            this.setUuid(clientRegistrationTemplate.getId().getId());
        }
        this.createdTime = clientRegistrationTemplate.getCreatedTime();
        this.providerId = clientRegistrationTemplate.getProviderId();
        this.authorizationUri = clientRegistrationTemplate.getAuthorizationUri();
        this.tokenUri = clientRegistrationTemplate.getAccessTokenUri();
        this.scope = clientRegistrationTemplate.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.userInfoUri = clientRegistrationTemplate.getUserInfoUri();
        this.userNameAttributeName = clientRegistrationTemplate.getUserNameAttributeName();
        this.jwkSetUri = clientRegistrationTemplate.getJwkSetUri();
        this.clientAuthenticationMethod = clientRegistrationTemplate.getClientAuthenticationMethod();
        this.comment = clientRegistrationTemplate.getComment();
        this.loginButtonIcon = clientRegistrationTemplate.getLoginButtonIcon();
        this.loginButtonLabel = clientRegistrationTemplate.getLoginButtonLabel();
        this.helpLink = clientRegistrationTemplate.getHelpLink();
        this.additionalInfo = clientRegistrationTemplate.getAdditionalInfo();
        OAuth2MapperConfig mapperConfig = clientRegistrationTemplate.getMapperConfig();
        if (mapperConfig != null){
            this.type = mapperConfig.getType();
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
            if (basicConfig != null) {
                this.emailAttributeKey = basicConfig.getEmailAttributeKey();
                this.firstNameAttributeKey = basicConfig.getFirstNameAttributeKey();
                this.lastNameAttributeKey = basicConfig.getLastNameAttributeKey();
                this.tenantNameStrategy = basicConfig.getTenantNameStrategy();
                this.tenantNamePattern = basicConfig.getTenantNamePattern();
                this.customerNamePattern = basicConfig.getCustomerNamePattern();
                this.defaultDashboardName = basicConfig.getDefaultDashboardName();
                this.alwaysFullScreen = basicConfig.isAlwaysFullScreen();
                this.parentCustomerNamePattern = basicConfig.getParentCustomerNamePattern();
                if (basicConfig.getUserGroupsNamePattern() != null && !basicConfig.getUserGroupsNamePattern().isEmpty()) {
                    this.userGroupsNamePattern = basicConfig.getUserGroupsNamePattern().stream()
                            .reduce((result, element) -> result + "," + element).orElse(null);
                }
            }
        }
    }

    @Override
    public OAuth2ClientRegistrationTemplate toData() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = new OAuth2ClientRegistrationTemplate();
        clientRegistrationTemplate.setId(new OAuth2ClientRegistrationTemplateId(id));
        clientRegistrationTemplate.setCreatedTime(createdTime);
        clientRegistrationTemplate.setAdditionalInfo(additionalInfo);

        clientRegistrationTemplate.setProviderId(providerId);
        clientRegistrationTemplate.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .type(type)
                        .basic(OAuth2BasicMapperConfig.builder()
                                .emailAttributeKey(emailAttributeKey)
                                .firstNameAttributeKey(firstNameAttributeKey)
                                .lastNameAttributeKey(lastNameAttributeKey)
                                .tenantNameStrategy(tenantNameStrategy)
                                .tenantNamePattern(tenantNamePattern)
                                .customerNamePattern(customerNamePattern)
                                .defaultDashboardName(defaultDashboardName)
                                .alwaysFullScreen(alwaysFullScreen)
                                .parentCustomerNamePattern(parentCustomerNamePattern)
                                .userGroupsNamePattern(userGroupsNamePattern != null ? Arrays.asList(userGroupsNamePattern.split(",")) : Collections.emptyList())
                                .build()
                        )
                        .build()
        );
        clientRegistrationTemplate.setAuthorizationUri(authorizationUri);
        clientRegistrationTemplate.setAccessTokenUri(tokenUri);
        clientRegistrationTemplate.setScope(Arrays.asList(scope.split(",")));
        clientRegistrationTemplate.setUserInfoUri(userInfoUri);
        clientRegistrationTemplate.setUserNameAttributeName(userNameAttributeName);
        clientRegistrationTemplate.setJwkSetUri(jwkSetUri);
        clientRegistrationTemplate.setClientAuthenticationMethod(clientAuthenticationMethod);
        clientRegistrationTemplate.setComment(comment);
        clientRegistrationTemplate.setLoginButtonIcon(loginButtonIcon);
        clientRegistrationTemplate.setLoginButtonLabel(loginButtonLabel);
        clientRegistrationTemplate.setHelpLink(helpLink);
        return clientRegistrationTemplate;
    }
}
