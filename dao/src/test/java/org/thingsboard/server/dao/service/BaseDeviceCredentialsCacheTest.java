/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;

import static org.mockito.Mockito.*;

public abstract class BaseDeviceCredentialsCacheTest extends AbstractServiceTest {

    private static final String CREDENTIALS_ID_1 = RandomStringUtils.randomAlphanumeric(20);
    private static final String CREDENTIALS_ID_2 = RandomStringUtils.randomAlphanumeric(20);

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    private DeviceCredentialsDao deviceCredentialsDao;
    private DeviceService deviceService;

    @Autowired
    private CacheManager cacheManager;

    private UUID deviceId = UUID.randomUUID();

    @Before
    public void setup() throws Exception {
        deviceCredentialsDao = mock(DeviceCredentialsDao.class);
        deviceService = mock(DeviceService.class);
        ReflectionTestUtils.setField(unwrapDeviceCredentialsService(), "deviceCredentialsDao", deviceCredentialsDao);
        ReflectionTestUtils.setField(unwrapDeviceCredentialsService(), "deviceService", deviceService);
    }

    @After
    public void cleanup() {
        cacheManager.getCache(CacheConstants.DEVICE_CREDENTIALS_CACHE).clear();
    }

    @Test
    public void testFindDeviceCredentialsByCredentialsId_Cached() {
        when(deviceCredentialsDao.findByCredentialsId(CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(CREDENTIALS_ID_1);
    }

    @Test
    public void testDeleteDeviceCredentials_EvictsCache() {
        when(deviceCredentialsDao.findByCredentialsId(CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(CREDENTIALS_ID_1);

        deviceCredentialsService.deleteDeviceCredentials(createDummyDeviceCredentials(CREDENTIALS_ID_1, deviceId));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(2)).findByCredentialsId(CREDENTIALS_ID_1);
    }

    @Test
    public void testSaveDeviceCredentials_EvictsPreviousCache() throws Exception {
        when(deviceCredentialsDao.findByCredentialsId(CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(CREDENTIALS_ID_1);

        when(deviceCredentialsDao.findByDeviceId(deviceId)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        UUID deviceCredentialsId = UUID.randomUUID();
        when(deviceCredentialsDao.findById(deviceCredentialsId)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));
        when(deviceService.findDeviceById(new DeviceId(deviceId))).thenReturn(new Device());

        deviceCredentialsService.updateDeviceCredentials(createDummyDeviceCredentials(deviceCredentialsId, CREDENTIALS_ID_2, deviceId));

        when(deviceCredentialsDao.findByCredentialsId(CREDENTIALS_ID_1)).thenReturn(null);

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(3)).findByCredentialsId(CREDENTIALS_ID_1);
    }

    private DeviceCredentialsService unwrapDeviceCredentialsService() throws Exception {
        if (AopUtils.isAopProxy(deviceCredentialsService) && deviceCredentialsService instanceof Advised) {
            Object target = ((Advised) deviceCredentialsService).getTargetSource().getTarget();
            return (DeviceCredentialsService) target;
        }
        return null;
    }

    private DeviceCredentials createDummyDeviceCredentialsEntity(String deviceCredentialsId) {
        DeviceCredentials result = new DeviceCredentials(new DeviceCredentialsId(UUID.randomUUID()));
        result.setCredentialsId(deviceCredentialsId);
        return result;
    }

    private DeviceCredentials createDummyDeviceCredentials(String deviceCredentialsId, UUID deviceId) {
        return createDummyDeviceCredentials(null, deviceCredentialsId, deviceId);
    }

    private DeviceCredentials createDummyDeviceCredentials(UUID id, String deviceCredentialsId, UUID deviceId) {
        DeviceCredentials result = new DeviceCredentials();
        result.setId(new DeviceCredentialsId(id));
        result.setDeviceId(new DeviceId(deviceId));
        result.setCredentialsId(deviceCredentialsId);
        result.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        return result;
    }
}

