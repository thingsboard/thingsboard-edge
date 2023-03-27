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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceInfoDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JpaDeviceInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private DeviceInfoDao deviceInfoDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    @Autowired
    private CustomerDao customerDao;

    private List<Device> devices = new ArrayList<>();

    private Map<String, DeviceProfileId> savedDeviceProfiles = new HashMap<>();

    @After
    public void tearDown() {
        for (Device device : devices) {
            deviceDao.removeById(device.getTenantId(), device.getUuidId());
        }
        devices.clear();
        for (DeviceProfileId deviceProfileId : savedDeviceProfiles.values()) {
            deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, deviceProfileId.getId());
        }
        savedDeviceProfiles.clear();
    }

    @Test
    public void testFindDeviceInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            devices.add(createDevice(tenantId1, null, i));
            devices.add(createDevice(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "DEVICE");
        PageData<DeviceInfo> deviceInfos1 = deviceInfoDao.findDevicesByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, deviceInfos1.getData().size());

        PageData<DeviceInfo> devicesInfos2 = deviceInfoDao.findDevicesByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, devicesInfos2.getData().size());
    }

    @Test
    public void testFindDeviceInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            devices.add(createDevice(tenantId1, customer1.getUuidId(), i));
            devices.add(createDevice(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "DEVICE", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<DeviceInfo> deviceInfos1 = deviceInfoDao.findDevicesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, deviceInfos1.getData().size());
        deviceInfos1.getData().forEach(deviceInfo -> Assert.assertNotEquals("CUSTOMER_0", deviceInfo.getOwnerName()));

        PageData<DeviceInfo> deviceInfos2 = deviceInfoDao.findDevicesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, deviceInfos2.getData().size());

        PageData<DeviceInfo> deviceInfos3 = deviceInfoDao.findDevicesByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, deviceInfos3.getData().size());
    }

    private Device createDevice(UUID tenantId, UUID customerId, int index) {
        return this.createDevice(tenantId, customerId, null, index);
    }

    private Device createDevice(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        Device device = new Device();
        device.setId(new DeviceId(Uuids.timeBased()));
        device.setTenantId(TenantId.fromUUID(tenantId));
        device.setCustomerId(new CustomerId(customerId));
        device.setName("DEVICE_" + index);
        device.setType(type);
        device.setDeviceProfileId(deviceProfileId(type));
        return deviceDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, device);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

    private DeviceProfileId deviceProfileId(String type) {
        DeviceProfileId deviceProfileId = savedDeviceProfiles.get(type);
        if (deviceProfileId == null) {
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(type);
            deviceProfile.setTenantId(TenantId.SYS_TENANT_ID);
            deviceProfile.setDescription("Test");
            DeviceProfile savedDeviceProfile = deviceProfileDao.save(TenantId.SYS_TENANT_ID, deviceProfile);
            deviceProfileId = savedDeviceProfile.getId();
            savedDeviceProfiles.put(type, deviceProfileId);
        }
        return deviceProfileId;
    }

}
