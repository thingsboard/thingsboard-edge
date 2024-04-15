/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema
public class OAuth2RegistrationInfo {
    @Schema(description = "Config for mapping OAuth2 log in response to platform entities", requiredMode = Schema.RequiredMode.REQUIRED)
    private OAuth2MapperConfig mapperConfig;
    @Schema(description = "OAuth2 client ID. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientId;
    @Schema(description = "OAuth2 client secret. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientSecret;
    @Schema(description = "Authorization URI of the OAuth2 provider. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authorizationUri;
    @Schema(description = "Access token URI of the OAuth2 provider. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessTokenUri;
    @Schema(description = "OAuth scopes that will be requested from OAuth2 platform. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> scope;
    @Schema(description = "User info URI of the OAuth2 provider")
    private String userInfoUri;
    @Schema(description = "Name of the username attribute in OAuth2 provider response. Cannot be empty")
    private String userNameAttributeName;
    @Schema(description = "JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    @Schema(description = "Client authentication method to use: 'BASIC' or 'POST'. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientAuthenticationMethod;
    @Schema(description = "OAuth2 provider label. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private String loginButtonLabel;
    @Schema(description = "Log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    @Schema(description = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    private List<PlatformType> platforms;
    @Schema(description = "Additional info of OAuth2 client (e.g. providerName)", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode additionalInfo;
}
