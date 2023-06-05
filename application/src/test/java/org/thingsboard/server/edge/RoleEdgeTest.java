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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RoleEdgeTest extends AbstractEdgeTest {

    @Test
    public void testTenantRole() throws Exception {
        // create role
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName("Generic Edge Role");
        edgeImitator.expectMessageAmount(1);
        Role savedRole = doPost("/api/role", role, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        RoleProto roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(savedRole.getUuidId().getMostSignificantBits(), roleProto.getIdMSB());
        Assert.assertEquals(savedRole.getUuidId().getLeastSignificantBits(), roleProto.getIdLSB());
        Assert.assertEquals(RoleType.GENERIC.name(), roleProto.getType());
        Assert.assertEquals("{\"ALL\":[\"ALL\"]}", roleProto.getPermissions());

        // update role
        edgeImitator.expectMessageAmount(1);
        savedRole.setName("Generic Edge Role Updated");
        savedRole = doPost("/api/role", savedRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals("Generic Edge Role Updated", roleProto.getName());

        // delete role
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/role/" + savedRole.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(savedRole.getUuidId().getMostSignificantBits(), roleProto.getIdMSB());
        Assert.assertEquals(savedRole.getUuidId().getLeastSignificantBits(), roleProto.getIdLSB());
    }

    @Test
    public void testCustomerRole() throws Exception {
        // create customer
        edgeImitator.expectMessageAmount(1);
        Customer savedCustomer = saveCustomer("Edge Customer", null);
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));
        role.setName("Customer Generic Edge Role");
        role.setOwnerId(savedCustomer.getId());
        role.setCustomerId(savedCustomer.getId());
        Role savedRole = doPost("/api/role", role, Role.class);
        // validate that no messages were sent to the edge
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // change edge owner from tenant to customer
        edgeImitator.expectMessageAmount(5);
        doPost("/api/owner/CUSTOMER/" + savedCustomer.getId().getId() + "/EDGE/" + edge.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<CustomerUpdateMsg> customerUpdateMsgs = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateMsgs.isPresent());
        CustomerUpdateMsg customerAUpdateMsg = customerUpdateMsgs.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerAUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), customerAUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), customerAUpdateMsg.getIdLSB());
        Assert.assertEquals(savedCustomer.getTitle(), customerAUpdateMsg.getTitle());

        Optional<RoleProto> roleProtoOpt = edgeImitator.findMessageByType(RoleProto.class);
        Assert.assertTrue(roleProtoOpt.isPresent());
        RoleProto roleProto = roleProtoOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(savedRole.getUuidId().getMostSignificantBits(), roleProto.getIdMSB());
        Assert.assertEquals(savedRole.getUuidId().getLeastSignificantBits(), roleProto.getIdLSB());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), roleProto.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), roleProto.getCustomerIdLSB());
        Assert.assertEquals(RoleType.GENERIC.name(), roleProto.getType());
        Assert.assertEquals("{\"ALL\":[\"ALL\"]}", roleProto.getPermissions());

        List<EntityGroupUpdateMsg> entityGroupUpdateMsgs = edgeImitator.findAllMessagesByType(EntityGroupUpdateMsg.class);
        Assert.assertEquals(2, entityGroupUpdateMsgs.size());

        Optional<EdgeConfiguration> edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());

        // update role
        edgeImitator.expectMessageAmount(1);
        savedRole.setName("Customer Generic Edge Role Updated");
        savedRole = doPost("/api/role", savedRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals("Customer Generic Edge Role Updated", roleProto.getName());

        // delete role
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/role/" + savedRole.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RoleProto);
        roleProto = (RoleProto) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, roleProto.getMsgType());
        Assert.assertEquals(savedRole.getUuidId().getMostSignificantBits(), roleProto.getIdMSB());
        Assert.assertEquals(savedRole.getUuidId().getLeastSignificantBits(), roleProto.getIdLSB());

        // change owner to tenant
        changeEdgeOwnerFromCustomerToTenant(savedCustomer);

        // delete customers
        doDelete("/api/customer/" + savedCustomer.getUuidId())
                .andExpect(status().isOk());
    }

}
