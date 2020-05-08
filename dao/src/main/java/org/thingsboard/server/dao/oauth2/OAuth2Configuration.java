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
package org.thingsboard.server.dao.oauth2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "security.oauth2", value = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "security.oauth2")
@Data
@Slf4j
public class OAuth2Configuration {

    private boolean enabled;
    private String loginProcessingUrl;
    private Map<String, OAuth2Client> clients = new HashMap<>();

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> result = new ArrayList<>();
        for (Map.Entry<String, OAuth2Client> entry : clients.entrySet()) {
            OAuth2Client client = entry.getValue();
            ClientRegistration registration = ClientRegistration.withRegistrationId(entry.getKey())
                    .clientId(client.getClientId())
                    .authorizationUri(client.getAuthorizationUri())
                    .clientSecret(client.getClientSecret())
                    .tokenUri(client.getAccessTokenUri())
                    .redirectUriTemplate(client.getRedirectUriTemplate())
                    .scope(client.getScope().split(","))
                    .clientName(client.getClientName())
                    .authorizationGrantType(new AuthorizationGrantType(client.getAuthorizationGrantType()))
                    .userInfoUri(client.getUserInfoUri())
                    .userNameAttributeName(client.getUserNameAttributeName())
                    .jwkSetUri(client.getJwkSetUri())
                    .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getClientAuthenticationMethod()))
                    .build();
            result.add(registration);
        }
        return new InMemoryClientRegistrationRepository(result);
    }

    public OAuth2Client getClientByRegistrationId(String registrationId) {
        OAuth2Client result = null;
        if (clients != null && !clients.isEmpty()) {
            for (String key : clients.keySet()) {
                if (key.equals(registrationId)) {
                    result = clients.get(key);
                    break;
                }
            }
        }
        return result;
    }
}
