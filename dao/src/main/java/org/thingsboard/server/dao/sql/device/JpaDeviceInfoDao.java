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
package org.thingsboard.server.dao.sql.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceInfoDao;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaDeviceInfoDao extends JpaAbstractDao<DeviceInfoEntity, DeviceInfo> implements DeviceInfoDao {

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    @Override
    protected Class<DeviceInfoEntity> getEntityClass() {
        return DeviceInfoEntity.class;
    }

    @Override
    protected JpaRepository<DeviceInfoEntity, UUID> getRepository() {
        return deviceInfoRepository;
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink) {
        if (filter.getCustomerId() != null && filter.isIncludeCustomers()) {
            return DaoUtil.toPageData(
                deviceInfoRepository.findDeviceInfosByFilterIncludingSubCustomers(
                        filter.getTenantId().getId(),
                        filter.getCustomerId().getId(),
                        DaoUtil.getStringId(filter.getDeviceProfileId()),
                        filter.getActive() != null,
                        Boolean.TRUE.equals(filter.getActive()),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    deviceInfoRepository.findDeviceInfosByFilter(
                            filter.getTenantId().getId(),
                            filter.isIncludeCustomers(),
                            DaoUtil.getStringId(filter.getCustomerId()),
                            DaoUtil.getStringId(filter.getDeviceProfileId()),
                            filter.getActive() != null,
                            Boolean.TRUE.equals(filter.getActive()),
                            Objects.toString(pageLink.getTextSearch(), ""),
                            DaoUtil.toPageable(pageLink)));
        }
    }

}
