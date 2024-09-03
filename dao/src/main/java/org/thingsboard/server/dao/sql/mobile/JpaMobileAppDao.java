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
package org.thingsboard.server.dao.sql.mobile;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.common.data.mobile.MobileAppOauth2Client;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.dao.model.sql.MobileAppEntity;
import org.thingsboard.server.dao.model.sql.MobileAppOauth2ClientCompositeKey;
import org.thingsboard.server.dao.model.sql.MobileAppOauth2ClientEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaMobileAppDao extends JpaAbstractDao<MobileAppEntity, MobileApp> implements MobileAppDao {

    private final MobileAppRepository mobileAppRepository;
    private final MobileAppOauth2ClientRepository mobileOauth2ProviderRepository;

    @Override
    protected Class<MobileAppEntity> getEntityClass() {
        return MobileAppEntity.class;
    }

    @Override
    protected JpaRepository<MobileAppEntity, UUID> getRepository() {
        return mobileAppRepository;
    }

    @Override
    public PageData<MobileApp> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(mobileAppRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<MobileAppOauth2Client> findOauth2ClientsByMobileAppId(TenantId tenantId, MobileAppId mobileAppId) {
        return DaoUtil.convertDataList(mobileOauth2ProviderRepository.findAllByMobileAppId(mobileAppId.getId()));
    }

    @Override
    public void addOauth2Client(MobileAppOauth2Client mobileAppOauth2Client) {
        mobileOauth2ProviderRepository.save(new MobileAppOauth2ClientEntity(mobileAppOauth2Client));
    }

    @Override
    public void removeOauth2Client(MobileAppOauth2Client mobileAppOauth2Client) {
        mobileOauth2ProviderRepository.deleteById(new MobileAppOauth2ClientCompositeKey(mobileAppOauth2Client.getMobileAppId().getId(),
                mobileAppOauth2Client.getOAuth2ClientId().getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        mobileAppRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP;
    }

}

