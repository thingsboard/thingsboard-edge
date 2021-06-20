/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class OtaPackageInfo extends SearchTextBasedWithAdditionalInfo<OtaPackageId> implements TenantEntity, HasName {

    private static final long serialVersionUID = 3168391583570815419L;

    private TenantId tenantId;
    private DeviceProfileId deviceProfileId;
    private OtaPackageType type;
    private String title;
    private String version;
    private String url;
    private boolean hasData;
    private String fileName;
    private String contentType;
    private ChecksumAlgorithm checksumAlgorithm;
    private String checksum;
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
        this.url = otaPackageInfo.getUrl();
        this.hasData = otaPackageInfo.isHasData();
        this.fileName = otaPackageInfo.getFileName();
        this.contentType = otaPackageInfo.getContentType();
        this.checksumAlgorithm = otaPackageInfo.getChecksumAlgorithm();
        this.checksum = otaPackageInfo.getChecksum();
        this.dataSize = otaPackageInfo.getDataSize();
    }

    @Override
    public String getSearchText() {
        return title;
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
}
