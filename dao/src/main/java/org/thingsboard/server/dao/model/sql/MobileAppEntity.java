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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_STORE_INFO_EMPTY_OBJECT;
import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_VERSION_INFO_EMPTY_OBJECT;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.MOBILE_APP_TABLE_NAME)
public class MobileAppEntity extends BaseSqlEntity<MobileApp> {

    @Column(name = TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = ModelConstants.MOBILE_APP_PKG_NAME_PROPERTY)
    private String pkgName;

    @Column(name = ModelConstants.MOBILE_APP_APP_SECRET_PROPERTY)
    private String appSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.MOBILE_APP_PLATFORM_TYPE_PROPERTY)
    private PlatformType platformType;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.MOBILE_APP_STATUS_PROPERTY)
    private MobileAppStatus status;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_VERSION_INFO_PROPERTY)
    private JsonNode versionInfo;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.MOBILE_APP_STORE_INFO_PROPERTY)
    private JsonNode storeInfo;

    public MobileAppEntity() {
        super();
    }

    public MobileAppEntity(MobileApp mobile) {
        super(mobile);
        if (mobile.getTenantId() != null) {
            this.tenantId = mobile.getTenantId().getId();
        }
        this.pkgName = mobile.getPkgName();
        this.appSecret = mobile.getAppSecret();
        this.platformType = mobile.getPlatformType();
        this.status = mobile.getStatus();
        this.versionInfo = toJson(mobile.getVersionInfo());
        this.storeInfo = toJson(mobile.getStoreInfo());
    }

    @Override
    public MobileApp toData() {
        MobileApp mobile = new MobileApp();
        mobile.setId(new MobileAppId(id));
        if (tenantId != null) {
            mobile.setTenantId(TenantId.fromUUID(tenantId));
        }
        mobile.setCreatedTime(createdTime);
        mobile.setPkgName(pkgName);
        mobile.setAppSecret(appSecret);
        mobile.setPlatformType(platformType);
        mobile.setStatus(status);
        mobile.setVersionInfo(versionInfo != null ? fromJson(versionInfo, MobileAppVersionInfo.class) : MOBILE_APP_VERSION_INFO_EMPTY_OBJECT);
        mobile.setStoreInfo(storeInfo != null ? fromJson(storeInfo, StoreInfo.class) : MOBILE_APP_STORE_INFO_EMPTY_OBJECT);
        return mobile;
    }
}
