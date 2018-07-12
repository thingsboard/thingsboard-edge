/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDeviceCredentialsDaoTest extends AbstractJpaDaoTest {

    @Autowired
    DeviceCredentialsDao deviceCredentialsDao;

    @Test
    @DatabaseSetup("classpath:dbunit/device_credentials.xml")
    public void testFindByDeviceId() {
        UUID deviceId = UUID.fromString("958e3a30-3215-11e7-93ae-92361f002671");
        DeviceCredentials deviceCredentials = deviceCredentialsDao.findByDeviceId(deviceId);
        assertNotNull(deviceCredentials);
        assertEquals("958e3314-3215-11e7-93ae-92361f002671", deviceCredentials.getId().getId().toString());
        assertEquals("ID_1", deviceCredentials.getCredentialsId());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/device_credentials.xml")
    public void findByCredentialsId() {
        String credentialsId = "ID_2";
        DeviceCredentials deviceCredentials = deviceCredentialsDao.findByCredentialsId(credentialsId);
        assertNotNull(deviceCredentials);
        assertEquals("958e3c74-3215-11e7-93ae-92361f002671", deviceCredentials.getId().getId().toString());
    }
}
