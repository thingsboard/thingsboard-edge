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
package org.thingsboard.server.dao.sql.firmware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.FirmwareInfo;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.firmware.FirmwareInfoDao;
import org.thingsboard.server.dao.model.sql.FirmwareInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class JpaFirmwareInfoDao extends JpaAbstractSearchTextDao<FirmwareInfoEntity, FirmwareInfo> implements FirmwareInfoDao {

    @Autowired
    private FirmwareInfoRepository firmwareInfoRepository;

    @Override
    protected Class<FirmwareInfoEntity> getEntityClass() {
        return FirmwareInfoEntity.class;
    }

    @Override
    protected CrudRepository<FirmwareInfoEntity, UUID> getCrudRepository() {
        return firmwareInfoRepository;
    }

    @Override
    public FirmwareInfo findById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(firmwareInfoRepository.findFirmwareInfoById(id));
    }

    @Override
    public FirmwareInfo save(TenantId tenantId, FirmwareInfo firmwareInfo) {
        FirmwareInfo savedFirmware = super.save(tenantId, firmwareInfo);
        if (firmwareInfo.getId() == null) {
            return savedFirmware;
        } else {
            return findById(tenantId, savedFirmware.getId().getId());
        }
    }

    @Override
    public PageData<FirmwareInfo> findFirmwareInfoByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(firmwareInfoRepository
                .findAllByTenantId(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<FirmwareInfo> findFirmwareInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, FirmwareType firmwareType, boolean hasData, PageLink pageLink) {
        return DaoUtil.toPageData(firmwareInfoRepository
                .findAllByTenantIdAndTypeAndDeviceProfileIdAndHasData(
                        tenantId.getId(),
                        deviceProfileId.getId(),
                        firmwareType,
                        hasData,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean isFirmwareUsed(FirmwareId firmwareId, FirmwareType type, DeviceProfileId deviceProfileId) {
        return firmwareInfoRepository.isFirmwareUsed(firmwareId.getId(), deviceProfileId.getId(), type.name());
    }
}
