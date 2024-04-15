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
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class OAuth2ParamsInfo {

    @Schema(description = "List of configured domains where OAuth2 platform will redirect a user after successful " +
            "authentication. Cannot be empty. There have to be only one domain with specific name with scheme type 'MIXED'. " +
            "Configured domains with the same name must have different scheme types", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OAuth2DomainInfo> domainInfos;
    @Schema(description = "Mobile applications settings. Application package name must be unique within the list", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OAuth2MobileInfo> mobileInfos;
    @Schema(description = "List of OAuth2 provider settings. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OAuth2RegistrationInfo> clientRegistrations;

}
