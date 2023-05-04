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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@DaoSqlTest
public class BaseUserServiceTest extends AbstractServiceTest {

    @Autowired
    CustomerService customerService;
    @Autowired
    UserService userService;

    private IdComparator<User> idComparator = new IdComparator<>();

    private UserSettings userSettings;

    @Before
    public void before() {
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant@thingsboard.org");
        userService.saveUser(tenantAdmin);

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customer@thingsboard.org");
        customerUser = userService.saveUser(customerUser);

        userSettings = createUserSettings(customerUser.getId());
    }

    @Test
    public void testFindUserByEmail() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID, "sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.SYS_ADMIN, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.TENANT_ADMIN, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "customer@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.CUSTOMER_USER, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "fake@thingsboard.org");
        Assert.assertNull(user);
    }

    @Test
    public void testFindUserById() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID, "sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        User foundUser = userService.findUserById(SYSTEM_TENANT_ID, user.getId());
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser);
    }

    @Test
    public void testFindUserCredentials() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID,"sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(SYSTEM_TENANT_ID, user.getId());
        Assert.assertNotNull(userCredentials);
    }

    @Test
    public void testSaveUser() {
        User tenantAdminUser = userService.findUserByEmail(SYSTEM_TENANT_ID,"tenant@thingsboard.org");
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantAdminUser.getTenantId());
        user.setEmail("tenant2@thingsboard.org");
        User savedUser = userService.saveUser(user);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(user.getEmail(), savedUser.getEmail());
        Assert.assertEquals(user.getTenantId(), savedUser.getTenantId());
        Assert.assertEquals(user.getAuthority(), savedUser.getAuthority());
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
        Assert.assertNotNull(userCredentials);
        Assert.assertNotNull(userCredentials.getId());
        Assert.assertNotNull(userCredentials.getUserId());
        Assert.assertNotNull(userCredentials.getActivateToken());

        savedUser.setFirstName("Joe");
        savedUser.setLastName("Downs");

        userService.saveUser(savedUser);
        savedUser = userService.findUserById(tenantId, savedUser.getId());
        Assert.assertEquals("Joe", savedUser.getFirstName());
        Assert.assertEquals("Downs", savedUser.getLastName());

        userService.deleteUser(tenantId, savedUser.getId());
    }

    @Test
    public void testSaveUserWithSameEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail("sysadmin@thingsboard.org");
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantAdminUser);
        });
    }

    @Test
    public void testSaveUserWithInvalidEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail("tenant_thingsboard.org");
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantAdminUser);
        });
    }

    @Test
    public void testSaveUserWithEmptyEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantAdminUser);
        });
    }

    @Test
    public void testSaveUserWithoutTenant() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setTenantId(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantAdminUser);
        });
    }

    @Test
    public void testDeleteUser() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantAdminUser.getTenantId());
        user.setEmail("tenant2@thingsboard.org");
        User savedUser = userService.saveUser(user);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        User foundUser = userService.findUserById(tenantId, savedUser.getId());
        Assert.assertNotNull(foundUser);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, foundUser.getId());
        Assert.assertNotNull(userCredentials);
        userService.deleteUser(tenantId, foundUser.getId());
        userCredentials = userService.findUserCredentialsByUserId(tenantId, foundUser.getId());
        foundUser = userService.findUserById(tenantId, foundUser.getId());
        Assert.assertNull(foundUser);
        Assert.assertNull(userCredentials);
    }

    @Test
    public void testFindTenantAdmins() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        PageData<User> pageData = userService.findTenantAdmins(tenantAdminUser.getTenantId(), new PageLink(10));
        Assert.assertFalse(pageData.hasNext());
        List<User> users = pageData.getData();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(tenantAdminUser, users.get(0));

        TenantId secondTenantId = createTenant();

        List<User> tenantAdmins = new ArrayList<>();
        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(secondTenantId);
            user.setEmail("testTenant" + i + "@thingsboard.org");
            tenantAdmins.add(userService.saveUser(user));
        }

        List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        do {
            pageData = userService.findTenantAdmins(secondTenantId, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        Assert.assertEquals(tenantAdmins, loadedTenantAdmins);

        tenantService.deleteTenant(secondTenantId);

        pageLink = new PageLink(33);
        pageData = userService.findTenantAdmins(secondTenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindTenantAdminsByEmail() {
        String email1 = "testEmail1";
        List<User> tenantAdminsEmail1 = new ArrayList<>();

        for (int i = 0; i < 94; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail1.add(userService.saveUser(user));
        }

        String email2 = "testEmail2";
        List<User> tenantAdminsEmail2 = new ArrayList<>();

        for (int i = 0; i < 132; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail2.add(userService.saveUser(user));
        }

        List<User> loadedTenantAdminsEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdminsEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail1, idComparator);
        Collections.sort(loadedTenantAdminsEmail1, idComparator);

        Assert.assertEquals(tenantAdminsEmail1, loadedTenantAdminsEmail1);

        List<User> loadedTenantAdminsEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        for (User user : loadedTenantAdminsEmail1) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedTenantAdminsEmail2) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerUsers() {
        User customerUser = userService.findUserByEmail(tenantId, "customer@thingsboard.org");
        PageData<User> pageData = userService.findCustomerUsers(customerUser.getTenantId(),
                customerUser.getCustomerId(), new PageLink(10));
        Assert.assertFalse(pageData.hasNext());
        List<User> users = pageData.getData();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(customerUser, users.get(0));

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        CustomerId customerId = customer.getId();

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 156; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(userService.saveUser(user));
        }

        List<User> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsers, idComparator);
        Collections.sort(loadedCustomerUsers, idComparator);

        Assert.assertEquals(customerUsers, loadedCustomerUsers);

        tenantService.deleteTenant(tenantId);

        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test
    public void testFindCustomerUsersByEmail() {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        CustomerId customerId = customer.getId();

        String email1 = "testEmail1";
        List<User> customerUsersEmail1 = new ArrayList<>();

        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail1.add(userService.saveUser(user));
        }

        String email2 = "testEmail2";
        List<User> customerUsersEmail2 = new ArrayList<>();

        for (int i = 0; i < 132; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail2.add(userService.saveUser(user));
        }

        List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsersEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail1, idComparator);
        Collections.sort(loadedCustomerUsersEmail1, idComparator);

        Assert.assertEquals(customerUsersEmail1, loadedCustomerUsersEmail1);

        List<User> loadedCustomerUsersEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsersEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail2, idComparator);
        Collections.sort(loadedCustomerUsersEmail2, idComparator);

        Assert.assertEquals(customerUsersEmail2, loadedCustomerUsersEmail2);

        for (User user : loadedCustomerUsersEmail1) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedCustomerUsersEmail2) {
            userService.deleteUser(tenantId, user.getId());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    private UserSettings createUserSettings(UserId userId) {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(userId);
        userSettings.setSettings(JacksonUtil.newObjectNode().put("text", StringUtils.randomAlphanumeric(10)));
        return userSettings;
    }
}
