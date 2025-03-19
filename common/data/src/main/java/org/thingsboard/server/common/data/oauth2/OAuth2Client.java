/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
public class OAuth2Client extends BaseDataWithAdditionalInfo<OAuth2ClientId> implements HasName, TenantEntity, HasOwnerId {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id")
    private CustomerId customerId;
    @Schema(description = "Oauth2 client title")
    @NotBlank
    @Length(fieldName = "title", max = 100, message = "cannot be longer than 100 chars")
    private String title;
    @Schema(description = "Config for mapping OAuth2 log in response to platform entities", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private OAuth2MapperConfig mapperConfig;
    @Schema(description = "OAuth2 client ID. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "clientId")
    private String clientId;
    @Schema(description = "OAuth2 client secret. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "clientSecret", max = 2048)
    private String clientSecret;
    @Schema(description = "Authorization URI of the OAuth2 provider. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "authorizationUri")
    private String authorizationUri;
    @Schema(description = "Access token URI of the OAuth2 provider. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "accessTokenUri")
    private String accessTokenUri;
    @Schema(description = "OAuth scopes that will be requested from OAuth2 platform. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Length(fieldName = "scope")
    private List<String> scope;
    @Schema(description = "User info URI of the OAuth2 provider")
    @Length(fieldName = "userInfoUri")
    private String userInfoUri;
    @Schema(description = "Name of the username attribute in OAuth2 provider response. Cannot be empty")
    @NotBlank
    @Length(fieldName = "userNameAttributeName")
    private String userNameAttributeName;
    @Schema(description = "JSON Web Key URI of the OAuth2 provider")
    @Length(fieldName = "jwkSetUri")
    private String jwkSetUri;
    @Schema(description = "Client authentication method to use: 'BASIC' or 'POST'. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "clientAuthenticationMethod")
    private String clientAuthenticationMethod;
    @Schema(description = "OAuth2 provider label. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "loginButtonLabel")
    private String loginButtonLabel;
    @Schema(description = "Log in button icon for OAuth2 provider")
    @Length(fieldName = "loginButtonIcon")
    private String loginButtonIcon;
    @Schema(description = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    @Length(fieldName = "platforms")
    private List<PlatformType> platforms;
    @Schema(description = "Additional info of OAuth2 client (e.g. providerName)", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode additionalInfo;

    public OAuth2Client() {
        super();
    }

    public OAuth2Client(OAuth2ClientId id) {
        super(id);
    }

    public OAuth2Client(OAuth2Client oAuth2Client) {
        super(oAuth2Client);
        this.tenantId = oAuth2Client.tenantId;
        this.customerId = oAuth2Client.customerId;
        this.title = oAuth2Client.title;
        this.mapperConfig = oAuth2Client.mapperConfig;
        this.clientId = oAuth2Client.clientId;
        this.clientSecret = oAuth2Client.clientSecret;
        this.authorizationUri = oAuth2Client.authorizationUri;
        this.accessTokenUri = oAuth2Client.accessTokenUri;
        this.scope = oAuth2Client.scope;
        this.userInfoUri = oAuth2Client.userInfoUri;
        this.userNameAttributeName = oAuth2Client.userNameAttributeName;
        this.jwkSetUri = oAuth2Client.jwkSetUri;
        this.clientAuthenticationMethod = oAuth2Client.clientAuthenticationMethod;
        this.loginButtonLabel = oAuth2Client.loginButtonLabel;
        this.loginButtonIcon = oAuth2Client.loginButtonIcon;
        this.platforms = oAuth2Client.platforms;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }

    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }
}
