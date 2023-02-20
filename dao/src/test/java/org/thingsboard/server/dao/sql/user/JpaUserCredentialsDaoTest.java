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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserCredentialsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    public static final String ACTIVATE_TOKEN = "ACTIVATE_TOKEN_0";
    public static final String RESET_TOKEN = "RESET_TOKEN_0";
    public static final int COUNT_USER_CREDENTIALS = 2;
    List<UserCredentials> userCredentialsList;
    UserCredentials neededUserCredentials;

    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Before
    public void setUp() {
        userCredentialsList = new ArrayList<>();
        for (int i=0; i<COUNT_USER_CREDENTIALS; i++) {
            userCredentialsList.add(createUserCredentials(i));
        }
        neededUserCredentials = userCredentialsList.get(0);
        assertNotNull(neededUserCredentials);
    }

    UserCredentials createUserCredentials(int number) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        userCredentials.setUserId(new UserId(UUID.randomUUID()));
        userCredentials.setPassword("password");
        userCredentials.setActivateToken("ACTIVATE_TOKEN_" + number);
        userCredentials.setResetToken("RESET_TOKEN_" + number);
        return userCredentialsDao.save(SYSTEM_TENANT_ID, userCredentials);
    }

    @After
    public void after() {
        for (UserCredentials userCredentials : userCredentialsList) {
            userCredentialsDao.removeById(TenantId.SYS_TENANT_ID, userCredentials.getUuidId());
        }
    }

    @Test
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find(SYSTEM_TENANT_ID);
        assertEquals(COUNT_USER_CREDENTIALS + 1, userCredentials.size());
    }

    @Test
    public void testFindByUserId() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByUserId(SYSTEM_TENANT_ID, neededUserCredentials.getUserId().getId());
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials, foundedUserCredentials);
    }

    @Test
    public void testFindByActivateToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByActivateToken(SYSTEM_TENANT_ID, ACTIVATE_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }

    @Test
    public void testFindByResetToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByResetToken(SYSTEM_TENANT_ID, RESET_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }
}
