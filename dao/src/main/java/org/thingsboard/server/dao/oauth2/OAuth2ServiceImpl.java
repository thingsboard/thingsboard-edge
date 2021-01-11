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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.*;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service
public class OAuth2ServiceImpl extends AbstractEntityService implements OAuth2Service {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CLIENT_REGISTRATION_ID = "Incorrect clientRegistrationId ";
    public static final String INCORRECT_DOMAIN_NAME = "Incorrect domainName ";
    public static final String INCORRECT_DOMAIN_SCHEME = "Incorrect domainScheme ";

    @Autowired
    private OAuth2ClientRegistrationInfoDao clientRegistrationInfoDao;
    @Autowired
    private OAuth2ClientRegistrationDao clientRegistrationDao;

    @Override
    public List<OAuth2ClientInfo> getOAuth2Clients(String domainSchemeStr, String domainName) {
        log.trace("Executing getOAuth2Clients [{}://{}]", domainSchemeStr, domainName);
        if (domainSchemeStr == null) {
            throw new IncorrectParameterException(INCORRECT_DOMAIN_SCHEME);
        }
        SchemeType domainScheme;
        try {
            domainScheme = SchemeType.valueOf(domainSchemeStr.toUpperCase());
        } catch (IllegalArgumentException e){
            throw new IncorrectParameterException(INCORRECT_DOMAIN_SCHEME);
        }
        validateString(domainName, INCORRECT_DOMAIN_NAME + domainName);
        return clientRegistrationInfoDao.findByDomainSchemesAndDomainName(Arrays.asList(domainScheme, SchemeType.MIXED), domainName).stream()
                .filter(OAuth2ClientRegistrationInfo::isEnabled)
                .map(OAuth2Utils::toClientInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveOAuth2Params(OAuth2ClientsParams oauth2Params) {
        log.trace("Executing saveOAuth2Params [{}]", oauth2Params);
        clientParamsValidator.accept(oauth2Params);
        clientRegistrationDao.deleteAll();
        clientRegistrationInfoDao.deleteAll();
        oauth2Params.getDomainsParams().forEach(domainParams -> {
            domainParams.getClientRegistrations().forEach(clientRegistrationDto -> {
                OAuth2ClientRegistrationInfo oAuth2ClientRegistrationInfo = OAuth2Utils.toClientRegistrationInfo(oauth2Params.isEnabled(), clientRegistrationDto);
                OAuth2ClientRegistrationInfo savedClientRegistrationInfo = clientRegistrationInfoDao.save(TenantId.SYS_TENANT_ID, oAuth2ClientRegistrationInfo);
                domainParams.getDomainInfos().forEach(domainInfo -> {
                    OAuth2ClientRegistration oAuth2ClientRegistration = OAuth2Utils.toClientRegistration(savedClientRegistrationInfo.getId(),
                            domainInfo.getScheme(), domainInfo.getName());
                    clientRegistrationDao.save(TenantId.SYS_TENANT_ID, oAuth2ClientRegistration);
                });
            });
        });
    }

    @Override
    public OAuth2ClientsParams findOAuth2Params() {
        log.trace("Executing findOAuth2Params");
        List<ExtendedOAuth2ClientRegistrationInfo> extendedInfos = clientRegistrationInfoDao.findAllExtended();
        return OAuth2Utils.toOAuth2Params(extendedInfos);
    }

    @Override
    public OAuth2ClientRegistrationInfo findClientRegistrationInfo(UUID id) {
        log.trace("Executing findClientRegistrationInfo [{}]", id);
        validateId(id, INCORRECT_CLIENT_REGISTRATION_ID + id);
        return clientRegistrationInfoDao.findById(null, id);
    }

    @Override
    public List<OAuth2ClientRegistrationInfo> findAllClientRegistrationInfos() {
        log.trace("Executing findAllClientRegistrationInfos");
        return clientRegistrationInfoDao.findAll();
    }

    private final Consumer<OAuth2ClientsParams> clientParamsValidator = oauth2Params -> {
        if (oauth2Params == null
                || oauth2Params.getDomainsParams() == null) {
            throw new DataValidationException("Domain params should be specified!");
        }
        for (OAuth2ClientsDomainParams domainParams : oauth2Params.getDomainsParams()) {
            if (domainParams.getDomainInfos() == null
                    || domainParams.getDomainInfos().isEmpty()) {
                throw new DataValidationException("List of domain configuration should be specified!");
            }
            for (DomainInfo domainInfo : domainParams.getDomainInfos()) {
                if (StringUtils.isEmpty(domainInfo.getName())) {
                    throw new DataValidationException("Domain name should be specified!");
                }
                if (domainInfo.getScheme() == null) {
                    throw new DataValidationException("Domain scheme should be specified!");
                }
            }
            domainParams.getDomainInfos().stream()
                    .collect(Collectors.groupingBy(DomainInfo::getName))
                    .forEach((domainName, domainInfos) -> {
                        if (domainInfos.size() > 1 && domainInfos.stream().anyMatch(domainInfo -> domainInfo.getScheme() == SchemeType.MIXED)) {
                            throw new DataValidationException("MIXED scheme type shouldn't be combined with another scheme type!");
                        }
                    });
            if (domainParams.getClientRegistrations() == null || domainParams.getClientRegistrations().isEmpty()) {
                throw new DataValidationException("Client registrations should be specified!");
            }
            for (ClientRegistrationDto clientRegistration : domainParams.getClientRegistrations()) {
                if (StringUtils.isEmpty(clientRegistration.getClientId())) {
                    throw new DataValidationException("Client ID should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientSecret())) {
                    throw new DataValidationException("Client secret should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAuthorizationUri())) {
                    throw new DataValidationException("Authorization uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getAccessTokenUri())) {
                    throw new DataValidationException("Token uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getScope())) {
                    throw new DataValidationException("Scope should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getUserInfoUri())) {
                    throw new DataValidationException("User info uri should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getUserNameAttributeName())) {
                    throw new DataValidationException("User name attribute name should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getClientAuthenticationMethod())) {
                    throw new DataValidationException("Client authentication method should be specified!");
                }
                if (StringUtils.isEmpty(clientRegistration.getLoginButtonLabel())) {
                    throw new DataValidationException("Login button label should be specified!");
                }
                OAuth2MapperConfig mapperConfig = clientRegistration.getMapperConfig();
                if (mapperConfig == null) {
                    throw new DataValidationException("Mapper config should be specified!");
                }
                if (mapperConfig.getType() == null) {
                    throw new DataValidationException("Mapper config type should be specified!");
                }
                if (mapperConfig.getType() == MapperType.BASIC) {
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
                    if (basicConfig == null) {
                        throw new DataValidationException("Basic config should be specified!");
                    }
                    if (StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                        throw new DataValidationException("Email attribute key should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == null) {
                        throw new DataValidationException("Tenant name strategy should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                            && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                        throw new DataValidationException("Tenant name pattern should be specified!");
                    }
                }
                if (mapperConfig.getType() == MapperType.GITHUB) {
                    OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
                    if (basicConfig == null) {
                        throw new DataValidationException("Basic config should be specified!");
                    }
                    if (!StringUtils.isEmpty(basicConfig.getEmailAttributeKey())) {
                        throw new DataValidationException("Email attribute key cannot be configured for GITHUB mapper type!");
                    }
                    if (basicConfig.getTenantNameStrategy() == null) {
                        throw new DataValidationException("Tenant name strategy should be specified!");
                    }
                    if (basicConfig.getTenantNameStrategy() == TenantNameStrategyType.CUSTOM
                            && StringUtils.isEmpty(basicConfig.getTenantNamePattern())) {
                        throw new DataValidationException("Tenant name pattern should be specified!");
                    }
                }
                if (mapperConfig.getType() == MapperType.CUSTOM) {
                    OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
                    if (customConfig == null) {
                        throw new DataValidationException("Custom config should be specified!");
                    }
                    if (StringUtils.isEmpty(customConfig.getUrl())) {
                        throw new DataValidationException("Custom mapper URL should be specified!");
                    }
                }
            }
        }
    };
}
