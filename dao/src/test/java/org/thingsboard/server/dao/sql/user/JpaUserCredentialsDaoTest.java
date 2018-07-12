/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserCredentialsDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find();
        assertEquals(2, userCredentials.size());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindByUserId() {
        UserCredentials userCredentials = userCredentialsDao.findByUserId(UUID.fromString("787827e6-27d7-11e7-93ae-92361f002671"));
        assertNotNull(userCredentials);
        assertEquals("4b9e010c-27d5-11e7-93ae-92361f002671", userCredentials.getId().toString());
        assertEquals(true, userCredentials.isEnabled());
        assertEquals("password", userCredentials.getPassword());
        assertEquals("ACTIVATE_TOKEN_2", userCredentials.getActivateToken());
        assertEquals("RESET_TOKEN_2", userCredentials.getResetToken());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindByActivateToken() {
        UserCredentials userCredentials = userCredentialsDao.findByActivateToken("ACTIVATE_TOKEN_1");
        assertNotNull(userCredentials);
        assertEquals("3ed10af0-27d5-11e7-93ae-92361f002671", userCredentials.getId().toString());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindByResetToken() {
        UserCredentials userCredentials = userCredentialsDao.findByResetToken("RESET_TOKEN_2");
        assertNotNull(userCredentials);
        assertEquals("4b9e010c-27d5-11e7-93ae-92361f002671", userCredentials.getId().toString());
    }
}
