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
package org.thingsboard.server.dao.sql.mobile;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleOauth2Client;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.mobile.MobileAppBundleDao;
import org.thingsboard.server.dao.model.sql.MobileAppBundleOauth2ClientEntity;
import org.thingsboard.server.dao.model.sql.MobileAppBundlePolicyInfoEntity;
import org.thingsboard.server.dao.model.sql.MobileAppOauth2ClientCompositeKey;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaMobileAppBundleDao extends JpaAbstractDao<MobileAppBundlePolicyInfoEntity, MobileAppBundle> implements MobileAppBundleDao {

    private final MobileAppBundleRepository mobileAppBundleRepository;
    private final MobileAppBundleOauth2ClientRepository mobileOauth2ProviderRepository;
    private final MobileAppBundlePolicyInfoRepository mobileAppBundlePolicyInfoRepository;

    @Override
    protected Class<MobileAppBundlePolicyInfoEntity> getEntityClass() {
        return MobileAppBundlePolicyInfoEntity.class;
    }

    @Override
    protected JpaRepository<MobileAppBundlePolicyInfoEntity, UUID> getRepository() {
        return mobileAppBundlePolicyInfoRepository;
    }

    @Override
    public MobileAppBundle findPolicyInfoByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platform) {
        return DaoUtil.getData(mobileAppBundlePolicyInfoRepository.findByPkgNameAndPlatformType(pkgName, platform));
    }

    @Override
    public PageData<MobileAppBundleInfo> findInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(mobileAppBundleRepository.findInfoByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public MobileAppBundleInfo findInfoById(TenantId tenantId, MobileAppBundleId mobileAppBundleId) {
        return DaoUtil.getData(mobileAppBundleRepository.findInfoById(mobileAppBundleId.getId()));
    }

    @Override
    public List<MobileAppBundleOauth2Client> findOauth2ClientsByMobileAppBundleId(TenantId tenantId, MobileAppBundleId mobileAppBundleId) {
        return DaoUtil.convertDataList(mobileOauth2ProviderRepository.findAllByMobileAppBundleId(mobileAppBundleId.getId()));
    }

    @Override
    public void addOauth2Client(TenantId tenantId, MobileAppBundleOauth2Client mobileAppBundleOauth2Client) {
        mobileOauth2ProviderRepository.save(new MobileAppBundleOauth2ClientEntity(mobileAppBundleOauth2Client));
    }

    @Override
    public void removeOauth2Client(TenantId tenantId, MobileAppBundleOauth2Client mobileAppBundleOauth2Client) {
        mobileOauth2ProviderRepository.deleteById(new MobileAppOauth2ClientCompositeKey(mobileAppBundleOauth2Client.getMobileAppBundleId().getId(),
                mobileAppBundleOauth2Client.getOAuth2ClientId().getId()));
    }

    @Override
    public MobileAppBundle findByPkgNameAndPlatform(TenantId tenantId, String pkgName, PlatformType platform) {
        return DaoUtil.getData(mobileAppBundleRepository.findByPkgNameAndPlatformType(pkgName, platform));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        mobileAppBundleRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP_BUNDLE;
    }

}

