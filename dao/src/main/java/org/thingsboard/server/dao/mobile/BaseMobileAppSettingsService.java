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
package org.thingsboard.server.dao.mobile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.BadgePosition;
import org.thingsboard.server.common.data.mobile.BadgeStyle;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseMobileAppSettingsService extends AbstractCachedEntityService<TenantId, MobileAppSettings, MobileAppSettingsEvictEvent> implements MobileAppSettingsService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String DEFAULT_QR_CODE_LABEL = "Scan to connect or download mobile app";

    private final MobileAppSettingsDao mobileAppSettingsDao;
    private final DataValidator<MobileAppSettings> mobileAppSettingsDataValidator;

    @Override
    public MobileAppSettings saveMobileAppSettings(TenantId tenantId, MobileAppSettings mobileAppSettings) {
        mobileAppSettingsDataValidator.validate(mobileAppSettings, s -> tenantId);
        try {
            MobileAppSettings savedMobileAppSettings = mobileAppSettingsDao.save(tenantId, mobileAppSettings);
            publishEvictEvent(new MobileAppSettingsEvictEvent(tenantId));
            return savedMobileAppSettings;
        } catch (Exception exception) {
            if (mobileAppSettings != null) {
                handleEvictEvent(new MobileAppSettingsEvictEvent(tenantId));
            }
            ConstraintViolationException e = DaoUtil.extractConstraintViolationException(exception).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("mobile_app_settings_tenant_id_key")) {
                throw new DataValidationException("Mobile application for specified tenant already exists!");
            } else {
                throw exception;
            }
        }
    }

    @Override
    public MobileAppSettings getCurrentMobileAppSettings(TenantId tenantId) {
        log.trace("Executing getMobileAppSettings for tenant [{}] ", tenantId);
        MobileAppSettings mobileAppSettings = cache.getAndPutInTransaction(tenantId,
                () -> mobileAppSettingsDao.findByTenantId(tenantId), true);
        return constructMobileAppSettings(mobileAppSettings);
    }

    @Override
    public MobileAppSettings getMobileAppSettings(TenantId tenantId) {
        log.trace("Executing getMobileQrCodeConfig for tenant [{}] ", tenantId);
        MobileAppSettings mobileAppSettings = getCurrentMobileAppSettings(tenantId);
        if (!tenantId.isSysTenantId() && mobileAppSettings.isUseSystemSettings()) {
            mobileAppSettings = getCurrentMobileAppSettings(TenantId.SYS_TENANT_ID);
        }
        return mobileAppSettings;
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        mobileAppSettingsDao.removeByTenantId(tenantId);
    }

    @TransactionalEventListener(classes = MobileAppSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(MobileAppSettingsEvictEvent event) {
        cache.evict(event.getTenantId());
    }

    private MobileAppSettings constructMobileAppSettings(MobileAppSettings mobileAppSettings) {
        if (mobileAppSettings == null) {
            mobileAppSettings = new MobileAppSettings();
            mobileAppSettings.setUseSystemSettings(true);
            mobileAppSettings.setUseDefaultApp(true);

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .enabled(true)
                    .build();
            IosConfig iosConfig = IosConfig.builder()
                    .enabled(true)
                    .build();
            QRCodeConfig qrCodeConfig = QRCodeConfig.builder()
                    .showOnHomePage(true)
                    .qrCodeLabelEnabled(true)
                    .qrCodeLabel(DEFAULT_QR_CODE_LABEL)
                    .badgeEnabled(true)
                    .badgePosition(BadgePosition.RIGHT)
                    .badgeStyle(BadgeStyle.ORIGINAL)
                    .badgeEnabled(true)
                    .build();

            mobileAppSettings.setQrCodeConfig(qrCodeConfig);
            mobileAppSettings.setAndroidConfig(androidConfig);
            mobileAppSettings.setIosConfig(iosConfig);
        }
        return mobileAppSettings;
    }

}
