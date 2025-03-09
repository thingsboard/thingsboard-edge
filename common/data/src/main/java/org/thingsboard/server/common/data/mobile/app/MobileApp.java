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
package org.thingsboard.server.common.data.mobile.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.validation.Length;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class MobileApp extends BaseData<MobileAppId> implements HasName, TenantEntity {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Application package name. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "pkgName")
    private String pkgName;
    @Schema(description = "Application secret. The length must be at least 16 characters", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Length(fieldName = "appSecret", min = 16, max = 2048, message = "must be at least 16 and max 2048 characters")
    private String appSecret;
    @Schema(description = "Application platform type: ANDROID or IOS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private PlatformType platformType;
    @Schema(description = "Application status: PUBLISHED, DEPRECATED, SUSPENDED, DRAFT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private MobileAppStatus status;
    @Schema(description = "Application version info")
    @Valid
    private MobileAppVersionInfo versionInfo;
    @Schema(description = "Application store information")
    @Valid
    private StoreInfo storeInfo;

    public MobileApp() {
        super();
    }

    public MobileApp(MobileAppId id) {
        super(id);
    }

    public MobileApp(MobileApp mobile) {
        super(mobile);
        this.tenantId = mobile.tenantId;
        this.pkgName = mobile.pkgName;
        this.appSecret = mobile.appSecret;
        this.platformType = mobile.platformType;
        this.status = mobile.status;
        this.versionInfo = mobile.versionInfo;
        this.storeInfo = mobile.storeInfo;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Mobile app package name", example = "my.mobile.app", accessMode = Schema.AccessMode.READ_ONLY)
    public String getName() {
        return pkgName;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP;
    }

}
