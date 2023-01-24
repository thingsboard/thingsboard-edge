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
package org.thingsboard.server.msa.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DashboardClientTest extends AbstractContainerTest {

    @Test
    public void testDashboards() throws Exception {
        // create dashboard #1, add to group #1 and assign group #1 to edge
        EntityGroup savedDashboardEntityGroup1 = createEntityGroup(EntityType.DASHBOARD);
        Dashboard savedDashboard1 = saveDashboardOnCloud("Edge Dashboard 1", savedDashboardEntityGroup1.getId());

        assignEntityGroupToEdge(savedDashboardEntityGroup1);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard1.getId()).isPresent());

        // update dashboard #1
        String updatedDashboardTitle = savedDashboard1.getTitle() + "Updated";
        savedDashboard1.setTitle(updatedDashboardTitle);
        cloudRestClient.saveDashboard(savedDashboard1);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> updatedDashboardTitle.equals(edgeRestClient.getDashboardById(savedDashboard1.getId()).get().getTitle()));

        // save dashboard #1 attribute
        JsonNode dashboardAttributes = JacksonUtil.OBJECT_MAPPER.readTree("{\"dashboardKey\":\"dashboardValue\"}");
        cloudRestClient.saveEntityAttributesV1(savedDashboard1.getId(), DataConstants.SERVER_SCOPE, dashboardAttributes);

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> verifyAttributeOnEdge(savedDashboard1.getId(),
                        DataConstants.SERVER_SCOPE, "dashboardKey", "dashboardValue"));

        // create dashboard #2 inside group #1
        Dashboard savedDashboard2 = saveDashboardOnCloud(StringUtils.randomAlphanumeric(15), savedDashboardEntityGroup1.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard2.getId()).isPresent());

        // add group #2 and assign to edge
        EntityGroup savedDashboardEntityGroup2 = createEntityGroup(EntityType.DASHBOARD);
        assignEntityGroupToEdge(savedDashboardEntityGroup2);

        // add dashboard #2 to group #2
        cloudRestClient.addEntitiesToEntityGroup(savedDashboardEntityGroup2.getId(), Collections.singletonList(savedDashboard2.getId()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> dashboard2Groups = edgeRestClient.getEntityGroupsForEntity(savedDashboard2.getId());
                    return dashboard2Groups.contains(savedDashboardEntityGroup2.getId());
                });

        // remove dashboard #2 from group #2
        cloudRestClient.removeEntitiesFromEntityGroup(savedDashboardEntityGroup2.getId(), Collections.singletonList(savedDashboard2.getId()));
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    List<EntityGroupId> dashboard2Groups = edgeRestClient.getEntityGroupsForEntity(savedDashboard2.getId());
                    return !dashboard2Groups.contains(savedDashboardEntityGroup2.getId());
                });

        // delete dashboard #2
        cloudRestClient.deleteDashboard(savedDashboard2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard2.getId()).isEmpty());

        // unassign group #1 from edge
        cloudRestClient.unassignEntityGroupFromEdge(edge.getId(), savedDashboardEntityGroup1.getId(), EntityType.DASHBOARD);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedDashboardEntityGroup1.getId()).isEmpty());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getDashboardById(savedDashboard1.getId()).isEmpty());

        // clean up
        cloudRestClient.deleteDashboard(savedDashboard1.getId());
        cloudRestClient.deleteEntityGroup(savedDashboardEntityGroup1.getId());
        cloudRestClient.deleteEntityGroup(savedDashboardEntityGroup2.getId());
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getEntityGroupById(savedDashboardEntityGroup2.getId()).isEmpty());
    }

}

