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
package org.thingsboard.server.dao.sql.user;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserSettings;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserSettingsDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

public class JpaUserSettingsDaoTest extends AbstractJpaDaoTest {

    private UUID tenantId;
    private User user;

    @Autowired
    private UserSettingsDao userSettingsDao;

    @Autowired
    private UserDao userDao;

    @Before
    public void setUp() {
        tenantId = Uuids.timeBased();
        user = saveUser(tenantId, Uuids.timeBased());
    }

    @After
    public void tearDown() {
        userDao.removeById(user.getTenantId(), user.getUuidId());
    }

    @Test
    public void testFindSettingsByUserId() {
        UserSettings userSettings = createUserSettings(user.getId());

        UserSettings retrievedUserSettings = userSettingsDao.findById(SYSTEM_TENANT_ID, user.getId());
        assertEquals(retrievedUserSettings.getSettings(), userSettings.getSettings());

        userSettingsDao.removeById(SYSTEM_TENANT_ID, user.getId());

        UserSettings retrievedUserSettings2 = userSettingsDao.findById(SYSTEM_TENANT_ID, user.getId());
        assertNull(retrievedUserSettings2);
    }

    private UserSettings createUserSettings(UserId userId) {
        UserSettings userSettings = new UserSettings();
        userSettings.setSettings(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        userSettings.setUserId(userId);
        return userSettingsDao.save(SYSTEM_TENANT_ID, userSettings);
    }

    private User saveUser(UUID tenantId, UUID customerId) {
        User user = new User();
        UUID id = Uuids.timeBased();
        user.setId(new UserId(id));
        user.setTenantId(TenantId.fromUUID(tenantId));
        user.setCustomerId(new CustomerId(customerId));
        if (customerId == NULL_UUID) {
            user.setAuthority(Authority.TENANT_ADMIN);
        } else {
            user.setAuthority(Authority.CUSTOMER_USER);
        }
        String idString = id.toString();
        String email = idString.substring(0, idString.indexOf('-')) + "@thingsboard.org";
        user.setEmail(email);
        return  userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }
}
