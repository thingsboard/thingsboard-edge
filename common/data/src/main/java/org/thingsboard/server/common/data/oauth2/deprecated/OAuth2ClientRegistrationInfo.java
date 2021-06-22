/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.oauth2.deprecated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.deprecated.OAuth2ClientRegistrationInfoId;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;

import java.util.List;

@Deprecated
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
public class OAuth2ClientRegistrationInfo extends SearchTextBasedWithAdditionalInfo<OAuth2ClientRegistrationInfoId> implements HasName {

    private boolean enabled;
    private OAuth2MapperConfig mapperConfig;
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String accessTokenUri;
    private List<String> scope;
    private String userInfoUri;
    private String userNameAttributeName;
    private String jwkSetUri;
    private String clientAuthenticationMethod;
    private String loginButtonLabel;
    private String loginButtonIcon;

    public OAuth2ClientRegistrationInfo(OAuth2ClientRegistrationInfo clientRegistration) {
        super(clientRegistration);
        this.enabled = clientRegistration.enabled;
        this.mapperConfig = clientRegistration.mapperConfig;
        this.clientId = clientRegistration.clientId;
        this.clientSecret = clientRegistration.clientSecret;
        this.authorizationUri = clientRegistration.authorizationUri;
        this.accessTokenUri = clientRegistration.accessTokenUri;
        this.scope = clientRegistration.scope;
        this.userInfoUri = clientRegistration.userInfoUri;
        this.userNameAttributeName = clientRegistration.userNameAttributeName;
        this.jwkSetUri = clientRegistration.jwkSetUri;
        this.clientAuthenticationMethod = clientRegistration.clientAuthenticationMethod;
        this.loginButtonLabel = clientRegistration.loginButtonLabel;
        this.loginButtonIcon = clientRegistration.loginButtonIcon;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return loginButtonLabel;
    }

    @Override
    public String getSearchText() {
        return getName();
    }
}
