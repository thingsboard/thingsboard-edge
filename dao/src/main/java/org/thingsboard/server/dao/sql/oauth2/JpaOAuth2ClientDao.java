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
package org.thingsboard.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2ClientEntity;
import org.thingsboard.server.dao.oauth2.OAuth2ClientDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2ClientDao extends JpaAbstractDao<OAuth2ClientEntity, OAuth2Client> implements OAuth2ClientDao {

    private final OAuth2ClientRepository repository;

    @Override
    protected Class<OAuth2ClientEntity> getEntityClass() {
        return OAuth2ClientEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2ClientEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public PageData<OAuth2Client> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantIdAndCustomerId(tenantId, customerId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<OAuth2Client> findEnabledByDomainName(String domainName) {
        return DaoUtil.convertDataList(repository.findEnabledByDomainNameAndPlatformType(domainName, PlatformType.WEB.name()));
    }

    @Override
    public List<OAuth2Client> findEnabledByPkgNameAndPlatformType(String pkgName, PlatformType platformType) {
        List<OAuth2ClientEntity> clientEntities;
        if (platformType != null) {
            clientEntities = switch (platformType) {
                case ANDROID -> repository.findEnabledByAndroidPkgNameAndPlatformType(pkgName, platformType.name());
                case IOS -> repository.findEnabledByIosPkgNameAndPlatformType(pkgName, platformType.name());
                default -> Collections.emptyList();
            };
        } else {
            clientEntities = Collections.emptyList();
        }
        return DaoUtil.convertDataList(clientEntities);
    }

    @Override
    public List<OAuth2Client> findByDomainId(UUID oauth2ParamsId) {
        return DaoUtil.convertDataList(repository.findByDomainId(oauth2ParamsId));
    }

    @Override
    public List<OAuth2Client> findByMobileAppBundleId(UUID mobileAppBundleId) {
        return DaoUtil.convertDataList(repository.findByMobileAppBundleId(mobileAppBundleId));
    }

    @Override
    public String findAppSecret(UUID id, String pkgName, PlatformType platformType) {
        return repository.findAppSecret(id, pkgName, platformType);
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        repository.deleteByTenantId(tenantId);
    }

    @Override
    public List<OAuth2Client> findByIds(UUID tenantId, List<OAuth2ClientId> oAuth2ClientIds) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndIdIn(tenantId, toUUIDs(oAuth2ClientIds)));
    }

    @Override
    public boolean isPropagateToEdge(TenantId tenantId, UUID oAuth2ClientId) {
        return repository.isPropagateToEdge(tenantId.getId(), oAuth2ClientId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OAUTH2_CLIENT;
    }

}
