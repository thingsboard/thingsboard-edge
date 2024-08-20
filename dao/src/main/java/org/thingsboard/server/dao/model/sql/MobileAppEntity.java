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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.MobileApp;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

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

    @Column(name = ModelConstants.MOBILE_APP_OAUTH2_ENABLED_PROPERTY)
    private Boolean oauth2Enabled;

    public MobileAppEntity() {
        super();
    }

    public MobileAppEntity(MobileApp mobile) {
        if (mobile.getId() != null) {
            this.setUuid(mobile.getId().getId());
        }
        if (mobile.getTenantId() != null) {
            this.tenantId = mobile.getTenantId().getId();
        }
        this.setCreatedTime(mobile.getCreatedTime());
        this.pkgName = mobile.getPkgName();
        this.appSecret = mobile.getAppSecret();
        this.oauth2Enabled = mobile.isOauth2Enabled();
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
        mobile.setOauth2Enabled(oauth2Enabled);
        return mobile;
    }
}
