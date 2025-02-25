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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.BadgePosition;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QRCodeConfig;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Map;

import static org.thingsboard.server.common.data.oauth2.PlatformType.ANDROID;
import static org.thingsboard.server.common.data.oauth2.PlatformType.IOS;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class QrCodeSettingServiceImpl extends AbstractCachedEntityService<TenantId, QrCodeSettings, QrCodeSettingsEvictEvent> implements QrCodeSettingService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String DEFAULT_QR_CODE_LABEL = "Scan to connect or download mobile app";

    @Value("${mobileApp.googlePlayLink:https://play.google.com/store/apps/details?id=org.thingsboard.cloud}")
    private String googlePlayLink;
    @Value("${mobileApp.appStoreLink:https://apps.apple.com/ua/app/thingsboard-cloud/id6499209395}")
    private String appStoreLink;

    private final QrCodeSettingsDao qrCodeSettingsDao;
    private final MobileAppService mobileAppService;
    private final DataValidator<QrCodeSettings> mobileAppSettingsDataValidator;

    @Override
    public QrCodeSettings saveQrCodeSettings(TenantId tenantId, QrCodeSettings qrCodeSettings) {
        mobileAppSettingsDataValidator.validate(qrCodeSettings, s -> tenantId);
        try {
            QrCodeSettings savedQrCodeSettings = qrCodeSettingsDao.save(tenantId, qrCodeSettings);
            publishEvictEvent(new QrCodeSettingsEvictEvent(tenantId));
            return constructMobileAppSettings(tenantId, savedQrCodeSettings);
        } catch (Exception e) {
            handleEvictEvent(new QrCodeSettingsEvictEvent(tenantId));
            checkConstraintViolation(e, Map.of(
                    "qr_code_settings_tenant_id_unq_key", "Mobile application for specified tenant already exists!"
            ));
            throw e;
        }
    }

    @Override
    public QrCodeSettings findQrCodeSettings(TenantId tenantId) {
        log.trace("Executing getMobileAppSettings for tenant [{}] ", tenantId);
        QrCodeSettings qrCodeSettings = cache.getAndPutInTransaction(tenantId,
                () -> qrCodeSettingsDao.findByTenantId(tenantId), true);
        return constructMobileAppSettings(tenantId, qrCodeSettings);
    }

    @Override
    public MobileApp findAppFromQrCodeSettings(TenantId tenantId, PlatformType platformType) {
        log.trace("Executing findAppQrCodeConfig for tenant [{}] ", tenantId);
        QrCodeSettings qrCodeSettings = getMergedQrCodeSettings(tenantId);
        return qrCodeSettings.getMobileAppBundleId() != null ? mobileAppService.findByBundleIdAndPlatformType(tenantId, qrCodeSettings.getMobileAppBundleId(), platformType) : null;
    }

    @Override
    public QrCodeSettings getMergedQrCodeSettings(TenantId tenantId) {
        log.trace("Executing getMobileQrCodeConfig for tenant [{}] ", tenantId);
        QrCodeSettings mobileAppSettings = findQrCodeSettings(tenantId);
        if (!tenantId.isSysTenantId() && mobileAppSettings.isUseSystemSettings()) {
            mobileAppSettings = findQrCodeSettings(TenantId.SYS_TENANT_ID);
        }
        return mobileAppSettings;
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        qrCodeSettingsDao.removeByTenantId(tenantId);
    }

    @TransactionalEventListener(classes = QrCodeSettingsEvictEvent.class)
    @Override
    public void handleEvictEvent(QrCodeSettingsEvictEvent event) {
        cache.evict(event.getTenantId());
    }

    private QrCodeSettings constructMobileAppSettings(TenantId tenantId, QrCodeSettings qrCodeSettings) {
        if (qrCodeSettings == null) {
            qrCodeSettings = new QrCodeSettings();
            qrCodeSettings.setUseDefaultApp(true);
            qrCodeSettings.setAndroidEnabled(true);
            qrCodeSettings.setIosEnabled(true);
            if (!tenantId.isSysTenantId()) {
                qrCodeSettings.setUseSystemSettings(true);
            }

            QRCodeConfig qrCodeConfig = QRCodeConfig.builder()
                    .showOnHomePage(true)
                    .qrCodeLabelEnabled(true)
                    .qrCodeLabel(DEFAULT_QR_CODE_LABEL)
                    .badgeEnabled(true)
                    .badgePosition(BadgePosition.RIGHT)
                    .badgeEnabled(true)
                    .build();

            qrCodeSettings.setQrCodeConfig(qrCodeConfig);
            qrCodeSettings.setMobileAppBundleId(qrCodeSettings.getMobileAppBundleId());
        }
        if (!qrCodeSettings.isUseSystemSettings()) {
            if (qrCodeSettings.isUseDefaultApp() || qrCodeSettings.getMobileAppBundleId() == null) {
                qrCodeSettings.setGooglePlayLink(googlePlayLink);
                qrCodeSettings.setAppStoreLink(appStoreLink);
            } else {
                MobileApp androidApp = mobileAppService.findByBundleIdAndPlatformType(qrCodeSettings.getTenantId(), qrCodeSettings.getMobileAppBundleId(), ANDROID);
                MobileApp iosApp = mobileAppService.findByBundleIdAndPlatformType(qrCodeSettings.getTenantId(), qrCodeSettings.getMobileAppBundleId(), IOS);
                if (androidApp != null && androidApp.getStoreInfo() != null) {
                    qrCodeSettings.setGooglePlayLink(androidApp.getStoreInfo().getStoreLink());
                }
                if (iosApp != null && iosApp.getStoreInfo() != null) {
                    qrCodeSettings.setAppStoreLink(iosApp.getStoreInfo().getStoreLink());
                }
            }
        }
        return qrCodeSettings;
    }

}
