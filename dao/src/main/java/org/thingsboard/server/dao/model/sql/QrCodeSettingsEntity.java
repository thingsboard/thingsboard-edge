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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.QrCodeSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QrCodeSettings;
import org.thingsboard.server.common.data.mobile.qrCodeSettings.QRCodeConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.QR_CODE_SETTINGS_TABLE_NAME)
public class QrCodeSettingsEntity extends BaseSqlEntity<QrCodeSettings> {

    @Column(name = ModelConstants.TENANT_ID_COLUMN, columnDefinition = "uuid")
    protected UUID tenantId;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_USE_SYSTEM_SETTINGS_PROPERTY)
    private boolean useSystemSettings;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_USE_DEFAULT_APP_PROPERTY)
    private boolean useDefaultApp;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_ANDROID_ENABLED_PROPERTY)
    private boolean androidEnabled;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_IOS_ENABLED_PROPERTY)
    private boolean iosEnabled;

    @Column(name = ModelConstants.QR_CODE_SETTINGS_BUNDLE_ID_PROPERTY)
    private UUID mobileAppBundleId;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.QR_CODE_SETTINGS_CONFIG_PROPERTY)
    private JsonNode qrCodeConfig;

    public QrCodeSettingsEntity(QrCodeSettings qrCodeSettings) {
        this.setId(qrCodeSettings.getUuidId());
        this.setCreatedTime(qrCodeSettings.getCreatedTime());
        this.tenantId = qrCodeSettings.getTenantId().getId();
        this.useSystemSettings = qrCodeSettings.isUseSystemSettings();
        this.useDefaultApp = qrCodeSettings.isUseDefaultApp();
        this.androidEnabled = qrCodeSettings.isAndroidEnabled();
        this.iosEnabled = qrCodeSettings.isIosEnabled();
        if (qrCodeSettings.getMobileAppBundleId() != null) {
            this.mobileAppBundleId = qrCodeSettings.getMobileAppBundleId().getId();
        }
        this.qrCodeConfig = toJson(qrCodeSettings.getQrCodeConfig());
    }

    @Override
    public QrCodeSettings toData() {
        QrCodeSettings qrCodeSettings = new QrCodeSettings(new QrCodeSettingsId(getUuid()));
        qrCodeSettings.setCreatedTime(createdTime);
        qrCodeSettings.setTenantId(TenantId.fromUUID(tenantId));
        qrCodeSettings.setUseSystemSettings(useSystemSettings);
        qrCodeSettings.setUseDefaultApp(useDefaultApp);
        qrCodeSettings.setAndroidEnabled(androidEnabled);
        qrCodeSettings.setIosEnabled(iosEnabled);
        if (mobileAppBundleId != null) {
            qrCodeSettings.setMobileAppBundleId(new MobileAppBundleId(mobileAppBundleId));
        }
        qrCodeSettings.setQrCodeConfig(fromJson(qrCodeConfig, QRCodeConfig.class));
        return qrCodeSettings;
    }

}
