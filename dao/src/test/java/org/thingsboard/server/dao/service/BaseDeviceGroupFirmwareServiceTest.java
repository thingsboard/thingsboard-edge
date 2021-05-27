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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.firmware.ChecksumAlgorithm;
import org.thingsboard.server.common.data.firmware.DeviceGroupFirmware;
import org.thingsboard.server.common.data.firmware.Firmware;
import org.thingsboard.server.common.data.firmware.FirmwareInfo;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.ColumnType;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import static org.thingsboard.server.common.data.firmware.FirmwareType.FIRMWARE;
import static org.thingsboard.server.common.data.firmware.FirmwareType.SOFTWARE;

public abstract class BaseDeviceGroupFirmwareServiceTest extends AbstractServiceTest {

    private static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final ChecksumAlgorithm CHECKSUM_ALGORITHM = ChecksumAlgorithm.SHA256;
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    private TenantId tenantId;
    private DeviceProfileId deviceProfileId;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    private FirmwareInfo createFirmware(String title, DeviceProfileId deviceProfileId) {
        Firmware firmware = new Firmware();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle(TITLE);
        firmware.setVersion(VERSION);
        firmware.setFileName(FILE_NAME);
        firmware.setContentType(CONTENT_TYPE);
        firmware.setChecksumAlgorithm(CHECKSUM_ALGORITHM);
        firmware.setChecksum(CHECKSUM);
        firmware.setData(DATA);
        Firmware savedFirmware = firmwareService.saveFirmware(firmware);
        Assert.assertNotNull(savedFirmware);
        return savedFirmware;
    }

    private Device createDevice(String name, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
        device.setDeviceProfileId(deviceProfileId);
        Device savedDevice = deviceService.saveDevice(device);
        Assert.assertNotNull(savedDevice);
        return savedDevice;
    }

    private EntityGroup createDeviceGroup(String name) {
        return createDeviceGroup(name, EntityType.DEVICE);
    }

    private EntityGroup createDeviceGroup(String name, EntityType groupType) {
        EntityGroup testDevicesGroup = new EntityGroup();
        testDevicesGroup.setType(groupType);
        testDevicesGroup.setName(name);
        testDevicesGroup.setOwnerId(tenantId);

        EntityGroupConfiguration entityGroupConfiguration = new EntityGroupConfiguration();

        entityGroupConfiguration.setColumns(Arrays.asList(
                new ColumnConfiguration(ColumnType.ENTITY_FIELD, EntityField.NAME.name().toLowerCase())
        ));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonConfiguration = mapper.valueToTree(entityGroupConfiguration);
        jsonConfiguration.putObject("settings");
        jsonConfiguration.putObject("actions");
        testDevicesGroup.setConfiguration(jsonConfiguration);
        EntityGroup savedGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, testDevicesGroup);
        Assert.assertNotNull(savedGroup);
        return savedGroup;
    }

    @Test
    public void testSaveDeviceGroupFirmware() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());
        deviceGroupFirmware.setGroupId(deviceGroup.getId());

        DeviceGroupFirmware savedDgf = deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
        Assert.assertNotNull(savedDgf);
        Assert.assertNotNull(savedDgf.getId());
        Assert.assertTrue(savedDgf.getFirmwareUpdateTime() > 0);
    }

    @Test
    public void testSaveDeviceGroupFirmwareWithEmptyDeviceGroup() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);

        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupFirmware should be assigned to entity group!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testSaveDeviceGroupFirmwareWithInvalidDeviceGroup() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setGroupId(new EntityGroupId(UUID.randomUUID()));
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Firmware is referencing to non-existent entity group!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testSaveGroupFirmwareWithInvalidEntityGroupType() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices", EntityType.ASSET);

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());
        deviceGroupFirmware.setGroupId(deviceGroup.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupFirmware can be only assigned to the Device group!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testSaveDeviceGroupFirmwareWithEmptyType() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupFirmware.setGroupId(deviceGroup.getId());
        deviceGroupFirmware.setFirmwareId(firmware.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Type should be specified!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testSaveDeviceGroupFirmwareWithEmptyFirmware() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupFirmware.setGroupId(deviceGroup.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupFirmware should be assigned to firmware!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testSaveDeviceGroupFirmwareWithInvalidFirmware() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupFirmware.setGroupId(deviceGroup.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());
        deviceGroupFirmware.setFirmwareId(new FirmwareId(UUID.randomUUID()));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Firmware is referencing to non-existent firmware!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testSaveDeviceGroupFirmwareWithInvalidFirmwareType() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupFirmware.setGroupId(deviceGroup.getId());
        deviceGroupFirmware.setFirmwareType(SOFTWARE);
        deviceGroupFirmware.setFirmwareId(firmware.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupFirmware firmware type should be the same as Firmware type!");
        deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
    }

    @Test
    public void testFindDeviceGroupFirmwareById() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());
        deviceGroupFirmware.setGroupId(deviceGroup.getId());

        DeviceGroupFirmware savedDgf = deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
        Assert.assertNotNull(savedDgf);

        DeviceGroupFirmware foundDfg = deviceGroupFirmwareService.findDeviceGroupFirmwareById(savedDgf.getId());
        Assert.assertNotNull(foundDfg);
        Assert.assertEquals(savedDgf, foundDfg);
    }

    @Test
    public void testFindDeviceGroupFirmwareByGroupIdAndFirmwareType() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());
        deviceGroupFirmware.setGroupId(deviceGroup.getId());

        DeviceGroupFirmware savedDgf = deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
        Assert.assertNotNull(savedDgf);

        DeviceGroupFirmware foundDfg = deviceGroupFirmwareService.findDeviceGroupFirmwareByGroupIdAndFirmwareType(deviceGroup.getId(), firmware.getType());
        Assert.assertNotNull(foundDfg);
        Assert.assertEquals(savedDgf, foundDfg);
    }

    @Test
    public void testDeleteDeviceGroupFirmware() {
        FirmwareInfo firmware = createFirmware(TITLE, deviceProfileId);
        Device device = createDevice("Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup("Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupFirmware deviceGroupFirmware = new DeviceGroupFirmware();
        deviceGroupFirmware.setFirmwareId(firmware.getId());
        deviceGroupFirmware.setFirmwareType(firmware.getType());
        deviceGroupFirmware.setGroupId(deviceGroup.getId());

        DeviceGroupFirmware savedDgf = deviceGroupFirmwareService.saveDeviceGroupFirmware(tenantId, deviceGroupFirmware);
        Assert.assertNotNull(savedDgf);

        deviceGroupFirmwareService.deleteDeviceGroupFirmware(savedDgf.getId());

        DeviceGroupFirmware foundDfg = deviceGroupFirmwareService.findDeviceGroupFirmwareById(savedDgf.getId());
        Assert.assertNull(foundDfg);
    }

}
