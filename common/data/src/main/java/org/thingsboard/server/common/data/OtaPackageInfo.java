/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class OtaPackageInfo extends BaseDataWithAdditionalInfo<OtaPackageId> implements TenantEntity, HasName, HasTitle {

    private static final long serialVersionUID = 3168391583570815419L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the ota package can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Device Profile Id. Device Profile Id of the ota package can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private DeviceProfileId deviceProfileId;
    @Schema(description = "OTA Package type.", example = "FIRMWARE", accessMode = Schema.AccessMode.READ_ONLY)
    private OtaPackageType type;
    @Length(fieldName = "title")
    @NoXss
    @Schema(description = "OTA Package title.", example = "fw", accessMode = Schema.AccessMode.READ_ONLY)
    private String title;
    @Length(fieldName = "version")
    @NoXss
    @Schema(description = "OTA Package version.", example = "1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String version;
    @Length(fieldName = "tag")
    @NoXss
    @Schema(description = "OTA Package tag.", example = "fw_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String tag;
    @Length(fieldName = "url")
    @NoXss
    @Schema(description = "OTA Package url.", example = "http://thingsboard.org/fw/1", accessMode = Schema.AccessMode.READ_ONLY)
    private String url;
    @Schema(description = "Indicates OTA Package 'has data'. Field is returned from DB ('true' if data exists or url is set).  If OTA Package 'has data' is 'false' we can not assign the OTA Package to the Device or Device Profile.", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean hasData;
    @Length(fieldName = "file name")
    @NoXss
    @Schema(description = "OTA Package file name.", example = "fw_1.0", accessMode = Schema.AccessMode.READ_ONLY)
    private String fileName;
    @NoXss
    @Length(fieldName = "contentType")
    @Schema(description = "OTA Package content type.", example = "APPLICATION_OCTET_STREAM", accessMode = Schema.AccessMode.READ_ONLY)
    private String contentType;
    @Schema(description = "OTA Package checksum algorithm.", example = "CRC32", accessMode = Schema.AccessMode.READ_ONLY)
    private ChecksumAlgorithm checksumAlgorithm;
    @Length(fieldName = "checksum", max = 1020)
    @Schema(description = "OTA Package checksum.", example = "0xd87f7e0c", accessMode = Schema.AccessMode.READ_ONLY)
    private String checksum;
    @Schema(description = "OTA Package data size.", example = "8", accessMode = Schema.AccessMode.READ_ONLY)
    private Long dataSize;

    public OtaPackageInfo() {
        super();
    }

    public OtaPackageInfo(OtaPackageId id) {
        super(id);
    }

    public OtaPackageInfo(OtaPackageInfo otaPackageInfo) {
        super(otaPackageInfo);
        this.tenantId = otaPackageInfo.getTenantId();
        this.deviceProfileId = otaPackageInfo.getDeviceProfileId();
        this.type = otaPackageInfo.getType();
        this.title = otaPackageInfo.getTitle();
        this.version = otaPackageInfo.getVersion();
        this.tag = otaPackageInfo.getTag();
        this.url = otaPackageInfo.getUrl();
        this.hasData = otaPackageInfo.isHasData();
        this.fileName = otaPackageInfo.getFileName();
        this.contentType = otaPackageInfo.getContentType();
        this.checksumAlgorithm = otaPackageInfo.getChecksumAlgorithm();
        this.checksum = otaPackageInfo.getChecksum();
        this.dataSize = otaPackageInfo.getDataSize();
    }

    @Schema(description = "JSON object with the ota package Id. " +
            "Specify existing ota package Id to update the ota package. " +
            "Referencing non-existing ota package id will cause error. " +
            "Omit this field to create new ota package.")
    @Override
    public OtaPackageId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the ota package creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return title;
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

    @JsonIgnore
    public boolean hasUrl() {
        return StringUtils.isNotEmpty(url);
    }

    @Schema(description = "OTA Package description.", example = "Description for the OTA Package fw_1.0")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }
}
