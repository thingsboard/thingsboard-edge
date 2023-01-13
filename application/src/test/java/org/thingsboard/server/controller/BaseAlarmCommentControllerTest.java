/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ContextConfiguration(classes = {BaseAlarmCommentControllerTest.Config.class})
public abstract class BaseAlarmCommentControllerTest extends AbstractControllerTest {

    protected final String CUSTOMER_ADMIN_EMAIL = "testadmincustomer@thingsboard.org";
    protected final String CUSTOMER_ADMIN_PASSWORD = "admincustomer";

    protected final String DIFFERENT_CUSTOMER_ADMIN_EMAIL = "testdiffadmincustomer@thingsboard.org";
    protected final String DIFFERENT_CUSTOMER_ADMIN_PASSWORD = "diffadmincustomer";

    protected Device customerDevice;
    protected Alarm alarm;
    private Role role;
    private EntityGroup entityGroup;
    private GroupPermission groupPermission;
    private final String classNameAlarm = "ALARM";


    static class Config {
        @Bean
        @Primary
        public AlarmDao alarmDao(AlarmDao alarmDao) {
            return Mockito.mock(AlarmDao.class, AdditionalAnswers.delegatesTo(alarmDao));
        }
    }

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("Type");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type("test alarm type")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setType(RoleType.GENERIC);
        role.setName("Test customer administrator");
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));

        this.role = doPost("/api/role", role, Role.class);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Test customer administrators");
        entityGroup.setType(EntityType.USER);
        entityGroup.setOwnerId(customerId);
        this.entityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        GroupPermission groupPermission = new GroupPermission(
                tenantId,
                this.entityGroup.getId(),
                this.role.getId(),
                null,
                null,
                false
        );
        this.groupPermission =
                doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        resetTokens();
    }

    @After
    public void teardown() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);
        loginSysAdmin();
        deleteDifferentTenant();
        clearCustomerAdminPermissionGroup();
    }

    @Test
    public void testCreateAlarmCommentViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());

        testLogEntityAction(alarm, alarm.getId(), tenantId, customerId, customerAdminUserId, CUSTOMER_ADMIN_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
    }

    @Test
    public void testCreateAlarmCommentViaCustomerWithoutPermission() throws Exception {
        loginCustomerUser();

        AlarmComment alarmComment = AlarmComment.builder()
                .comment(JacksonUtil.newObjectNode().set("text", new TextNode(RandomStringUtils.randomAlphanumeric(10))))
                .build();

        doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));
    }

    @Test
    public void testCreateAlarmCommentViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        AlarmComment createdComment = createAlarmComment(alarm.getId());
        Assert.assertEquals(AlarmCommentType.OTHER, createdComment.getType());

        testLogEntityAction(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED_COMMENT, 1, createdComment);
    }

    @Test
    public void testUpdateAlarmCommentViaCustomerWithPermission() throws Exception {
        loginCustomerAdministrator();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityAction(alarm, alarm.getId(), tenantId, customerId, customerAdminUserId, CUSTOMER_ADMIN_EMAIL, ActionType.UPDATED_COMMENT, 1, updatedAlarmComment);
    }

    @Test
    public void testUpdateAlarmCommentViaCustomerWithoutPermission() throws Exception {
        loginCustomerAdministrator();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginCustomerUser();
        doPost("/api/alarm/" + alarm.getId() + "/comment", alarmComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));
    }

    @Test
    public void testUpdateAlarmCommentViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);
        AlarmComment updatedAlarmComment = saveAlarmComment(alarm.getId(), savedComment);

        Assert.assertNotNull(updatedAlarmComment);
        Assert.assertEquals(newComment.get("text"), updatedAlarmComment.getComment().get("text"));
        Assert.assertEquals("true", updatedAlarmComment.getComment().get("edited").asText());
        Assert.assertNotNull(updatedAlarmComment.getComment().get("editedOn"));

        testLogEntityAction(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED_COMMENT, 1, savedComment);
    }

    @Test
    public void testUpdateAlarmCommentViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    @Test
    public void testUpdateAlarmCommentViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        AlarmComment savedComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);
        JsonNode newComment = JacksonUtil.newObjectNode().set("text", new TextNode("Updated comment"));
        savedComment.setComment(newComment);

        doPost("/api/alarm/" + alarm.getId() + "/comment", savedComment)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));

        loginDifferentCustomerAdministrator();
        doPost("/api/alarm", alarm)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + classNameAlarm + " '" + alarm.getType() +"'!")));

        testNotifyEntityNever(alarm.getId(), savedComment);
    }

    @Test
    public void testDeleteAlarmCommentViaCustomerWithPermission() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginCustomerAdministrator();
        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        testLogEntityAction(alarm, alarm.getId(), tenantId, customerId, customerAdminUserId, CUSTOMER_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, alarmComment);
    }

    @Test
    public void testDeleteAlarmCommentViaCustomerWithoutPermission() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginCustomerUser();
        doDelete("/api/alarm/" + alarm.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + classNameAlarm + " '" + alarm.getType() +"'!")));
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isOk());

        testLogEntityAction(alarm, alarm.getId(), tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.DELETED_COMMENT, 1, alarmComment);
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + classNameAlarm + " '" + alarm.getType() +"'!")));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testDeleteAlarmCommentViaDifferentCustomer() throws Exception {
        loginTenantAdmin();
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId() + "/comment/" + alarmComment.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + classNameAlarm + " '" + alarm.getType() +"'!")));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    public void testFindAlarmCommentsViaCustomerUser() throws Exception {
        loginCustomerAdministrator();

        List<AlarmComment> createdAlarmComments = new LinkedList<>();

        final int size = 10;
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.randomAlphanumeric(10))
            );
        }

        var response = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size,
                new TypeReference<PageData<AlarmCommentInfo>>() {}
        );
        var foundAlarmCommentInfos = response.getData();
        Assert.assertNotNull("Found pageData is null", foundAlarmCommentInfos);
        Assert.assertNotEquals(
                "Expected alarms are not found!",
                0, foundAlarmCommentInfos.size()
        );

        boolean allMatch = createdAlarmComments.stream()
                .allMatch(alarmComment -> foundAlarmCommentInfos.stream()
                        .map(AlarmCommentInfo::getComment)
                        .anyMatch(comment -> alarmComment.getComment().equals(comment))
                );
        Assert.assertTrue("Created alarm comment doesn't match any found!", allMatch);
    }

    @Test
    public void testFindAlarmCommentssViaDifferentCustomerUser() throws Exception {
        loginCustomerAdministrator();

        final int size = 10;
        List<AlarmComment> createdAlarmComments = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            createdAlarmComments.add(
                    createAlarmComment(alarm.getId(), RandomStringUtils.randomAlphanumeric(10))
            );
        }

        loginDifferentCustomer();
        doGet("/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "'" + classNameAlarm + "' resource!")));

        loginDifferentCustomerAdministrator();
        doGet("/api/alarm/" + alarm.getId() + "/comment?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "DEVICE" + " '" + customerDevice.getName() + "'!")));
    }

    @Test
    public void testFindAlarmCommentsViaPublicCustomer() throws Exception {
        loginCustomerAdministrator();

        EntityGroupInfo deviceGroup = createSharedPublicEntityGroup(
                "Device Test Entity Group",
                EntityType.DEVICE,
                customerId
        );
        String publicId = deviceGroup.getAdditionalInfo().get("publicCustomerId").asText();

        Device device = new Device();
        device.setName("Test Public Device");
        device.setLabel("Label");
        device.setCustomerId(customerId);
        device = doPost("/api/device?entityGroupId=" + deviceGroup.getUuidId(), device, Device.class);


        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(device.getId())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type("Test")
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull("Saved alarm is null!", alarm);
        AlarmComment alarmComment = createAlarmComment(alarm.getId());

        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        PageData<AlarmCommentInfo> pageData = doGetTyped(
                "/api/alarm/" + alarm.getId() + "/comment" + "?page=0&pageSize=1", new TypeReference<PageData<AlarmCommentInfo>>() {}
        );

        Assert.assertNotNull("Found pageData is null", pageData);
        Assert.assertNotEquals("Expected alarm comments are not found!", 0, pageData.getTotalElements());

        AlarmCommentInfo alarmCommentInfo = pageData.getData().get(0);
        boolean equals = alarmComment.getId().equals(alarmCommentInfo.getId()) && alarmComment.getComment().equals(alarmCommentInfo.getComment());
        Assert.assertTrue("Created alarm doesn't match the found one!", equals);
    }

    private AlarmComment createAlarmComment(AlarmId alarmId, String text)  {
        AlarmComment alarmComment = AlarmComment.builder()
                .comment(JacksonUtil.newObjectNode().set("text", new TextNode(text)))
                .build();

        return saveAlarmComment(alarmId, alarmComment);
    }
    private AlarmComment createAlarmComment(AlarmId alarmId)  {
        return createAlarmComment(alarmId, "Please take a look");
    }
    private AlarmComment saveAlarmComment(AlarmId alarmId, AlarmComment alarmComment) {
        alarmComment = doPost("/api/alarm/" + alarmId + "/comment", alarmComment, AlarmComment.class);
        Assert.assertNotNull(alarmComment);

        return alarmComment;
    }

    private void clearCustomerAdminPermissionGroup() throws Exception {
        loginTenantAdmin();
        doDelete("/api/groupPermission/" + groupPermission.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/entityGroup/" + entityGroup.getUuidId())
                .andExpect(status().isOk());
        doDelete("/api/role/" + role.getUuidId())
                .andExpect(status().isOk());
    }

    private User savedCustomerAdministrator;
    private User savedDifferentCustomerAdministrator;

    private void loginCustomerAdministrator() throws Exception {
        if (savedCustomerAdministrator == null) {
            savedCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    customerId,
                    CUSTOMER_ADMIN_EMAIL,
                    CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedCustomerAdministrator.getEmail(), CUSTOMER_ADMIN_PASSWORD);
    }

    private void loginDifferentCustomerAdministrator() throws Exception {
        if (savedDifferentCustomerAdministrator == null) {
            if (differentCustomerId == null) {
                createDifferentCustomer();
            }

            savedDifferentCustomerAdministrator = createCustomerAdministrator(
                    tenantId,
                    differentCustomerId,
                    DIFFERENT_CUSTOMER_ADMIN_EMAIL,
                    DIFFERENT_CUSTOMER_ADMIN_PASSWORD
            );
        }
        login(savedDifferentCustomerAdministrator.getEmail(), DIFFERENT_CUSTOMER_ADMIN_PASSWORD);
    }

    private User createCustomerAdministrator(TenantId tenantId, CustomerId customerId, String email, String pass) throws Exception {
        loginTenantAdmin();

        User user = new User();
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setFirstName("customer");
        user.setLastName("admin");
        user.setAuthority(Authority.CUSTOMER_USER);

        user = createUser(user, pass, entityGroup.getId());
        customerAdminUserId = user.getId();
        resetTokens();

        return user;
    }

    private EntityGroupInfo createSharedPublicEntityGroup(String name, EntityType entityType, EntityId ownerId) throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(name);
        entityGroup.setType(entityType);
        EntityGroupInfo groupInfo =
                doPostWithResponse("/api/entityGroup", entityGroup, EntityGroupInfo.class);

        ShareGroupRequest groupRequest = new ShareGroupRequest(
                ownerId,
                true,
                null,
                true,
                null
        );

        doPost("/api/entityGroup/" + groupInfo.getId() + "/share", groupRequest)
                .andExpect(status().isOk());

        doPost("/api/entityGroup/" + groupInfo.getId() + "/makePublic")
                .andExpect(status().isOk());
        return doGet("/api/entityGroup/" + groupInfo.getUuidId(), EntityGroupInfo.class);
    }
}
