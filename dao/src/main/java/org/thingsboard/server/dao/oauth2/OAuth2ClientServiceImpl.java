/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientLoginInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service("OAuth2ClientService")
public class OAuth2ClientServiceImpl extends AbstractEntityService implements OAuth2ClientService {

    @Autowired
    private OAuth2ClientDao oauth2ClientDao;
    @Autowired
    private DataValidator<OAuth2Client> oAuth2ClientDataValidator;

    @Override
    public List<OAuth2ClientLoginInfo> findOAuth2ClientLoginInfosByDomainName(String domainName) {
        log.trace("Executing findOAuth2ClientLoginInfosByDomainName [{}] ", domainName);
        return oauth2ClientDao.findEnabledByDomainName(domainName)
                .stream()
                .map(OAuth2Utils::toClientLoginInfo)
                .collect(Collectors.toList());
    }

    @Override
    public List<OAuth2ClientLoginInfo> findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType(String pkgName, PlatformType platformType) {
        log.trace("Executing findOAuth2ClientLoginInfosByMobilePkgNameAndPlatformType pkgName=[{}] platformType=[{}]",pkgName, platformType);
        return oauth2ClientDao.findEnabledByPkgNameAndPlatformType(pkgName, platformType)
                .stream()
                .map(OAuth2Utils::toClientLoginInfo)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OAuth2Client saveOAuth2Client(TenantId tenantId, OAuth2Client oAuth2Client) {
        log.trace("Executing saveOAuth2Client [{}]", oAuth2Client);
        oAuth2ClientDataValidator.validate(oAuth2Client, OAuth2Client::getTenantId);
        OAuth2Client savedOauth2Client = oauth2ClientDao.save(tenantId, oAuth2Client);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entity(oAuth2Client).build());
        return savedOauth2Client;
    }

    @Override
    public OAuth2Client findOAuth2ClientById(TenantId tenantId, OAuth2ClientId oAuth2ClientId) {
        log.trace("Executing findOAuth2ClientById [{}]", oAuth2ClientId);
        return oauth2ClientDao.findById(tenantId, oAuth2ClientId.getId());
    }

    @Override
    public List<OAuth2Client> findOAuth2ClientsByTenantId(TenantId tenantId) {
        log.trace("Executing findOAuth2ClientsByTenantId [{}]", tenantId);
        return oauth2ClientDao.findByTenantId(tenantId.getId(), new PageLink(Integer.MAX_VALUE)).getData();
    }

    @Override
    public String findAppSecret(OAuth2ClientId oAuth2ClientId, String pkgName) {
        log.trace("Executing findAppSecret oAuth2ClientId = [{}] pkgName = [{}]", oAuth2ClientId, pkgName);
        return oauth2ClientDao.findAppSecret(oAuth2ClientId.getId(), pkgName);
    }

    @Override
    @Transactional
    public void deleteOAuth2ClientById(TenantId tenantId, OAuth2ClientId oAuth2ClientId) {
        log.trace("Executing deleteOAuth2ClientById [{}]", oAuth2ClientId);
        oauth2ClientDao.removeById(tenantId, oAuth2ClientId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(oAuth2ClientId)
                .build());

    }

    @Override
    public void deleteOauth2ClientsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteOauth2ClientsByTenantId, tenantId [{}]", tenantId);
        oauth2ClientDao.deleteByTenantId(tenantId.getId());
    }

    @Override
    public PageData<OAuth2ClientInfo> findOAuth2ClientInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findOAuth2ClientInfosByTenantId tenantId=[{}]", tenantId);
        PageData<OAuth2Client> clients = oauth2ClientDao.findByTenantId(tenantId.getId(), pageLink);
        return clients.mapData(OAuth2ClientInfo::new);
    }

    @Override
    public List<OAuth2ClientInfo> findOAuth2ClientInfosByIds(TenantId tenantId, List<OAuth2ClientId> oAuth2ClientIds) {
        log.trace("Executing findQueueStatsByIds, tenantId [{}], oAuth2ClientIds [{}]", tenantId, oAuth2ClientIds);
        return oauth2ClientDao.findByIds(tenantId.getId(), oAuth2ClientIds)
                .stream()
                .map(OAuth2ClientInfo::new)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteOauth2ClientsByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findOAuth2ClientById(tenantId, new OAuth2ClientId(entityId.getId())));
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteOAuth2ClientById(tenantId, (OAuth2ClientId) id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }

}
