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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.firmware.ChecksumAlgorithm;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_CHECKSUM_ALGORITHM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_CHECKSUM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_CONTENT_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_DATA_SIZE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_DEVICE_PROFILE_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_FILE_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TITLE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.FIRMWARE_VERSION_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = FIRMWARE_TABLE_NAME)
public class FirmwareInfoEntity extends BaseSqlEntity<FirmwareInfo> implements SearchTextEntity<FirmwareInfo> {

    @Column(name = FIRMWARE_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = FIRMWARE_DEVICE_PROFILE_ID_COLUMN)
    private UUID deviceProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = FIRMWARE_TYPE_COLUMN)
    private FirmwareType type;

    @Column(name = FIRMWARE_TITLE_COLUMN)
    private String title;

    @Column(name = FIRMWARE_VERSION_COLUMN)
    private String version;

    @Column(name = FIRMWARE_FILE_NAME_COLUMN)
    private String fileName;

    @Column(name = FIRMWARE_CONTENT_TYPE_COLUMN)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = FIRMWARE_CHECKSUM_ALGORITHM_COLUMN)
    private ChecksumAlgorithm checksumAlgorithm;

    @Column(name = FIRMWARE_CHECKSUM_COLUMN)
    private String checksum;

    @Column(name = FIRMWARE_DATA_SIZE_COLUMN)
    private Long dataSize;

    @Type(type = "json")
    @Column(name = ModelConstants.FIRMWARE_ADDITIONAL_INFO_COLUMN)
    private JsonNode additionalInfo;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Transient
    private boolean hasData;

    public FirmwareInfoEntity() {
        super();
    }

    public FirmwareInfoEntity(FirmwareInfo firmware) {
        this.createdTime = firmware.getCreatedTime();
        this.setUuid(firmware.getUuidId());
        this.tenantId = firmware.getTenantId().getId();
        this.type = firmware.getType();
        if (firmware.getDeviceProfileId() != null) {
            this.deviceProfileId = firmware.getDeviceProfileId().getId();
        }
        this.title = firmware.getTitle();
        this.version = firmware.getVersion();
        this.fileName = firmware.getFileName();
        this.contentType = firmware.getContentType();
        this.checksumAlgorithm = firmware.getChecksumAlgorithm();
        this.checksum = firmware.getChecksum();
        this.dataSize = firmware.getDataSize();
        this.additionalInfo = firmware.getAdditionalInfo();
    }

    public FirmwareInfoEntity(UUID id, long createdTime, UUID tenantId, UUID deviceProfileId, FirmwareType type, String title, String version,
                              String fileName, String contentType, ChecksumAlgorithm checksumAlgorithm, String checksum, Long dataSize,
                              Object additionalInfo, boolean hasData) {
        this.id = id;
        this.createdTime = createdTime;
        this.tenantId = tenantId;
        this.deviceProfileId = deviceProfileId;
        this.type = type;
        this.title = title;
        this.version = version;
        this.fileName = fileName;
        this.contentType = contentType;
        this.checksumAlgorithm = checksumAlgorithm;
        this.checksum = checksum;
        this.dataSize = dataSize;
        this.hasData = hasData;
        this.additionalInfo = JacksonUtil.convertValue(additionalInfo, JsonNode.class);
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public FirmwareInfo toData() {
        FirmwareInfo firmware = new FirmwareInfo(new FirmwareId(id));
        firmware.setCreatedTime(createdTime);
        firmware.setTenantId(new TenantId(tenantId));
        if (deviceProfileId != null) {
            firmware.setDeviceProfileId(new DeviceProfileId(deviceProfileId));
        }
        firmware.setType(type);
        firmware.setTitle(title);
        firmware.setVersion(version);
        firmware.setFileName(fileName);
        firmware.setContentType(contentType);
        firmware.setChecksumAlgorithm(checksumAlgorithm);
        firmware.setChecksum(checksum);
        firmware.setDataSize(dataSize);
        firmware.setAdditionalInfo(additionalInfo);
        firmware.setHasData(hasData);
        return firmware;
    }
}
