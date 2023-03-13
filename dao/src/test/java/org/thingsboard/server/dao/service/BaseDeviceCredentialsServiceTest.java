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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.exception.DataValidationException;

public abstract class BaseDeviceCredentialsServiceTest extends AbstractBeforeTest {

    @Autowired
    DeviceCredentialsService deviceCredentialsService;
    @Autowired
    DeviceService deviceService;

    @Test
    public void testCreateDeviceCredentials() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        });
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setDeviceId(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsType() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsType(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsId() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsId(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveNonExistentDeviceCredentials() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        DeviceCredentials newDeviceCredentials = new DeviceCredentials(new DeviceCredentialsId(Uuids.timeBased()));
        newDeviceCredentials.setCreatedTime(deviceCredentials.getCreatedTime());
        newDeviceCredentials.setDeviceId(deviceCredentials.getDeviceId());
        newDeviceCredentials.setCredentialsType(deviceCredentials.getCredentialsType());
        newDeviceCredentials.setCredentialsId(deviceCredentials.getCredentialsId());
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, newDeviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveDeviceCredentialsWithNonExistentDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setDeviceId(new DeviceId(Uuids.timeBased()));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testFindDeviceCredentialsByDeviceId() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNull(deviceCredentials);
    }

    @Test
    public void testFindDeviceCredentialsByCredentialsId() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(deviceCredentials.getCredentialsId());
        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(deviceCredentials.getCredentialsId());
        Assert.assertNull(foundDeviceCredentials);
    }

    @Test
    public void testSaveDeviceCredentials() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");
        deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }
}

