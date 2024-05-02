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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.MobileAppSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.AndroidConfig;
import org.thingsboard.server.common.data.mobile.IosConfig;
import org.thingsboard.server.common.data.mobile.MobileAppSettings;
import org.thingsboard.server.common.data.mobile.QRCodeConfig;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.MOBILE_APP_SETTINGS_TABLE_NAME)
public class MobileAppSettingsEntity extends BaseSqlEntity<MobileAppSettings> {

    @Column(name = ModelConstants.TENANT_ID_COLUMN, columnDefinition = "uuid")
    protected UUID tenantId;

    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_USE_SYSTEM_SETTINGS_PROPERTY)
    private boolean useSystemSettings;

    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_USE_DEFAULT_APP_PROPERTY)
    private boolean useDefaultApp;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_ANDROID_CONFIG_PROPERTY)
    private JsonNode androidConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_IOS_CONFIG_PROPERTY)
    private JsonNode iosConfig;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_SETTINGS_QR_CODE_CONFIG_PROPERTY)
    private JsonNode qrCodeConfig;

    public MobileAppSettingsEntity(MobileAppSettings mobileAppSettings) {
        if (mobileAppSettings.getId() != null) {
            this.setId(mobileAppSettings.getId().getId());
        }
        this.setCreatedTime(mobileAppSettings.getCreatedTime());
        this.tenantId = mobileAppSettings.getTenantId().getId();
        this.useSystemSettings = mobileAppSettings.isUseSystemSettings();
        this.useDefaultApp = mobileAppSettings.isUseDefaultApp();
        this.androidConfig = toJson(mobileAppSettings.getAndroidConfig());
        this.iosConfig = toJson(mobileAppSettings.getIosConfig());
        this.qrCodeConfig = toJson(mobileAppSettings.getQrCodeConfig());
   }

    @Override
    public MobileAppSettings toData() {
        MobileAppSettings mobileAppSettings = new MobileAppSettings(new MobileAppSettingsId(getUuid()));
        mobileAppSettings.setCreatedTime(createdTime);
        mobileAppSettings.setTenantId(TenantId.fromUUID(tenantId));
        mobileAppSettings.setUseSystemSettings(useSystemSettings);
        mobileAppSettings.setUseDefaultApp(useDefaultApp);
        mobileAppSettings.setAndroidConfig(fromJson(androidConfig, AndroidConfig.class));
        mobileAppSettings.setIosConfig(fromJson(iosConfig, IosConfig.class));
        mobileAppSettings.setQrCodeConfig(fromJson(qrCodeConfig, QRCodeConfig.class));
        return mobileAppSettings;
    }
}
