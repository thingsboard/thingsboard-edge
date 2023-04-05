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
package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.validation.Length;

import javax.validation.Valid;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@ApiModel
public class OAuth2ClientRegistrationTemplate extends SearchTextBasedWithAdditionalInfo<OAuth2ClientRegistrationTemplateId> implements HasName {

    @Length(fieldName = "providerId")
    @ApiModelProperty(value = "OAuth2 provider identifier (e.g. its name)", required = true)
    private String providerId;
    @Valid
    @ApiModelProperty(value = "Default config for mapping OAuth2 log in response to platform entities")
    private OAuth2MapperConfig mapperConfig;
    @Length(fieldName = "authorizationUri")
    @ApiModelProperty(value = "Default authorization URI of the OAuth2 provider")
    private String authorizationUri;
    @Length(fieldName = "accessTokenUri")
    @ApiModelProperty(value = "Default access token URI of the OAuth2 provider")
    private String accessTokenUri;
    @ApiModelProperty(value = "Default OAuth scopes that will be requested from OAuth2 platform")
    private List<String> scope;
    @Length(fieldName = "userInfoUri")
    @ApiModelProperty(value = "Default user info URI of the OAuth2 provider")
    private String userInfoUri;
    @Length(fieldName = "userNameAttributeName")
    @ApiModelProperty(value = "Default name of the username attribute in OAuth2 provider log in response")
    private String userNameAttributeName;
    @Length(fieldName = "jwkSetUri")
    @ApiModelProperty(value = "Default JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    @Length(fieldName = "clientAuthenticationMethod")
    @ApiModelProperty(value = "Default client authentication method to use: 'BASIC' or 'POST'")
    private String clientAuthenticationMethod;
    @ApiModelProperty(value = "Comment for OAuth2 provider")
    private String comment;
    @Length(fieldName = "loginButtonIcon")
    @ApiModelProperty(value = "Default log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    @Length(fieldName = "loginButtonLabel")
    @ApiModelProperty(value = "Default OAuth2 provider label")
    private String loginButtonLabel;
    @Length(fieldName = "helpLink")
    @ApiModelProperty(value = "Help link for OAuth2 provider")
    private String helpLink;

    public OAuth2ClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        super(clientRegistrationTemplate);
        this.providerId = clientRegistrationTemplate.providerId;
        this.mapperConfig = clientRegistrationTemplate.mapperConfig;
        this.authorizationUri = clientRegistrationTemplate.authorizationUri;
        this.accessTokenUri = clientRegistrationTemplate.accessTokenUri;
        this.scope = clientRegistrationTemplate.scope;
        this.userInfoUri = clientRegistrationTemplate.userInfoUri;
        this.userNameAttributeName = clientRegistrationTemplate.userNameAttributeName;
        this.jwkSetUri = clientRegistrationTemplate.jwkSetUri;
        this.clientAuthenticationMethod = clientRegistrationTemplate.clientAuthenticationMethod;
        this.comment = clientRegistrationTemplate.comment;
        this.loginButtonIcon = clientRegistrationTemplate.loginButtonIcon;
        this.loginButtonLabel = clientRegistrationTemplate.loginButtonLabel;
        this.helpLink = clientRegistrationTemplate.helpLink;
    }

    @Override
    public String getName() {
        return providerId;
    }

    @Override
    public String getSearchText() {
        return getName();
    }
}
