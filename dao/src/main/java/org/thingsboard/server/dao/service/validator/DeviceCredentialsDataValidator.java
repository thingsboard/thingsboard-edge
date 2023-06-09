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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class DeviceCredentialsDataValidator extends DataValidator<DeviceCredentials> {

    @Autowired
    private DeviceCredentialsDao deviceCredentialsDao;

    @Autowired
    private DeviceService deviceService;

    @Override
    protected void validateCreate(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentialsDao.findByDeviceId(tenantId, deviceCredentials.getDeviceId().getId()) != null) {
            throw new DeviceCredentialsValidationException("Credentials for this device are already specified!");
        }
        if (deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId()) != null) {
            throw new DeviceCredentialsValidationException("Device credentials are already assigned to another device!");
        }
    }

    @Override
    protected DeviceCredentials validateUpdate(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentialsDao.findById(tenantId, deviceCredentials.getUuidId()) == null) {
            throw new DeviceCredentialsValidationException("Unable to update non-existent device credentials!");
        }
        DeviceCredentials existingCredentials = deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId());
        if (existingCredentials != null && !existingCredentials.getId().equals(deviceCredentials.getId())) {
            throw new DeviceCredentialsValidationException("Device credentials are already assigned to another device!");
        }
        return existingCredentials;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getDeviceId() == null) {
            throw new DeviceCredentialsValidationException("Device credentials should be assigned to device!");
        }
        if (deviceCredentials.getCredentialsType() == null) {
            throw new DeviceCredentialsValidationException("Device credentials type should be specified!");
        }
        if (StringUtils.isEmpty(deviceCredentials.getCredentialsId())) {
            throw new DeviceCredentialsValidationException("Device credentials id should be specified!");
        }
        Device device = deviceService.findDeviceById(tenantId, deviceCredentials.getDeviceId());
        if (device == null) {
            throw new DeviceCredentialsValidationException("Can't assign device credentials to non-existent device!");
        }
    }
}
