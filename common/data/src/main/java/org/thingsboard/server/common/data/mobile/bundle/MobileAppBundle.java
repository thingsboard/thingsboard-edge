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
package org.thingsboard.server.common.data.mobile.bundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.layout.MobileLayoutConfig;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.common.data.validation.Length;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class MobileAppBundle extends BaseData<MobileAppBundleId> implements HasName, TenantEntity {

    @Schema(description = "JSON object with Tenant Id")
    private TenantId tenantId;
    @Schema(description = "Application bundle title. Cannot be empty", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Length(fieldName = "title")
    private String title;
    @Schema(description = "Application bundle description.")
    @Length(fieldName = "description")
    private String description;
    @Schema(description = "Android application id")
    private MobileAppId androidAppId;
    @Schema(description = "IOS application id")
    private MobileAppId iosAppId;
    @Schema(description = "Application layout configuration")
    @Valid
    private MobileLayoutConfig layoutConfig;
    @Schema(description = "Application self registration configuration")
    @Valid
    private MobileSelfRegistrationParams selfRegistrationParams;
    @Schema(description = "Whether OAuth2 settings are enabled or not")
    private Boolean oauth2Enabled;

    public MobileAppBundle() {
        super();
    }

    public MobileAppBundle(MobileAppBundleId id) {
        super(id);
    }

    public MobileAppBundle(MobileAppBundle mobile) {
        super(mobile);
        this.tenantId = mobile.tenantId;
        this.title = mobile.title;
        this.description = mobile.description;
        this.androidAppId = mobile.androidAppId;
        this.iosAppId = mobile.iosAppId;
        this.layoutConfig = mobile.layoutConfig;
        this.selfRegistrationParams = mobile.selfRegistrationParams;
        this.oauth2Enabled = mobile.oauth2Enabled;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Mobile app bundle title", example = "My main application", accessMode = Schema.AccessMode.READ_ONLY)
    public String getName() {
        return title;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MOBILE_APP_BUNDLE;
    }
}
