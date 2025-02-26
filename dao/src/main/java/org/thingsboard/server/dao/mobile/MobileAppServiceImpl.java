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
package org.thingsboard.server.dao.mobile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.checkNotNull;

@Slf4j
@Service
public class MobileAppServiceImpl extends AbstractEntityService implements MobileAppService {

    private static final String PLATFORM_TYPE_IS_REQUIRED = "Platform type is required if package name is specified";

    @Autowired
    private MobileAppDao mobileAppDao;
    @Autowired
    private DataValidator<MobileApp> mobileAppDataValidator;

    @Override
    public MobileApp saveMobileApp(TenantId tenantId, MobileApp mobileApp) {
        log.trace("Executing saveMobileApp [{}]", mobileApp);
        mobileAppDataValidator.validate(mobileApp, a -> tenantId);
        try {
            MobileApp savedMobileApp = mobileAppDao.save(tenantId, mobileApp);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(savedMobileApp).build());
            return savedMobileApp;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    Map.of("mobile_app_pkg_name_platform_unq_key", "Mobile app with such package name and platform already exists!"));
            throw e;
        }
    }

    @Override
    public void deleteMobileAppById(TenantId tenantId, MobileAppId mobileAppId) {
        log.trace("Executing deleteMobileAppById [{}]", mobileAppId.getId());
        mobileAppDao.removeById(tenantId, mobileAppId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(mobileAppId).build());
    }

    @Override
    public MobileApp findMobileAppById(TenantId tenantId, MobileAppId mobileAppId) {
        log.trace("Executing findMobileAppById [{}] [{}]", tenantId, mobileAppId);
        return mobileAppDao.findById(tenantId, mobileAppId.getId());
    }

    @Override
    public PageData<MobileApp> findMobileAppsByTenantId(TenantId tenantId, PlatformType platformType, PageLink pageLink) {
        log.trace("Executing findMobileAppInfosByTenantId [{}]", tenantId);
        return mobileAppDao.findByTenantId(tenantId, platformType, pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findMobileAppById(tenantId, new MobileAppId(entityId.getId())));
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteMobileAppById(tenantId, (MobileAppId) id);
    }

    @Override
    public MobileApp findByBundleIdAndPlatformType(TenantId tenantId, MobileAppBundleId mobileAppBundleId, PlatformType platformType) {
        log.trace("Executing findAndroidQrConfig, tenantId [{}], mobileAppBundleId [{}]", tenantId, mobileAppBundleId);
        return mobileAppDao.findByBundleIdAndPlatformType(tenantId, mobileAppBundleId, platformType);
    }

    @Override
    public MobileApp findMobileAppByPkgNameAndPlatformType(String pkgName, PlatformType platformType) {
        log.trace("Executing findMobileAppByPkgNameAndPlatformType, pkgName [{}], platform [{}]", pkgName, platformType);
        checkNotNull(platformType, PLATFORM_TYPE_IS_REQUIRED);
        return mobileAppDao.findByPkgNameAndPlatformType(TenantId.SYS_TENANT_ID, pkgName, platformType);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        mobileAppDao.deleteByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP;
    }
}
