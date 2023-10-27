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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeEntityType;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@DaoSqlTest
public class EntityGroupEdgeTest extends AbstractEdgeTest {

    @Test
    public void testAllEdgeEntityGroup_autoAssignedToEdgeAfterUnAssign() throws Exception {
        sendAssetCreateMsgToCloud("Asset From Edge");

        EntityGroup edgeAllAssetGroup = findEdgeAllGroup(EntityType.ASSET);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/entityGroup/" + edgeAllAssetGroup.getUuidId().toString() + "/" + edgeAllAssetGroup.getType().name(), EntityGroup.class);

        validateThatEntityGroupNotAssignedToEdge(edgeAllAssetGroup.getId(), EntityType.ASSET);

        sendAssetCreateMsgToCloud("Asset From Edge 2");

        validateThatEntityGroupAssignedToEdge(edgeAllAssetGroup.getId(), EntityType.ASSET);
    }

    @Test
    public void testEdgeRename_edgeAllGroupsRenamed() throws Exception {
        UUID deviceUuid = Uuids.timeBased();
        sendDeviceCreateMsgToCloud("Device From Edge", deviceUuid);
        sendAssetCreateMsgToCloud("Asset From Edge");
        sendEntityViewCreateMsgToCloud("Entity View From Edge", deviceUuid);
        sendDashboardCreateMsgToCloud("Dashboard From Edge");

        String currentEdgeName = edge.getName();

        verifyEdgeAllGroupNamingConvention(EntityType.DEVICE, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ASSET, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ENTITY_VIEW, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.DASHBOARD, currentEdgeName);

        String newEdgeName = "New Edge Name for rename test";
        edge.setName(newEdgeName);
        edge = doPost("/api/edge", edge, Edge.class);

        verifyEdgeAllGroupNamingConvention(EntityType.DEVICE, newEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ASSET, newEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ENTITY_VIEW, newEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.DASHBOARD, newEdgeName);

        // rollback
        edge.setName(currentEdgeName);
        edge = doPost("/api/edge", edge, Edge.class);

        verifyEdgeAllGroupNamingConvention(EntityType.DEVICE, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ASSET, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.ENTITY_VIEW, currentEdgeName);
        verifyEdgeAllGroupNamingConvention(EntityType.DASHBOARD, currentEdgeName);
    }

    private void verifyEdgeAllGroupNamingConvention(EntityType entityType, String edgeName) {
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    EntityGroup edgeAllGroup = findEdgeAllGroup(entityType);
                    return edgeAllGroup.getName().contains(edgeName);
                });
    }

    private void sendDeviceCreateMsgToCloud(String deviceName, UUID uuid) throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceUpdateMsg.Builder deviceUpdateMsgBuilder = DeviceUpdateMsg.newBuilder();
        deviceUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        deviceUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        deviceUpdateMsgBuilder.setName(deviceName);
        deviceUpdateMsgBuilder.setType("default");
        deviceUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceUpdateMsg(deviceUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendAssetCreateMsgToCloud(String assetName) throws Exception {
        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AssetUpdateMsg.Builder assetUpdateMsgBuilder = AssetUpdateMsg.newBuilder();
        assetUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        assetUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        assetUpdateMsgBuilder.setName(assetName);
        assetUpdateMsgBuilder.setType("default");
        assetUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(assetUpdateMsgBuilder);
        uplinkMsgBuilder.addAssetUpdateMsg(assetUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendEntityViewCreateMsgToCloud(String entityViewName, UUID entityUuid) throws Exception {
        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        EntityViewUpdateMsg.Builder entityViewUpdateMsgBuilder = EntityViewUpdateMsg.newBuilder();
        entityViewUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        entityViewUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        entityViewUpdateMsgBuilder.setName(entityViewName);
        entityViewUpdateMsgBuilder.setType("default");
        entityViewUpdateMsgBuilder.setEntityType(EdgeEntityType.DEVICE);
        entityViewUpdateMsgBuilder.setEntityIdMSB(entityUuid.getMostSignificantBits());
        entityViewUpdateMsgBuilder.setEntityIdLSB(entityUuid.getLeastSignificantBits());
        entityViewUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addEntityViewUpdateMsg(entityViewUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private void sendDashboardCreateMsgToCloud(String dashboardName) throws Exception {
        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardUpdateMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        dashboardUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        dashboardUpdateMsgBuilder.setTitle(dashboardName);
        dashboardUpdateMsgBuilder.setConfiguration("");
        dashboardUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDashboardUpdateMsg(dashboardUpdateMsgBuilder.build());

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

    private EntityGroup findEdgeAllGroup(EntityType groupType) throws Exception {
        List<EntityGroupInfo> groupsList = getEntityGroupsByOwnerAndType(tenantId, groupType);
        EntityGroup edgeAllAssetGroup = null;
        for (EntityGroupInfo tmp : groupsList) {
            if (tmp.isEdgeGroupAll()) {
                edgeAllAssetGroup = tmp;
                break;
            }
        }
        Assert.assertNotNull(edgeAllAssetGroup);
        return edgeAllAssetGroup;
    }
}
