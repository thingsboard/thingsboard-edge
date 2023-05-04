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
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityGroupRequestMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.Collections;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class BaseDashboardEdgeTest extends AbstractEdgeTest {

    @Test
    public void testDashboards() throws Exception {
        // create dashboard entity group and assign to edge
        EntityGroup dashboardEntityGroup1 = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "DashboardGroup1", tenantId);

        // create dashboard and add to entity group 1
        edgeImitator.expectMessageAmount(1);
        Dashboard savedDashboard = saveDashboard("Edge Dashboard 1", dashboardEntityGroup1.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(dashboardEntityGroup1.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getEntityGroupIdMSB());
        Assert.assertEquals(dashboardEntityGroup1.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getEntityGroupIdLSB());

        // request dashboards by entity group id
        testDashboardEntityGroupRequestMsg(dashboardEntityGroup1.getUuidId().getMostSignificantBits(),
                dashboardEntityGroup1.getUuidId().getLeastSignificantBits(),
                savedDashboard.getId());

        // add dashboard to entity group 2
        EntityGroup dashboardEntityGroup2 = createEntityGroupAndAssignToEdge(EntityType.DASHBOARD, "DashboardGroup2", tenantId);
        edgeImitator.expectMessageAmount(1);
        addEntitiesToEntityGroup(Collections.singletonList(savedDashboard.getId()), dashboardEntityGroup2.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getEntityGroupIdMSB());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getEntityGroupIdLSB());

        // update dashboard
        edgeImitator.expectMessageAmount(1);
        savedDashboard.setTitle("Edge Dashboard 1 Updated");
        savedDashboard = doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals("Edge Dashboard 1 Updated", dashboardUpdateMsg.getTitle());

        // remove dashboard from entity group 2
        edgeImitator.expectMessageAmount(1);
        deleteEntitiesFromEntityGroup(Collections.singletonList(savedDashboard.getId()), dashboardEntityGroup2.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getEntityGroupIdMSB());
        Assert.assertEquals(dashboardEntityGroup2.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getEntityGroupIdLSB());

        unAssignEntityGroupFromEdge(dashboardEntityGroup2);

        // delete dashboard
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
    }

    private void testDashboardEntityGroupRequestMsg(long msbId, long lsbId, DashboardId expectedDashboardId) throws Exception {
        EntityGroupRequestMsg.Builder entitiesGroupRequestMsgBuilder = EntityGroupRequestMsg.newBuilder()
                .setEntityGroupIdMSB(msbId)
                .setEntityGroupIdLSB(lsbId)
                .setType(EntityType.DASHBOARD.name());
        testAutoGeneratedCodeByProtobuf(entitiesGroupRequestMsgBuilder);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addEntityGroupEntitiesRequestMsg(entitiesGroupRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        DashboardId receivedDashboardId =
                new DashboardId(new UUID(dashboardUpdateMsg.getIdMSB(), dashboardUpdateMsg.getIdLSB()));
        Assert.assertEquals(expectedDashboardId, receivedDashboardId);

        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsg);
    }
}
