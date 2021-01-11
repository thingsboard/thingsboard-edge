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
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationInfoId;
import org.thingsboard.server.common.data.oauth2.*;

import java.util.*;

public class OAuth2Utils {
    public static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    public static OAuth2ClientInfo toClientInfo(OAuth2ClientRegistrationInfo clientRegistrationInfo) {
        OAuth2ClientInfo client = new OAuth2ClientInfo();
        client.setName(clientRegistrationInfo.getLoginButtonLabel());
        client.setUrl(String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, clientRegistrationInfo.getUuidId().toString()));
        client.setIcon(clientRegistrationInfo.getLoginButtonIcon());
        return client;
    }

    public static OAuth2ClientsParams toOAuth2Params(List<ExtendedOAuth2ClientRegistrationInfo> extendedOAuth2ClientRegistrationInfos) {
        Map<OAuth2ClientRegistrationInfoId, List<DomainInfo>> domainsByInfoId = new LinkedHashMap<>();
        Map<OAuth2ClientRegistrationInfoId, OAuth2ClientRegistrationInfo> infoById = new LinkedHashMap<>();
        for (ExtendedOAuth2ClientRegistrationInfo extendedClientRegistrationInfo : extendedOAuth2ClientRegistrationInfos) {
            String domainName = extendedClientRegistrationInfo.getDomainName();
            SchemeType domainScheme = extendedClientRegistrationInfo.getDomainScheme();
            domainsByInfoId.computeIfAbsent(extendedClientRegistrationInfo.getId(), key -> new ArrayList<>())
                    .add(new DomainInfo(domainScheme, domainName));
            infoById.put(extendedClientRegistrationInfo.getId(), extendedClientRegistrationInfo);
        }
        Map<List<DomainInfo>, OAuth2ClientsDomainParams> domainParamsMap = new LinkedHashMap<>();
        domainsByInfoId.forEach((clientRegistrationInfoId, domainInfos) -> {
            domainParamsMap.computeIfAbsent(domainInfos,
                    key -> new OAuth2ClientsDomainParams(key, new ArrayList<>())
            )
                    .getClientRegistrations()
                    .add(toClientRegistrationDto(infoById.get(clientRegistrationInfoId)));
        });
        boolean enabled = extendedOAuth2ClientRegistrationInfos.stream()
                .map(OAuth2ClientRegistrationInfo::isEnabled)
                .findFirst().orElse(false);
        return new OAuth2ClientsParams(enabled, new ArrayList<>(domainParamsMap.values()));
    }

    public static ClientRegistrationDto toClientRegistrationDto(OAuth2ClientRegistrationInfo oAuth2ClientRegistrationInfo) {
        return ClientRegistrationDto.builder()
                .mapperConfig(oAuth2ClientRegistrationInfo.getMapperConfig())
                .clientId(oAuth2ClientRegistrationInfo.getClientId())
                .clientSecret(oAuth2ClientRegistrationInfo.getClientSecret())
                .authorizationUri(oAuth2ClientRegistrationInfo.getAuthorizationUri())
                .accessTokenUri(oAuth2ClientRegistrationInfo.getAccessTokenUri())
                .scope(oAuth2ClientRegistrationInfo.getScope())
                .userInfoUri(oAuth2ClientRegistrationInfo.getUserInfoUri())
                .userNameAttributeName(oAuth2ClientRegistrationInfo.getUserNameAttributeName())
                .jwkSetUri(oAuth2ClientRegistrationInfo.getJwkSetUri())
                .clientAuthenticationMethod(oAuth2ClientRegistrationInfo.getClientAuthenticationMethod())
                .loginButtonLabel(oAuth2ClientRegistrationInfo.getLoginButtonLabel())
                .loginButtonIcon(oAuth2ClientRegistrationInfo.getLoginButtonIcon())
                .additionalInfo(oAuth2ClientRegistrationInfo.getAdditionalInfo())
                .build();
    }

    public static OAuth2ClientRegistrationInfo toClientRegistrationInfo(boolean enabled, ClientRegistrationDto clientRegistrationDto) {
        OAuth2ClientRegistrationInfo clientRegistrationInfo = new OAuth2ClientRegistrationInfo();
        clientRegistrationInfo.setEnabled(enabled);
        clientRegistrationInfo.setMapperConfig(clientRegistrationDto.getMapperConfig());
        clientRegistrationInfo.setClientId(clientRegistrationDto.getClientId());
        clientRegistrationInfo.setClientSecret(clientRegistrationDto.getClientSecret());
        clientRegistrationInfo.setAuthorizationUri(clientRegistrationDto.getAuthorizationUri());
        clientRegistrationInfo.setAccessTokenUri(clientRegistrationDto.getAccessTokenUri());
        clientRegistrationInfo.setScope(clientRegistrationDto.getScope());
        clientRegistrationInfo.setUserInfoUri(clientRegistrationDto.getUserInfoUri());
        clientRegistrationInfo.setUserNameAttributeName(clientRegistrationDto.getUserNameAttributeName());
        clientRegistrationInfo.setJwkSetUri(clientRegistrationDto.getJwkSetUri());
        clientRegistrationInfo.setClientAuthenticationMethod(clientRegistrationDto.getClientAuthenticationMethod());
        clientRegistrationInfo.setLoginButtonLabel(clientRegistrationDto.getLoginButtonLabel());
        clientRegistrationInfo.setLoginButtonIcon(clientRegistrationDto.getLoginButtonIcon());
        clientRegistrationInfo.setAdditionalInfo(clientRegistrationDto.getAdditionalInfo());
        return clientRegistrationInfo;
    }

    public static OAuth2ClientRegistration toClientRegistration(OAuth2ClientRegistrationInfoId clientRegistrationInfoId, SchemeType domainScheme, String domainName) {
        OAuth2ClientRegistration clientRegistration = new OAuth2ClientRegistration();
        clientRegistration.setClientRegistrationId(clientRegistrationInfoId);
        clientRegistration.setDomainName(domainName);
        clientRegistration.setDomainScheme(domainScheme);
        return clientRegistration;
    }
}
