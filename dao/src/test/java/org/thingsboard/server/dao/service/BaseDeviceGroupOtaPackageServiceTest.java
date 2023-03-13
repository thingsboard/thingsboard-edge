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

import lombok.Getter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

public abstract class BaseDeviceGroupOtaPackageServiceTest extends AbstractServiceTest {

    private static final String TITLE = "My firmware";
    private static final String FILE_NAME = "filename.txt";
    private static final String VERSION = "v1.0";
    private static final String CONTENT_TYPE = "text/plain";
    private static final ChecksumAlgorithm CHECKSUM_ALGORITHM = ChecksumAlgorithm.SHA256;
    private static final String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";
    private static final ByteBuffer DATA = ByteBuffer.wrap(new byte[]{1});

    @Getter
    @Autowired
    EntityGroupService entityGroupService;
    @Autowired
    DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    @Autowired
    DeviceProfileService deviceProfileService;
    @Getter
    @Autowired
    DeviceService deviceService;
    @Autowired
    OtaPackageService otaPackageService;

    private DeviceProfileId deviceProfileId;

    @SuppressWarnings("deprecation")
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        DeviceProfile deviceProfile = this.createDeviceProfile(tenantId, "Device Profile");
        DeviceProfile savedDeviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedDeviceProfile);
        deviceProfileId = savedDeviceProfile.getId();
    }

    private OtaPackageInfo createOtaPackage(String title, DeviceProfileId deviceProfileId) {
        OtaPackage firmware = new OtaPackage();
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
        firmware.setDataSize((long) DATA.capacity());
        OtaPackage savedOtaPackage = otaPackageService.saveOtaPackage(firmware);
        Assert.assertNotNull(savedOtaPackage);
        return savedOtaPackage;
    }

    @Test
    public void testSaveDeviceGroupOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);
        Assert.assertNotNull(savedDgf.getId());
        Assert.assertTrue(savedDgf.getOtaPackageUpdateTime() > 0);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithEmptyDeviceGroup() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupOtaPackage should be assigned to entity group!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithInvalidDeviceGroup() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setGroupId(new EntityGroupId(UUID.randomUUID()));
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("OtaPackage is referencing to non-existent entity group!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testSaveGroupOtaPackageWithInvalidEntityGroupType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createEntityGroup(tenantId, EntityType.ASSET, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupOtaPackage can be only assigned to the Device group!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithEmptyType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("Type should be specified!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithEmptyOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupOtaPackage should be assigned to OtaPackage!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithInvalidOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setOtaPackageId(new OtaPackageId(UUID.randomUUID()));

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupOtaPackage is referencing to non-existent OtaPackage!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testSaveDeviceGroupOtaPackageWithInvalidOtaPackageType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");
        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(SOFTWARE);
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());

        thrown.expect(DataValidationException.class);
        thrown.expectMessage("DeviceGroupOtaPackage type should be the same as OtaPackage type!");
        deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
    }

    @Test
    public void testFindDeviceGroupOtaPackageById() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);

        DeviceGroupOtaPackage foundDfg = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(savedDgf.getId());
        Assert.assertNotNull(foundDfg);
        Assert.assertEquals(savedDgf, foundDfg);
    }

    @Test
    public void testFindDeviceGroupOtaPackageByGroupIdAndOtaPackageType() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);

        DeviceGroupOtaPackage foundDfg = deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(deviceGroup.getId(), firmware.getType());
        Assert.assertNotNull(foundDfg);
        Assert.assertEquals(savedDgf, foundDfg);
    }

    @Test
    public void testDeleteDeviceGroupOtaPackage() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        DeviceGroupOtaPackage savedDgf = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        Assert.assertNotNull(savedDgf);

        deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(savedDgf.getId());

        DeviceGroupOtaPackage foundDfg = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(savedDgf.getId());
        Assert.assertNull(foundDfg);
    }

    @Test
    public void testDeviceGroupOtaPackageDeletionOnDeleteOta() {
        OtaPackageInfo firmware = createOtaPackage(TITLE, deviceProfileId);
        Device device = createDevice(tenantId, "Test device", deviceProfileId);
        EntityGroup deviceGroup = createDeviceGroup(tenantId, "Test devices");

        entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());
        deviceGroupOtaPackage.setOtaPackageType(firmware.getType());
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());

        deviceGroupOtaPackage = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);

        assertThat(otaPackageService.findOtaPackageById(tenantId, firmware.getId())).isNotNull();
        assertThat(deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId())).isNotNull();

        otaPackageService.deleteOtaPackage(tenantId, firmware.getId());

        assertThat(deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId())).isNull();
    }

}
