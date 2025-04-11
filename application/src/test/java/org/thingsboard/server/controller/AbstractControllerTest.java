/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.ShareGroupRequest;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AbstractControllerTest.class, loader = SpringBootContextLoader.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Configuration
@ComponentScan({"org.thingsboard.server"})
@EnableWebSocket
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public abstract class AbstractControllerTest extends AbstractNotifyEntityTest {

    public static final String WS_URL = "ws://localhost:";

    @LocalServerPort
    protected int wsPort;

    protected volatile TbTestWebSocketClient wsClient; // lazy
    protected volatile TbTestWebSocketClient anotherWsClient; // lazy

    public TbTestWebSocketClient getWsClient() {
        if (wsClient == null) {
            synchronized (this) {
                try {
                    if (wsClient == null) {
                        wsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return wsClient;
    }

    public TbTestWebSocketClient getAnotherWsClient() {
        if (anotherWsClient == null) {
            synchronized (this) {
                try {
                    if (anotherWsClient == null) {
                        anotherWsClient = buildAndConnectWebSocketClient();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return anotherWsClient;
    }

    @Before
    public void beforeWsTest() throws Exception {
        // placeholder
    }

    @After
    public void afterWsTest() throws Exception {
        if (wsClient != null) {
            wsClient.close();
        }
        if (anotherWsClient != null) {
            anotherWsClient.close();
        }
    }

    protected TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        return buildAndConnectWebSocketClient("/api/ws");
    }

    protected TbTestWebSocketClient buildAndConnectWebSocketClient(String path) throws URISyntaxException, InterruptedException {
        TbTestWebSocketClient wsClient = new TbTestWebSocketClient(new URI(WS_URL + wsPort + path));
        assertThat(wsClient.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        if (!path.contains("token=")) {
            wsClient.authenticate(token);
        }
        return wsClient;
    }

    protected void resetSysAdminWhiteLabelingSettings() throws Exception {
        loginSysAdmin();

        doPost("/api/whiteLabel/loginWhiteLabelParams", new LoginWhiteLabelingParams(), LoginWhiteLabelingParams.class);
        doPost("/api/whiteLabel/whiteLabelParams", new WhiteLabelingParams(), WhiteLabelingParams.class);
    }

    protected EntityGroupInfo createSharedPublicEntityGroup(String name, EntityType entityType, EntityId ownerId) throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(name);
        entityGroup.setType(entityType);
        EntityGroupInfo groupInfo =
                doPostWithResponse("/api/entityGroup", entityGroup, EntityGroupInfo.class);
        var groupId = groupInfo.getId();

        shareGroup(groupId, ownerId);

        doPost("/api/entityGroup/" + groupId + "/makePublic")
                .andExpect(status().isOk());
        return doGet("/api/entityGroup/" + groupId, EntityGroupInfo.class);
    }

    protected void shareGroup(EntityGroupId entityGroupId, EntityId ownerId) throws Exception {
        ShareGroupRequest groupRequest = new ShareGroupRequest(
                ownerId,
                true,
                null,
                true,
                null
        );

        doPost("/api/entityGroup/" + entityGroupId + "/share", groupRequest)
                .andExpect(status().isOk());
    }

    protected void createPublicCustomerOnTenantLevel() throws Exception {
        createPublicGroupAndDeleteAfter(tenantId);
    }

    protected void createPublicCustomerOnCustomerLevel(Customer customer) throws Exception {
        User tmpUser = new User();
        tmpUser.setAuthority(Authority.CUSTOMER_USER);
        tmpUser.setTenantId(tenantId);
        tmpUser.setCustomerId(customer.getId());
        tmpUser.setEmail("tmpCustomerUser@thingsboard.org");
        User savedUser = createUser(tmpUser, "customer", findCustomerAdminsGroup(customer.getId()).getId());
        login("tmpCustomerUser@thingsboard.org", "customer");

        createPublicGroupAndDeleteAfter(customer.getId());

        loginTenantAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());
    }

    private void createPublicGroupAndDeleteAfter(EntityId ownerId) throws Exception {
        EntityGroup tmpEntityGroup = new EntityGroup();
        tmpEntityGroup.setType(EntityType.DEVICE);
        tmpEntityGroup.setName(ownerId.getId().toString());
        tmpEntityGroup.setOwnerId(ownerId);
        tmpEntityGroup = doPost("/api/entityGroup", tmpEntityGroup, EntityGroup.class);

        doPost("/api/entityGroup/" + tmpEntityGroup.getUuidId() + "/makePublic").andExpect(status().isOk());

        doDelete("/api/entityGroup/" + tmpEntityGroup.getId().getId().toString()).andExpect(status().isOk());
    }
}
