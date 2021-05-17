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
package org.thingsboard.server.common.data.firmware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class FirmwareInfo extends SearchTextBasedWithAdditionalInfo<FirmwareId> implements TenantEntity, HasName {

    private static final long serialVersionUID = 3168391583570815419L;

    private TenantId tenantId;
    private DeviceProfileId deviceProfileId;
    private FirmwareType type;
    private String title;
    private String version;
    private boolean hasData;
    private String fileName;
    private String contentType;
    private String checksumAlgorithm;
    private String checksum;
    private Long dataSize;


    public FirmwareInfo() {
        super();
    }

    public FirmwareInfo(FirmwareId id) {
        super(id);
    }

    public FirmwareInfo(FirmwareInfo firmwareInfo) {
        super(firmwareInfo);
        this.tenantId = firmwareInfo.getTenantId();
        this.deviceProfileId = firmwareInfo.getDeviceProfileId();
        this.type = firmwareInfo.getType();
        this.title = firmwareInfo.getTitle();
        this.version = firmwareInfo.getVersion();
        this.hasData = firmwareInfo.isHasData();
        this.fileName = firmwareInfo.getFileName();
        this.contentType = firmwareInfo.getContentType();
        this.checksumAlgorithm = firmwareInfo.getChecksumAlgorithm();
        this.checksum = firmwareInfo.getChecksum();
        this.dataSize = firmwareInfo.getDataSize();
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
        return EntityType.FIRMWARE;
    }
}
